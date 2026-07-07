package com.smu.daiary.data.source

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.math.max

/**
 * 일기 사진을 Firebase Storage에 업로드하고 다운로드 URL을 돌려주는 데이터소스.
 *
 * 로컬 content:// URI를 리사이즈 + JPEG 압축해서 올려 용량을 줄인다.
 * 저장 경로: users/{userId}/diaries/{date}/{uuid}.jpg
 */
class PhotoStorageDataSource(private val context: Context) {

    private val storage = FirebaseStorage.getInstance()

    /** 최대 변 길이(px). 이보다 크면 비율을 유지하며 축소한다. */
    private val maxDimension = 1600

    /** JPEG 압축 품질(0~100). */
    private val jpegQuality = 80

    /**
     * localUri가 이미 http(s) URL이면 재업로드 없이 그대로 반환(재저장 시 중복 업로드 방지).
     * 아니면 압축 후 업로드하여 다운로드 URL 문자열을 반환한다. 실패 시 예외를 던진다.
     */
    suspend fun uploadDiaryPhoto(userId: String, date: String, localUri: String): String =
        withContext(Dispatchers.IO) {
            if (localUri.startsWith("http")) return@withContext localUri

            val bytes = compressToJpeg(Uri.parse(localUri))
            val ref = storage.reference
                .child("users/$userId/diaries/$date/${UUID.randomUUID()}.jpg")
            ref.putBytes(bytes).await()
            ref.downloadUrl.await().toString()
        }

    /** URI의 이미지를 maxDimension 이내로 축소하고 JPEG 바이트로 압축한다. */
    private fun compressToJpeg(uri: Uri): ByteArray {
        val resolver = context.contentResolver

        // 1) 원본 크기만 먼저 읽어 축소 배율(inSampleSize) 계산 — 큰 사진을 통째로 디코드하지 않는다.
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOptions) }
        val longestEdge = max(boundsOptions.outWidth, boundsOptions.outHeight).coerceAtLeast(1)

        var sample = 1
        while (longestEdge / sample > maxDimension) sample *= 2

        // 2) 실제 디코드
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: throw IllegalStateException("이미지를 읽을 수 없습니다: $uri")

        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }
}
