package com.smu.daiary.data.source

import android.content.Context
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.smu.daiary.data.model.PhotoMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import android.content.ContentUris
import androidx.core.net.toUri
import com.smu.daiary.util.DiaryDateUtil

class PhotoDataSource(private val context: Context) {

    // 일기 기준 날짜의 사진만 조회. 오전 4시 이전이면 전날 기준으로 조회한다.
    private fun todayRange(): Pair<Long, Long> {
        val today = DiaryDateUtil.diaryDate()
        val zone = ZoneId.systemDefault()
        val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }

    suspend fun fetchTodayPhotos(): List<PhotoMeta> = withContext(Dispatchers.IO) {
        val (start, end) = todayRange()
        val photos = mutableListOf<PhotoMeta>()

        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN  // 촬영 시각 (epoch millis)
        )
        val selection = "${MediaStore.Images.Media.DATE_TAKEN} BETWEEN ? AND ?"
        val selectionArgs = arrayOf(start.toString(), end.toString())

        val cursor = context.contentResolver.query(
            uri, projection, selection, selectionArgs,
            "${MediaStore.Images.Media.DATE_TAKEN} ASC"
        )

        cursor?.use {
            val idIdx = it.getColumnIndex(MediaStore.Images.Media._ID)
            val takenIdx = it.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)

            while (it.moveToNext()) {
                val imageId = it.getLong(idIdx)
                val mediaStoreTakenAt = it.getLong(takenIdx)

                val contentUri =
                    ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        imageId
                    )

                val exif = readExif(contentUri.toString())

                // 오늘 여부는 위 MediaStore 쿼리로 이미 필터링됨.
                // 시간 데이터 활용은 EXIF 촬영 시각을 우선하고, 없으면 MediaStore DATE_TAKEN으로 폴백.
                val takenAt = exif.takenAt ?: mediaStoreTakenAt

                photos.add(
                    PhotoMeta(
                        uri = contentUri.toString(),
                        takenAt = takenAt,
                        latitude = exif.latitude,
                        longitude = exif.longitude,
                        isCameraPhoto = exif.isCameraPhoto
                    )
                )
            }
        }

        photos
    }

    /**
     * 갤러리에서 수동 선택하는 등 MediaStore 조회를 거치지 않은 단일 URI의 메타데이터를 읽는다.
     * takenAt은 EXIF 촬영 시각(없으면 0), 위도/경도는 EXIF GPS(없으면 0.0).
     */
    suspend fun readPhotoMeta(uri: String): PhotoMeta = withContext(Dispatchers.IO) {
        val exif = readExif(uri)
        PhotoMeta(
            uri = uri,
            takenAt = exif.takenAt ?: 0L,
            latitude = exif.latitude,
            longitude = exif.longitude,
            isCameraPhoto = exif.isCameraPhoto
        )
    }

    /** EXIF에서 읽어온 사진 메타데이터. 값이 없으면 takenAt=null, 좌표=0.0 */
    private data class ExifData(
        val takenAt: Long?,
        val latitude: Double,
        val longitude: Double,
        val isCameraPhoto: Boolean
    )

    /**
     * EXIF를 한 번 열어 촬영 시각과 위도/경도를 함께 읽는다.
     * - takenAt: EXIF DateTimeOriginal(epoch millis). 스크린샷·다운로드 등 없으면 null
     * - 위도/경도: GPS 정보 없으면 0.0
     */
    private fun readExif(uri: String): ExifData {
        return try {
            context.contentResolver.openInputStream(uri.toUri())?.use { input ->
                val exif = ExifInterface(input)

                val latLon = FloatArray(2)
                val (lat, lon) =
                    if (exif.getLatLong(latLon)) {
                        latLon[0].toDouble() to latLon[1].toDouble()
                    } else {
                        0.0 to 0.0
                    }

                // 카메라 제조사/기종 태그가 있으면 직접 촬영한 사진으로 판별
                val isCameraPhoto =
                    exif.getAttribute(ExifInterface.TAG_MAKE) != null ||
                        exif.getAttribute(ExifInterface.TAG_MODEL) != null

                ExifData(
                    takenAt = exif.dateTimeOriginal,
                    latitude = lat,
                    longitude = lon,
                    isCameraPhoto = isCameraPhoto
                )
            } ?: ExifData(null, 0.0, 0.0, false)
        } catch (e: Exception) {
            ExifData(null, 0.0, 0.0, false)
        }
    }
}
