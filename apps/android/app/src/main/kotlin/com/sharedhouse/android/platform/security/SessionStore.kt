package com.sharedhouse.android.platform.security

import com.sharedhouse.network.SessionDto

sealed interface SessionLoadResult {
    data object Missing : SessionLoadResult

    data class Restored(val session: SessionDto) : SessionLoadResult

    data object Invalid : SessionLoadResult

    data object Unavailable : SessionLoadResult
}

enum class SessionSaveResult {
    SAVED,
    UNAVAILABLE,
}

interface SessionStore {
    suspend fun load(): SessionLoadResult

    suspend fun save(session: SessionDto): SessionSaveResult

    /** Returns true when no recoverable local session remains. */
    suspend fun clear(): Boolean
}
