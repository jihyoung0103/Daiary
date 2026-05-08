package com.smu.daiary.data.weather

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.smu.daiary.diary.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

// OpenWeatherMap에서 발급받은 API 키를 여기에 입력하세요.
// 발급: https://openweathermap.org/api → 무료 회원가입 후 My API Keys
private const val API_KEY = "f5569a69e61d72b030f10a97a0bf0f4c"
private const val BASE_URL = "https://api.openweathermap.org/data/2.5/weather"

class WeatherDataSource(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun fetchTodayWeather(): WeatherData {
        // 1. 현재 위치(위도/경도) 가져오기
        val cts = CancellationTokenSource()
        val location = fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
            .await()
            ?: throw IllegalStateException("위치를 가져올 수 없습니다. 위치 권한을 확인해주세요.")

        // 2. OpenWeatherMap API 호출
        val url = "$BASE_URL?lat=${location.latitude}&lon=${location.longitude}" +
                "&appid=$API_KEY&units=metric&lang=kr"

        val response = withContext(Dispatchers.IO) {
            URL(url).readText()
        }

        // 3. JSON 파싱 → WeatherData 반환
        val json = JSONObject(response)
        val weatherObj = json.getJSONArray("weather").getJSONObject(0)
        val main = json.getJSONObject("main")

        return WeatherData(
            description = weatherObj.getString("description"), // 예: "맑음", "흐림"
            temperature = main.getDouble("temp"),              // 섭씨
            humidity = main.getInt("humidity"),                // %
            city = json.getString("name")                      // 도시명
        )
    }
}
