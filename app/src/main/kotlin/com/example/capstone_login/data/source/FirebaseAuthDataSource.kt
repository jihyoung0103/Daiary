package com.example.capstone_login.data.source

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

/**
 * Data source wrapping Firebase Authentication SDK.
 * ONLY this class imports com.google.firebase.auth.*.
 * Uses lazy initialization to avoid FirebaseApp pre-initialization crash (Pitfall 2).
 */
class FirebaseAuthDataSource {

    private val auth by lazy { FirebaseAuth.getInstance() }

    /**
     * Stub: sign in with email and password.
     * Phase 2 implements the actual Firebase call.
     * @throws FirebaseAuthException on auth failure
     */
    suspend fun signIn(email: String, password: String): FirebaseUser {
        // TODO Phase 2: implement
        // return auth.signInWithEmailAndPassword(email, password).await().user
        //     ?: throw IllegalStateException("Auth succeeded but user is null")
        throw NotImplementedError("Phase 2: implement signIn")
    }

    /**
     * Stub: create account with email and password.
     * Phase 2 implements the actual Firebase call.
     */
    suspend fun signUp(email: String, password: String): FirebaseUser {
        // TODO Phase 2: implement
        // return auth.createUserWithEmailAndPassword(email, password).await().user
        //     ?: throw IllegalStateException("Auth succeeded but user is null")
        throw NotImplementedError("Phase 2: implement signUp")
    }

    fun signOut() {
        auth.signOut()
    }

    fun currentUser(): FirebaseUser? = auth.currentUser
}
