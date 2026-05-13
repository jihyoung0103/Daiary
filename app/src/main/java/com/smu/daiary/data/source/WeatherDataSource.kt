package com.smu.daiary.data.source

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.smu.daiary.data.model.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.time.LocalDate

// OpenWeatherMap에서 발급받은 API 키를 여기에 입력하세요.
// 발급: https://openweathermap.org/api → 무료 회원가입 후 My API Keys
private const val API_KEY = "f5569a69e61d72b030f10a97a0bf0f4c"
private const val CURRENT_URL = "https://api.openweathermap.org/data/2.5/weather"
private const val FORECAST_URL = "https://api.openweathermap.org/data/2.5/forecast"

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

class WeatherDataSource(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun fetchWeather(): WeatherData {
        // 1. 현재 위치(위도/경도) 가져오기
        val cts = CancellationTokenSource()
        val location = fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
            .await()
            ?: throw IllegalStateException("위치를 가져올 수 없습니다. 위치 권한을 확인해주세요.")

        val lat = location.latitude
        val lon = location.longitude

        // 2. 오늘 날씨 + 내일 날씨 순차 호출
        val today = fetchToday(lat, lon)
        val tomorrow = fetchTomorrow(lat, lon)

        return today.copy(
            tomorrowDescription = tomorrow.tomorrowDescription,
            tomorrowTemperature = tomorrow.tomorrowTemperature,
            tomorrowHumidity = tomorrow.tomorrowHumidity
        )
    }

    // 오늘 현재 날씨 (/weather 엔드포인트)
    private suspend fun fetchToday(lat: Double, lon: Double): WeatherData {
        val url = "$CURRENT_URL?lat=$lat&lon=$lon&appid=$API_KEY&units=metric&lang=kr"
        val response = withContext(Dispatchers.IO) { URL(url).readText() }
        val json = JSONObject(response)
        val weatherObj = json.getJSONArray("weather").getJSONObject(0)
        val main = json.getJSONObject("main")

        return WeatherData(
            description = mapToCanonical(weatherObj.getString("description")),
            temperature = main.getDouble("temp"),
            humidity = main.getInt("humidity"),
            city = json.getString("name")
        )
    }

    // 내일 날씨 (/forecast 엔드포인트 → 내일 정오 슬롯 추출)
    private suspend fun fetchTomorrow(lat: Double, lon: Double): WeatherData {
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
