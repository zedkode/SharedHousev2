package com.sharedhouse.android.platform.security

import com.sharedhouse.network.AccountDto
import com.sharedhouse.network.SessionDto
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class AndroidKeystoreSessionStoreTest {
    @Test
    fun `encrypted session round trips without exposing either token`() = runTest {
        val directory = createTempDirectory("sharedhouse-session-test").toFile()
        try {
            val file = directory.resolve("session.bin")
            val store = AndroidKeystoreSessionStore.forTest(file, JvmAesSessionCipher())
            val session = testSession()

            assertEquals(SessionSaveResult.SAVED, store.save(session))

            val rawStorage = file.readBytes().decodeToString()
            assertFalse(rawStorage.contains(session.accessToken))
            assertFalse(rawStorage.contains(session.refreshToken))
            assertEquals(session, assertIs<SessionLoadResult.Restored>(store.load()).session)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `tampered ciphertext is rejected and made unrecoverable`() = runTest {
        val directory = createTempDirectory("sharedhouse-session-test").toFile()
        try {
            val file = directory.resolve("session.bin")
            val cipher = JvmAesSessionCipher()
            val store = AndroidKeystoreSessionStore.forTest(file, cipher)
            assertEquals(SessionSaveResult.SAVED, store.save(testSession()))
            val tampered = file.readBytes().also { bytes ->
                bytes[bytes.lastIndex] = (bytes.last().toInt() xor 0x01).toByte()
            }
            file.writeBytes(tampered)

            assertEquals(SessionLoadResult.Invalid, store.load())
            assertFalse(file.exists())
            assertTrue(cipher.destroyed)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `failed rotation leaves the previous atomic session intact`() = runTest {
        val directory = createTempDirectory("sharedhouse-session-test").toFile()
        try {
            val file = directory.resolve("session.bin")
            val cipher = JvmAesSessionCipher()
            val store = AndroidKeystoreSessionStore.forTest(file, cipher)
            val original = testSession()
            assertEquals(SessionSaveResult.SAVED, store.save(original))

            cipher.failEncryption = true
            assertEquals(
                SessionSaveResult.UNAVAILABLE,
                store.save(original.copy(accessToken = "replacement-access")),
            )
            cipher.failEncryption = false

            assertEquals(original, assertIs<SessionLoadResult.Restored>(store.load()).session)
        } finally {
            directory.deleteRecursively()
        }
    }
}

private class JvmAesSessionCipher : SessionCipher {
    private var key: SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    var failEncryption = false
    var destroyed = false

    override fun encrypt(plaintext: ByteArray): EncryptedSession {
        if (failEncryption) throw GeneralSecurityException("Synthetic encryption failure")
        val iv = ByteArray(12).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        cipher.updateAAD(TEST_ASSOCIATED_DATA)
        return EncryptedSession(iv, cipher.doFinal(plaintext))
    }

    override fun decrypt(initializationVector: ByteArray, ciphertext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, initializationVector))
        cipher.updateAAD(TEST_ASSOCIATED_DATA)
        return cipher.doFinal(ciphertext)
    }

    override fun destroyKey(): Boolean {
        destroyed = true
        return true
    }
}

private fun testSession() = SessionDto(
    accessToken = "test-access-token-sensitive",
    refreshToken = "test-refresh-token-sensitive",
    accessTokenExpiresAt = "2026-08-08T12:15:00Z",
    refreshTokenExpiresAt = "2026-09-08T12:00:00Z",
    account = AccountDto(
        id = "018f0000-0000-7000-8000-000000000001",
        email = "alex@example.test",
        emailVerified = true,
        displayName = "Alex",
        preferredLocale = "en",
    ),
)

private val TEST_ASSOCIATED_DATA = "com.sharedhouse.android:session:v1".encodeToByteArray()
