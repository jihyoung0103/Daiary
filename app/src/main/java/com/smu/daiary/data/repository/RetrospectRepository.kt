package com.smu.daiary.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.smu.daiary.data.model.RetrospectReport
import kotlinx.coroutines.tasks.await

/**
 * Firestore CRUD를 담당하는 Repository.
 *
 * 컬렉션 구조:
 *   users/{userId}/retrospects/{id}   id = "yyyy-'W'ww"(주간) | "yyyy-MM"(월간)
 *
 * 문서 ID로 존재 여부를 바로 확인할 수 있습니다.
 */
class RetrospectRepository {

    private val db = FirebaseFirestore.getInstance()

    private fun retrospectsRef(userId: String) =
        db.collection("users").document(userId).collection("retrospects")

    /** 저장된 회고를 가져옵니다. 없으면 null. */
    suspend fun getRetrospect(userId: String, id: String): Result<RetrospectReport?> = runCatching {
        retrospectsRef(userId).document(id).get().await().toObject<RetrospectReport>()
    }

    /** 회고를 Firestore에 저장(덮어쓰기)합니다. */
    suspend fun saveRetrospect(userId: String, report: RetrospectReport): Result<Unit> = runCatching {
        retrospectsRef(userId).document(report.id).set(report).await()
    }
}
