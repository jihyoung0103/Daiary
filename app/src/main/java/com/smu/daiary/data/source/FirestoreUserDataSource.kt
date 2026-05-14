package com.smu.daiary.data.source.firebase

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreUserDataSource {

    private val db by lazy { FirebaseFirestore.getInstance() }

    /**
     * 로그인/회원가입 시 users/{uid} 문서를 upsert.
     * Firestore 실패는 non-fatal — 인증 성공을 막지 않음.
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
        }
    }
}
