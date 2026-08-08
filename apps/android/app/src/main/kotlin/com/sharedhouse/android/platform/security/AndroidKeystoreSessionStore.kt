package com.sharedhouse.android.platform.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.sharedhouse.network.SessionDto
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.time.DateTimeException
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private const val AndroidKeyStoreProvider = "AndroidKeyStore"
private const val KeyAlias = "sharedhouse.session.aes-gcm.v1"
private const val SessionFileName = "sharedhouse-session-v1.bin"
private const val CipherTransformation = "AES/GCM/NoPadding"
private const val GcmTagBits = 128
private const val MaximumEncryptedSessionBytes = 64 * 1024
private const val EnvelopeMagic = 0x53485331 // SHS1
private const val EnvelopeVersion: Byte = 1
private const val MaximumIvBytes = 32

private val SessionAssociatedData = "com.sharedhouse.android:session:v1".encodeToByteArray()

class AndroidKeystoreSessionStore private constructor(
    private val sessionFile: File,
    private val cipher: SessionCipher,
) : SessionStore {
    private val mutex = Mutex()

    constructor(context: Context) : this(
        sessionFile = File(context.applicationContext.noBackupFilesDir, SessionFileName),
        cipher = AndroidKeystoreSessionCipher(),
    )

    override suspend fun load(): SessionLoadResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!sessionFile.exists()) return@withLock SessionLoadResult.Missing
            val encryptedBytes = try {
                val size = sessionFile.length()
                if (size !in 1..MaximumEncryptedSessionBytes.toLong()) {
                    invalidateStoredSession()
                    return@withLock SessionLoadResult.Invalid
                }
                sessionFile.readBytes()
            } catch (_: Exception) {
                return@withLock SessionLoadResult.Unavailable
            }

            try {
                val envelope = SessionEnvelope.decode(encryptedBytes)
                val plaintext = cipher.decrypt(envelope.initializationVector, envelope.ciphertext)
                SessionLoadResult.Restored(SessionPayloadCodec.decode(plaintext))
            } catch (_: SessionFormatException) {
                invalidateStoredSession()
                SessionLoadResult.Invalid
            } catch (_: SerializationException) {
                invalidateStoredSession()
                SessionLoadResult.Invalid
            } catch (_: IllegalArgumentException) {
                invalidateStoredSession()
                SessionLoadResult.Invalid
            } catch (_: DateTimeException) {
                invalidateStoredSession()
                SessionLoadResult.Invalid
            } catch (_: GeneralSecurityException) {
                invalidateStoredSession()
                SessionLoadResult.Invalid
            } catch (_: Exception) {
                SessionLoadResult.Unavailable
            }
        }
    }

    override suspend fun save(session: SessionDto): SessionSaveResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val plaintext = SessionPayloadCodec.encode(session)
                val encrypted = cipher.encrypt(plaintext)
                val envelope = SessionEnvelope(
                    initializationVector = encrypted.initializationVector,
                    ciphertext = encrypted.ciphertext,
                ).encode()
                require(envelope.size <= MaximumEncryptedSessionBytes)
                writeAtomically(envelope)
                SessionSaveResult.SAVED
            } catch (_: Exception) {
                SessionSaveResult.UNAVAILABLE
            }
        }
    }

    override suspend fun clear(): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val fileRemoved = runCatching {
                !sessionFile.exists() || sessionFile.delete()
            }.getOrDefault(false)
            val keyRemoved = runCatching { cipher.destroyKey() }.getOrDefault(false)
            fileRemoved || keyRemoved
        }
    }

    private fun invalidateStoredSession() {
        runCatching { sessionFile.delete() }
        runCatching { cipher.destroyKey() }
    }

    private fun writeAtomically(bytes: ByteArray) {
        val parent = requireNotNull(sessionFile.parentFile)
        check(parent.exists() || parent.mkdirs())
        val temporary = File(parent, "${sessionFile.name}.tmp")
        try {
            FileOutputStream(temporary).use { output ->
                output.write(bytes)
                output.fd.sync()
            }
            try {
                Files.move(
                    temporary.toPath(),
                    sessionFile.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(
                    temporary.toPath(),
                    sessionFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }
        } finally {
            runCatching { temporary.delete() }
        }
    }

    internal companion object {
        fun forTest(sessionFile: File, cipher: SessionCipher): AndroidKeystoreSessionStore =
            AndroidKeystoreSessionStore(sessionFile, cipher)
    }
}

internal data class EncryptedSession(
    val initializationVector: ByteArray,
    val ciphertext: ByteArray,
)

internal interface SessionCipher {
    @Throws(GeneralSecurityException::class)
    fun encrypt(plaintext: ByteArray): EncryptedSession

    @Throws(GeneralSecurityException::class)
    fun decrypt(initializationVector: ByteArray, ciphertext: ByteArray): ByteArray

    fun destroyKey(): Boolean
}

private class AndroidKeystoreSessionCipher : SessionCipher {
    override fun encrypt(plaintext: ByteArray): EncryptedSession {
        val cipher = Cipher.getInstance(CipherTransformation)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        cipher.updateAAD(SessionAssociatedData)
        return EncryptedSession(
            initializationVector = cipher.iv.copyOf(),
            ciphertext = cipher.doFinal(plaintext),
        )
    }

    override fun decrypt(initializationVector: ByteArray, ciphertext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(CipherTransformation)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getExistingKey(),
            GCMParameterSpec(GcmTagBits, initializationVector),
        )
        cipher.updateAAD(SessionAssociatedData)
        return cipher.doFinal(ciphertext)
    }

    override fun destroyKey(): Boolean {
        val keyStore = keyStore()
        if (keyStore.containsAlias(KeyAlias)) keyStore.deleteEntry(KeyAlias)
        return !keyStore.containsAlias(KeyAlias)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = keyStore()
        (keyStore.getKey(KeyAlias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStoreProvider)
        generator.init(
            KeyGenParameterSpec.Builder(
                KeyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private fun getExistingKey(): SecretKey =
        (keyStore().getKey(KeyAlias, null) as? SecretKey)
            ?: throw GeneralSecurityException("Session key is unavailable.")

    private fun keyStore(): KeyStore = KeyStore.getInstance(AndroidKeyStoreProvider).apply {
        load(null)
    }
}

private data class SessionEnvelope(
    val initializationVector: ByteArray,
    val ciphertext: ByteArray,
) {
    fun encode(): ByteArray = ByteBuffer.allocate(
        Int.SIZE_BYTES + Byte.SIZE_BYTES + Byte.SIZE_BYTES + initializationVector.size +
            Int.SIZE_BYTES + ciphertext.size,
    )
        .putInt(EnvelopeMagic)
        .put(EnvelopeVersion)
        .put(initializationVector.size.toByte())
        .put(initializationVector)
        .putInt(ciphertext.size)
        .put(ciphertext)
        .array()

    companion object {
        fun decode(bytes: ByteArray): SessionEnvelope {
            try {
                val buffer = ByteBuffer.wrap(bytes)
                if (buffer.remaining() < Int.SIZE_BYTES + 2 * Byte.SIZE_BYTES + Int.SIZE_BYTES) {
                    throw SessionFormatException()
                }
                if (buffer.int != EnvelopeMagic || buffer.get() != EnvelopeVersion) {
                    throw SessionFormatException()
                }
                val ivSize = buffer.get().toInt() and 0xff
                if (ivSize !in 12..MaximumIvBytes || buffer.remaining() < ivSize + Int.SIZE_BYTES) {
                    throw SessionFormatException()
                }
                val iv = ByteArray(ivSize).also(buffer::get)
                val ciphertextSize = buffer.int
                if (ciphertextSize <= GcmTagBits / Byte.SIZE_BITS || ciphertextSize != buffer.remaining()) {
                    throw SessionFormatException()
                }
                val ciphertext = ByteArray(ciphertextSize).also(buffer::get)
                return SessionEnvelope(iv, ciphertext)
            } catch (error: SessionFormatException) {
                throw error
            } catch (_: Exception) {
                throw SessionFormatException()
            }
        }
    }
}

private object SessionPayloadCodec {
    private val json = Json {
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = false
    }

    fun encode(session: SessionDto): ByteArray {
        validate(session)
        return json.encodeToString(SessionDto.serializer(), session).encodeToByteArray()
    }

    fun decode(bytes: ByteArray): SessionDto {
        val session = json.decodeFromString(SessionDto.serializer(), bytes.decodeToString())
        validate(session)
        return session
    }

    private fun validate(session: SessionDto) {
        require(session.accessToken.isNotBlank() && session.accessToken.length <= 4096)
        require(session.refreshToken.isNotBlank() && session.refreshToken.length <= 4096)
        require(session.accessTokenExpiresAt.length in 10..64)
        require(session.refreshTokenExpiresAt.length in 10..64)
        Instant.parse(session.accessTokenExpiresAt)
        Instant.parse(session.refreshTokenExpiresAt)
        require(session.account.id.isNotBlank() && session.account.id.length <= 128)
        require(session.account.email.isNotBlank() && session.account.email.length <= 320)
        require(session.account.displayName.isNotBlank() && session.account.displayName.length <= 120)
        require(session.account.emailVerified)
        require(session.account.preferredLocale in setOf("en", "ro"))
    }
}

private class SessionFormatException : Exception()
