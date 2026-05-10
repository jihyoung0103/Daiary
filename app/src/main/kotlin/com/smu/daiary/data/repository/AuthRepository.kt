package com.smu.daiary.data.repository

import com.smu.daiary.data.source.FirebaseAuthDataSource
import com.smu.daiary.data.source.FirestoreUserDataSource
import com.smu.daiary.model.User
import com.smu.daiary.util.Result
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser

/**
 * Repository combining Firebase Auth and Firestore data sources.
 * ViewModel talks ONLY to AuthRepository — not to DataSource classes directly.
 * Wraps all Firebase exceptions as Result.Error so ViewModel never sees raw Firebase types
 * (exception to the rule: FirebaseUser is returned in Result.Success for Phase 2 navigation).
 */
class AuthRepository(
    private val authSource: FirebaseAuthDataSource = FirebaseAuthDataSource(),
    private val userSource: FirestoreUserDataSource = FirestoreUserDataSource()
) {

    /**
     * Stub: Sign in flow — Auth then Firestore upsert.
     * Phase 2 implements actual calls.
     */
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val user = authSource.signIn(email, password)
            // Non-fatal: proceed to calendar even if upsert fails (Firestore offline resilience)
            runCatching { userSource.upsertUser(user.uid, user.email ?: "") }
            Result.Success(user)
        } catch (e: FirebaseAuthException) {
            Result.Error(e)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Stub: Sign up flow — creates account then upserts user document.
     * Phase 2 implements actual calls.
     */
    suspend fun signUp(email: String, password: String): Result<FirebaseUser> {
        return try {
            val user = authSource.signUp(email, password)
            val displayName = user.email?.substringBefore("@").orEmpty()
            runCatching { userSource.upsertUser(user.uid, user.email ?: "", displayName) }
            Result.Success(user)
        } catch (e: FirebaseAuthException) {
            Result.Error(e)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun signOut() = authSource.signOut()

    fun currentUser(): FirebaseUser? = authSource.currentUser()
}
