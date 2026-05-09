package com.smu.daiary.diary

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore CRUD를 담당하는 Repository.
 *
 * 컬렉션 구조:
 *   users/{userId}/diaries/{diaryId}
 */
class DiaryRepository {

    private val db = FirebaseFirestore.getInstance()

    private fun diariesRef(userId: String) =
        db.collection("users").document(userId).collection("diaries")

    /**
     * 특정 사용자의 일기 목록을 실시간으로 구독합니다.
     * 최신순(createdAt 내림차순)으로 반환합니다.
     */
    fun getDiaries(userId: String): Flow<List<DiaryEntry>> = callbackFlow {
        val listener = diariesRef(userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val diaries = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<DiaryEntry>()?.copy(id = doc.id)
                } ?: emptyList()
                trySend(diaries)
            }
        awaitClose { listener.remove() }
    }

    /** 일기를 Firestore에 추가합니다. */
    suspend fun addDiary(userId: String, entry: DiaryEntry): Result<Unit> = runCatching {
        diariesRef(userId).add(entry).await()
    }

    /** 일기를 수정합니다. */
    suspend fun updateDiary(userId: String, entry: DiaryEntry): Result<Unit> = runCatching {
        diariesRef(userId).document(entry.id).set(entry).await()
    }

    /** 일기를 삭제합니다. */
    suspend fun deleteDiary(userId: String, diaryId: String): Result<Unit> = runCatching {
        diariesRef(userId).document(diaryId).delete().await()
    }
}
