package com.smu.daiary.data.source

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

/**
 * Data source wrapping Firebase Authentication SDK.
 * ONLY this class imports com.google.firebase.auth.*.
 * Uses lazy initialization to avoid FirebaseApp pre-initialization crash.
 */
class FirebaseAuthDataSource {

    private val auth by lazy { FirebaseAuth.getInstance() }

    /**
     * Sign in with email and password.
     * @throws FirebaseAuthException on auth failure (wrong password, user not found, etc.)
     * @throws IllegalStateException if Firebase returns a null user after success
     */
    suspend fun signIn(email: String, password: String): FirebaseUser {
        return auth.signInWithEmailAndPassword(email, password).await().user
            ?: throw IllegalStateException("Auth succeeded but user is null")
    }

    /**
     * Create account with email and password.
     * @throws FirebaseAuthException on auth failure (email in use, weak password, etc.)
     * @throws IllegalStateException if Firebase returns a null user after success
     */
    suspend fun signUp(email: String, password: String): FirebaseUser {
        return auth.createUserWithEmailAndPassword(email, password).await().user
            ?: throw IllegalStateException("Auth succeeded but user is null")
    }

    fun signOut() {
        auth.signOut()
    }

    fun currentUser(): FirebaseUser? = auth.currentUser
}
