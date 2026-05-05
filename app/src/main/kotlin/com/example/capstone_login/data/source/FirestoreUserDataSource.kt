package com.example.capstone_login.data.source

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Data source wrapping Firebase Firestore SDK.
 * ONLY this class imports com.google.firebase.firestore.*.
 * Uses lazy initialization to avoid FirebaseApp pre-initialization crash (Pitfall 2).
 */
class FirestoreUserDataSource {

    private val db by lazy { FirebaseFirestore.getInstance() }

    /**
     * Stub: upsert user document at users/{uid}.
     * Phase 2 implements the actual Firestore write using set(merge=true).
     * Using set() with SetOptions.merge() is idempotent — safe to call on every login.
     */
    suspend fun upsertUser(uid: String, email: String) {
        // TODO Phase 2: implement
        // val data = mapOf(
        //     "email" to email,
        //     "lastLoginAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        // )
        // db.collection("users").document(uid).set(data, SetOptions.merge()).await()
        throw NotImplementedError("Phase 2: implement upsertUser")
    }
}
