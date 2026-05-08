package com.smu.daiary.data.photo

import android.content.Context
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.smu.daiary.diary.PhotoMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

class PhotoDataSource(private val context: Context) {

    private fun todayRange(): Pair<Long, Long> {
        val today = LocalDate.now()
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
            MediaStore.Images.Media.DATA,       // 파일 절대 경로
            MediaStore.Images.Media.DATE_TAKEN  // 촬영 시각 (epoch millis)
        )
        val selection = "${MediaStore.Images.Media.DATE_TAKEN} BETWEEN ? AND ?"
        val selectionArgs = arrayOf(start.toString(), end.toString())

        val cursor = context.contentResolver.query(
            uri, projection, selection, selectionArgs,
            "${MediaStore.Images.Media.DATE_TAKEN} ASC"
        )

        cursor?.use {
            val dataIdx = it.getColumnIndex(MediaStore.Images.Media.DATA)
            val takenIdx = it.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)

            while (it.moveToNext()) {
                val filePath = it.getString(dataIdx) ?: continue
                val takenAt = it.getLong(takenIdx)

                // EXIF에서 GPS 좌표 추출
                val (lat, lon) = extractGps(filePath)

                photos.add(
                    PhotoMeta(
                        uri = filePath,
                        takenAt = takenAt,
                        latitude = lat,
                        longitude = lon
                    )
                )
            }
        }

        photos
    }

    // EXIF에서 위도/경도 파싱. GPS 정보 없는 사진은 0.0으로 반환
    private fun extractGps(filePath: String): Pair<Double, Double> {
        return try {
            val exif = ExifInterface(filePath)
            val latLon = FloatArray(2)
            if (exif.getLatLong(latLon)) {
                latLon[0].toDouble() to latLon[1].toDouble()
            } else {
                0.0 to 0.0
            }
        } catch (e: Exception) {
            0.0 to 0.0
        }
    }
}
