package com.smu.daiary.data.source

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
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

    /**
     * 사진을 시계 방향 90° 돌려 캐시 폴더에 JPEG로 저장하고 그 file:// URI를 돌려준다.
     * 이미 업로드된 사진(https)은 받아서 돌리고, 기기 사진은 [compressToJpeg]로 방향·크기를 먼저 맞춘다.
     * 돌린 파일은 로컬 URI라 저장할 때 [uploadDiaryPhoto]가 새로 올린다. 옛 Storage 파일은 그대로 남는다.
     */
    suspend fun rotateToCacheFile(source: String): String = withContext(Dispatchers.IO) {
        val bytes = if (source.startsWith("http")) java.net.URL(source).openStream().use { it.readBytes() }
        else compressToJpeg(Uri.parse(source))
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalStateException("이미지를 읽을 수 없습니다: $source")
        val rotated = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(90f) }, true
        )
        bitmap.recycle()
        val dir = java.io.File(context.cacheDir, "rotated").apply { mkdirs() }
        val file = java.io.File(dir, "${UUID.randomUUID()}.jpg")
        file.outputStream().use { rotated.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        rotated.recycle()
        Uri.fromFile(file).toString()
    }

    /**
     * URI의 이미지를 maxDimension 이내로 축소하고 JPEG 바이트로 압축한다.
     *
     * 다시 인코딩하므로 EXIF(GPS 좌표·촬영 기기·시각)가 전부 빠진다. 업로드뿐 아니라
     * AI 분석으로 보낼 때도 이걸 거친다 — 원본을 그대로 보내면 집 위치가 외부로 나간다.
     * EXIF가 빠지면 회전 정보도 사라지므로, 회전은 픽셀에 먼저 적용한다.
     */
    fun compressToJpeg(uri: Uri): ByteArray {
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
        val upright = rotateUpright(uri, bitmap)

        return ByteArrayOutputStream().use { out ->
            upright.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
            upright.recycle()
            out.toByteArray()
        }
    }

    /**
     * EXIF 방향값(회전 + 좌우 반전)을 픽셀에 적용한다. 세로 사진이 눕거나 전면 카메라 사진이 뒤집히지 않게.
     * 반전이 낀 방향(2·4·5·7)은 회전한 뒤 좌우로 뒤집으면 된다 — Glide TransformationUtils와 같은 순서.
     */
    private fun rotateUpright(uri: Uri, bitmap: Bitmap): Bitmap {
        val (degrees, flipped) = context.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).let { exif -> exif.rotationDegrees to exif.isFlipped }
        } ?: (0 to false)
        if (degrees == 0 && !flipped) return bitmap
        val matrix = Matrix().apply {
            postRotate(degrees.toFloat())
            if (flipped) postScale(-1f, 1f)
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        bitmap.recycle()
        return rotated
    }
}
