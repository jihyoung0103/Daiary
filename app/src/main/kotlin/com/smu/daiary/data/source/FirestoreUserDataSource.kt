package com.smu.daiary.data.source

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Data source wrapping Firebase Firestore SDK.
 * ONLY this class imports com.google.firebase.firestore.*.
 * Uses lazy initialization to avoid FirebaseApp pre-initialization crash.
 */
class FirestoreUserDataSource {

    private val db by lazy { FirebaseFirestore.getInstance() }

    /**
     * Upsert user document at users/{uid}.
     * Uses set() with SetOptions.merge() — idempotent, safe to call on every login.
     * Non-fatal: exceptions are caught and logged; callers receive success even if Firestore write fails.
     * This ensures auth success is not blocked by Firestore offline/permission errors.
     */
    suspend fun upsertUser(uid: String, email: String, displayName: String = "") {
        try {
            val data = buildMap<String, Any> {
                put("email", email)
                put("lastLoginAt", FieldValue.serverTimestamp())
                if (displayName.isNotBlank()) put("displayName", displayName)
            }
            db.collection("users").document(uid).set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w("FirestoreUserDataSource", "upsertUser failed (non-fatal): ${e.message}")
            // Do not rethrow — Firestore write failure must not block auth success
        }
    }
}
