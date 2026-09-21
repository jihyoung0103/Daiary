package com.smu.daiary.data.source.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class FirebaseAuthDataSource {

    private val auth by lazy { FirebaseAuth.getInstance() }

    /** @throws FirebaseAuthException on auth failure */
    suspend fun signIn(email: String, password: String): FirebaseUser =
        auth.signInWithEmailAndPassword(email, password).await().user
            ?: throw IllegalStateException("Auth succeeded but user is null")

    /** @throws FirebaseAuthException on auth failure */
    suspend fun signUp(email: String, password: String): FirebaseUser =
        auth.createUserWithEmailAndPassword(email, password).await().user
            ?: throw IllegalStateException("Auth succeeded but user is null")

    fun signOut() = auth.signOut()

    fun currentUser(): FirebaseUser? = auth.currentUser
}
