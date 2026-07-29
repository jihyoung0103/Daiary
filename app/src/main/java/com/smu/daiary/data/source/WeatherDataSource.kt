package com.smu.daiary.data.source

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.smu.daiary.BuildConfig
import com.smu.daiary.data.model.WeatherData
import com.smu.daiary.data.model.WeatherSnapshot
import com.smu.daiary.util.DiaryDateUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private val API_KEY get() = BuildConfig.OPENWEATHER_API_KEY
private const val CURRENT_URL = "https://api.openweathermap.org/data/2.5/weather"
private const val FORECAST_URL = "https://api.openweathermap.org/data/2.5/forecast"

/** 모바일 네트워크 기준. 이 값을 넘기면 날씨를 포기하고 나머지 수집을 진행시킨다. */
private const val CONNECT_TIMEOUT_MS = 5_000
private const val READ_TIMEOUT_MS = 10_000

/**
 * OpenWeather condition code를 앱 내부 canonical 이름 5종으로 매핑.
 *
 * 분기 순서에 의미가 있다:
 * - 511(우박)은 5xx지만 성격이 언 비라 눈으로 보낸다. 반드시 비(5xx)보다 위여야 한다.
 * - 7xx(박무·안개·황사·돌풍)는 5종 어디에도 안 맞아 흐림으로 흡수한다.
 * - 801(구름 조금, 11~25%)까지는 맑음으로 본다. 체감상 맑은 날이다.
 *
 */
internal fun mapToCanonical(id: Int): String = when (id) {
    in 200..232      -> "뇌우"
    511, in 600..622 -> "눈"
    in 300..321, in 500..531 -> "비"
    in 700..781      -> "흐림"
    800, 801         -> "맑음"
    in 802..804      -> "흐림"
    else             -> "흐림"
}

/**
 * 날씨 데이터 수집 소스.
 *
 * 사용 패턴:
 * - 오늘 스냅샷 ([fetchTodaySnapshot]): WeatherCollectionWorker가 하루 1번 호출
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

    /**
     * 날씨 API 공통 호출. 두 가지를 반드시 여기서 처리한다.
     *
     * 1. **타임아웃** — HttpURLConnection 기본값은 0(무제한)이다. 응답 없는 네트워크에서
     *    URL.readText()가 영영 매달리면 loadBlocks의 await 사슬이 멈춰 블록 선택 화면
     *    로딩 스피너가 끝나지 않는다.
     * 2. **상태 코드** — 200을 확인하지 않으면 OpenWeather의 에러 본문
     *    ({"cod":401,"message":"Invalid API key"})을 그대로 파싱하다 엉뚱한
     *    JSONException이 나서, 키 만료나 rate limit이 "날씨 정보 없음"으로 조용히 묻힌다.
     */
    private suspend fun fetchJson(url: String): JSONObject = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
        }
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                // 원인을 로그에 남긴다. 여기가 비면 키 만료를 추적할 방법이 없다.
                val detail = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IOException("날씨 API 실패 (${conn.responseCode}) $detail")
            }
            JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conn.disconnect()
        }
    }

    /** 현재 날씨 API 호출 → 스냅샷 반환. */
    private suspend fun fetchCurrent(lat: Double, lon: Double): WeatherSnapshot {
        val url = "$CURRENT_URL?lat=$lat&lon=$lon&appid=$API_KEY&units=metric&lang=kr"
        val json = fetchJson(url)
        val weatherObj = json.getJSONArray("weather").getJSONObject(0)
        val main = json.getJSONObject("main")

        return WeatherSnapshot(
            timestamp = System.currentTimeMillis(),
            description = mapToCanonical(weatherObj.getInt("id")),
            temperature = main.getDouble("temp"),
            humidity = main.getInt("humidity"),
            city = json.getString("name")
        )
    }

    /** 내일 정오 슬롯 예보. WeatherData에 tomorrow* 필드만 채워 반환. */
    private suspend fun fetchTomorrowSlot(lat: Double, lon: Double): WeatherData {
        val url = "$FORECAST_URL?lat=$lat&lon=$lon&appid=$API_KEY&units=metric&lang=kr"
        val json = fetchJson(url)
        val list = json.getJSONArray("list")

        // 일기 기준일의 다음 날. LocalDate.now()를 쓰면 새벽 0~4시에 어제 일기를
        // 쓰는 동안 "내일 날씨"가 모레 예보로 조회된다.
        val tomorrow = DiaryDateUtil.diaryDate().plusDays(1).toString() // "YYYY-MM-DD"

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
            // 슬롯을 못 찾으면 빈 문자열. 여기서 else 분기("흐림")로 흘리면
            // "예보 없음"이 "흐림 예보"로 둔갑한다. 호출부가 isNotBlank()로 거른다.
            tomorrowDescription = weatherObj?.optInt("id", -1)?.takeIf { it > 0 }?.let { mapToCanonical(it) } ?: "",
            tomorrowTemperature = main?.getDouble("temp") ?: 0.0,
            tomorrowHumidity = main?.getInt("humidity") ?: 0
        )
    }
}
