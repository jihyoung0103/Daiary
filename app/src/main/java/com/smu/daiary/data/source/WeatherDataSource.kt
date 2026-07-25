package com.smu.daiary.data.source

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.smu.daiary.BuildConfig
import com.smu.daiary.data.model.WeatherData
import com.smu.daiary.data.model.WeatherSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.time.LocalDate

private val API_KEY get() = BuildConfig.OPENWEATHER_API_KEY
private const val CURRENT_URL = "https://api.openweathermap.org/data/2.5/weather"
private const val FORECAST_URL = "https://api.openweathermap.org/data/2.5/forecast"

/** OpenWeatherMap 응답 description을 앱 내부 canonical 이름으로 매핑. */
private fun mapToCanonical(description: String): String {
    val d = description.lowercase().trim()
    return when {
        d.contains("맑") || d.contains("clear") || d.contains("sunny") -> "맑음"
        d.contains("비") || d.contains("rain") || d.contains("drizzle") ||
            d.contains("thunder") || d.contains("뇌우") -> "비"
        d.contains("눈") || d.contains("snow") || d.contains("sleet") -> "눈"
        d.contains("바람") || d.contains("wind") -> "바람"
        d.contains("흐") || d.contains("구름") || d.contains("cloud") -> "흐림"
        else -> "맑음"
    }
}

/**
 * 날씨 데이터 수집 소스.
 *
 * 사용 패턴:
 * - 오늘 스냅샷 ([fetchTodaySnapshot]): WeatherCollectionWorker가 하루 2번(9시, 15시) 호출
 * - 내일 예보 ([fetchTomorrow]): 사용자 일기 작성 시점에 WriteViewModel이 호출
 *
 * 위치는 [FusedLocationProviderClient.lastLocation] 캐시로 얻는다.
 * 백그라운드 위치 권한(ACCESS_BACKGROUND_LOCATION) 없이도 동작하도록 하기 위함이며,
 * 사용자가 앱을 사용하는 과정에서 위치 캐시가 갱신된다.
 */
class WeatherDataSource(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    /**
     * 백그라운드 워커용. 마지막으로 캐시된 위치로 현재 날씨 스냅샷을 반환.
     * 위치 캐시가 없으면 예외 발생 → 워커는 재시도.
     */
    @SuppressLint("MissingPermission")
    suspend fun fetchTodaySnapshot(): WeatherSnapshot {
        val (lat, lon) = lastKnownLocation()
            ?: throw IllegalStateException("위치 캐시가 비어있음. 앱을 켜서 위치 갱신 필요.")
        return fetchCurrent(lat, lon)
    }

    /**
     * 사용자 작성 시점용. 내일 정오 근처 예보를 조회.
     * 오늘 날씨는 백그라운드가 이미 쌓아놓은 스냅샷을 Firestore에서 읽어 쓴다.
     */
    @SuppressLint("MissingPermission")
    suspend fun fetchTomorrow(): WeatherData {
        val (lat, lon) = lastKnownLocation()
            ?: throw IllegalStateException("위치 캐시가 비어있음. 위치 권한 및 GPS 확인 필요.")
        return fetchTomorrowSlot(lat, lon)
    }

    /** 마지막으로 캐시된 위치. 없으면 null. */
    private suspend fun lastKnownLocation(): Pair<Double, Double>? {
        val loc = fusedLocationClient.lastLocation.await() ?: return null
        return loc.latitude to loc.longitude
    }

    /** 현재 날씨 API 호출 → 스냅샷 반환. */
    private suspend fun fetchCurrent(lat: Double, lon: Double): WeatherSnapshot {
        val url = "$CURRENT_URL?lat=$lat&lon=$lon&appid=$API_KEY&units=metric&lang=kr"
        val response = withContext(Dispatchers.IO) { URL(url).readText() }
        val json = JSONObject(response)
        val weatherObj = json.getJSONArray("weather").getJSONObject(0)
        val main = json.getJSONObject("main")

        return WeatherSnapshot(
            timestamp = System.currentTimeMillis(),
            description = mapToCanonical(weatherObj.getString("description")),
            temperature = main.getDouble("temp"),
            humidity = main.getInt("humidity"),
            city = json.getString("name")
        )
    }

    /** 내일 정오 슬롯 예보. WeatherData에 tomorrow* 필드만 채워 반환. */
    private suspend fun fetchTomorrowSlot(lat: Double, lon: Double): WeatherData {
        val url = "$FORECAST_URL?lat=$lat&lon=$lon&appid=$API_KEY&units=metric&lang=kr"
        val response = withContext(Dispatchers.IO) { URL(url).readText() }
        val json = JSONObject(response)
        val list = json.getJSONArray("list")

        val tomorrow = LocalDate.now().plusDays(1).toString() // "YYYY-MM-DD"

        // 내일 날짜 슬롯 중 정오(12:00)에 가장 가까운 것 선택
        var tomorrowSlot: JSONObject? = null
        for (i in 0 until list.length()) {
            val item = list.getJSONObject(i)
            val dtTxt = item.getString("dt_txt") // 예: "2024-01-15 12:00:00"
            if (dtTxt.startsWith(tomorrow)) {
                tomorrowSlot = item
                if (dtTxt.contains("12:00:00")) break
            }
        }

        val weatherObj = tomorrowSlot?.getJSONArray("weather")?.getJSONObject(0)
        val main = tomorrowSlot?.getJSONObject("main")

        return WeatherData(
            tomorrowDescription = weatherObj?.getString("description")?.let { mapToCanonical(it) } ?: "",
            tomorrowTemperature = main?.getDouble("temp") ?: 0.0,
            tomorrowHumidity = main?.getInt("humidity") ?: 0
        )
    }
}
