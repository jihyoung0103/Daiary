package com.smu.daiary.data.repository.firebase

import com.google.firebase.auth.FirebaseUser
import com.smu.daiary.data.source.firebase.FirebaseAuthDataSource
import com.smu.daiary.data.source.firebase.FirestoreUserDataSource

sealed class AuthResult<out T> {
    data class Success<out T>(val data: T) : AuthResult<T>()
    data class Error(val exception: Exception) : AuthResult<Nothing>()
}

class AuthRepository(
    private val authSource: FirebaseAuthDataSource = FirebaseAuthDataSource(),
    private val userSource: FirestoreUserDataSource = FirestoreUserDataSource()
) {

    suspend fun signIn(email: String, password: String): AuthResult<FirebaseUser> {
        return try {
            val user = authSource.signIn(email, password)
            runCatching { userSource.upsertUser(user.uid, user.email ?: "") }
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e)
        }
    }

    suspend fun signUp(email: String, password: String, displayName: String = ""): AuthResult<FirebaseUser> {
        return try {
            val user = authSource.signUp(email, password)
            val nameToSave = displayName.trim().ifBlank { user.email?.substringBefore("@").orEmpty() }
            runCatching { userSource.upsertUser(user.uid, user.email ?: "", nameToSave) }
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e)
        }
    }

    suspend fun upsertUser(uid: String, email: String, displayName: String = "") {
        runCatching { userSource.upsertUser(uid, email, displayName) }
    }

    fun signOut() = authSource.signOut()

    fun currentUser(): FirebaseUser? = authSource.currentUser()
}
