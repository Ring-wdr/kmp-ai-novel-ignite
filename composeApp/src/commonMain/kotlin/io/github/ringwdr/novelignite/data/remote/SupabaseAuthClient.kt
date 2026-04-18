package io.github.ringwdr.novelignite.data.remote

interface SupabaseAuthClient {
    suspend fun signInWithPassword(email: String, password: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
}
