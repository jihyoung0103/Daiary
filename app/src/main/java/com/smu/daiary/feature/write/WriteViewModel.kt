package com.smu.daiary.feature.write

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smu.daiary.data.model.DiaryEntry
import com.smu.daiary.data.repository.DailyDataRepository
import com.smu.daiary.feature.write.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.smu.daiary.data.repository.DiaryRepository
import com.smu.daiary.data.model.CalendarEvent
import com.smu.daiary.data.source.CalendarDataSource
import com.smu.daiary.data.source.PhotoDataSource
import com.smu.daiary.data.source.WeatherDataSource
import com.smu.daiary.R
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import com.smu.daiary.util.DiaryDateUtil
import com.smu.daiary.data.source.EncodedImage


private const val TAG = "WriteViewModel"

/**
 * 일기 작성 화면 전체의 상태와 비즈니스 로직을 담당하는 ViewModel.
 *
 * 주요 책임:
 * 1. 오늘 데이터 수집 — 날씨·캘린더·사진·결제를 병렬로 수집해 ContentBlock 목록 구성
 * 2. 블록/사진/결제 선택 상태 관리 — 사용자가 AI에 넘길 데이터를 취사선택
 * 3. AI 초안 생성 — 선택된 블록 + MBTI + 사진 분석 결과 + 최근 문체 샘플을 Claude API에 전달
 * 4. 초안 편집 & Firestore 저장 — 사용자가 수정한 내용을 DiaryEntry로 변환해 저장
 * 5. 기존 일기 편집 지원 — 홈에서 넘어온 DiaryEntry를 draft로 복원
 */
class WriteViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // ─────────────────────────────────────────────────────────────
    // 내부 유틸
    // ─────────────────────────────────────────────────────────────

    /**
     * SharedPreferences 언어 설정("한국어"/"English")에 맞는 로케일 Context를 반환.
     * Application context는 MainActivity.attachBaseContext의 로케일 재설정 영향을 받지
     * 않으므로, 매번 직접 Configuration을 덮어써야 한다.
     */
    private fun localizedContext(): Context {
        val prefs = context.getSharedPreferences("daiary_settings", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "한국어") ?: "한국어"
        val locale = if (lang == "English") Locale("en") else Locale("ko")
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    // ─────────────────────────────────────────────────────────────
    // Repositories & DataSources
    // ─────────────────────────────────────────────────────────────

    private val diaryRepository = DiaryRepository()
    private val dailyDataRepository = DailyDataRepository()
    private val aiRepository = com.smu.daiary.data.repository.AiRepository()
    private val weatherDataSource = WeatherDataSource(context)
    private val photoDataSource = PhotoDataSource(context)
    private val calendarDataSource = CalendarDataSource(context)
    private val healthDataSource = com.smu.daiary.data.source.HealthDataSource(context)
    // ─────────────────────────────────────────────────────────────
    // UI State — 블록 / 사진 / 결제
    // ─────────────────────────────────────────────────────────────

    /** AI에 전달할 데이터 블록 목록 (날씨·캘린더·사진·결제). 수집 후 채워짐 */
    private val _blocks = MutableStateFlow<List<ContentBlock>>(emptyList())
    val blocks: StateFlow<List<ContentBlock>> = _blocks.asStateFlow()

    /** 오늘 찍힌 사진 목록 — 개별 선택/해제 가능 */
    private val _photos = MutableStateFlow<List<PhotoSelectableItem>>(emptyList())
    val photos = _photos.asStateFlow()

    /** 오늘 캘린더 일정 목록 — 개별 선택/해제 가능 */
    private val _calendarEvents = MutableStateFlow<List<CalendarSelectableItem>>(emptyList())
    val calendarEvents = _calendarEvents.asStateFlow()

    /** 내일/모레 향후 일정 목록 — 개별 선택/해제 가능 */
    private val _upcomingEvents = MutableStateFlow<List<CalendarSelectableItem>>(emptyList())
    val upcomingEvents = _upcomingEvents.asStateFlow()

    /** 오늘 결제 내역 목록 — 개별 선택/해제 가능 */
    private val _payments = MutableStateFlow<List<PaymentSelectableItem>>(emptyList())
    val payments = _payments.asStateFlow()

    // ─────────────────────────────────────────────────────────────
    // UI State — 로딩 / 생성 / 저장
    // ─────────────────────────────────────────────────────────────

    /** 데이터 수집(loadBlocks) 진행 중 여부 */
    private val _isLoadingBlocks = MutableStateFlow(false)
    val isLoadingBlocks: StateFlow<Boolean> = _isLoadingBlocks.asStateFlow()

    /** AI가 생성한 일기 초안. null이면 아직 생성 전 */
    private val _draft = MutableStateFlow<DiaryDraft?>(null)
    val draft: StateFlow<DiaryDraft?> = _draft.asStateFlow()

    /** Firestore 저장 진행 중 여부 */
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // ─────────────────────────────────────────────────────────────
    // UI State — 날씨·감정 선택
    // ─────────────────────────────────────────────────────────────

    /** 사용자가 직접 선택한 날씨 이모지/텍스트 */
    private val _selectedWeather = MutableStateFlow<String?>(null)
    val selectedWeather: StateFlow<String?> = _selectedWeather.asStateFlow()

    /** 사용자가 직접 선택한 감정 이모지/텍스트 */
    private val _selectedEmotion = MutableStateFlow<String?>(null)
    val selectedEmotion: StateFlow<String?> = _selectedEmotion.asStateFlow()

    // ─────────────────────────────────────────────────────────────
    // UI State — AI 초안 생성
    // ─────────────────────────────────────────────────────────────

    /** 질문 생성(prepareGeneration) 진행 중 여부 */
    private val _isGeneratingQuestions = MutableStateFlow(false)
    val isGeneratingQuestions: StateFlow<Boolean> = _isGeneratingQuestions.asStateFlow()

    /** 생성된 맥락 질문 목록. null = 아직 생성 전, emptyList = 질문 없음 */
    private val _contextQuestions = MutableStateFlow<List<ContextQuestion>?>(null)
    val contextQuestions: StateFlow<List<ContextQuestion>?> = _contextQuestions.asStateFlow()

    /** Claude API 호출 진행 중 여부 */
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    /** AI 초안 생성 실패 시 메시지. UI에서 스낵바로 표시 */
    private val _generateError = MutableStateFlow<String?>(null)
    val generateError: StateFlow<String?> = _generateError.asStateFlow()

    /** 사진 다중 분석 결과 요약 (analyzePhotos 호출 결과) */
    private val _photoAnalysis = MutableStateFlow<String?>(null)
    val photoAnalysis: StateFlow<String?> = _photoAnalysis.asStateFlow()

    // ─────────────────────────────────────────────────────────────
    // 이벤트
    // ─────────────────────────────────────────────────────────────

    /** 저장 완료 일회성 이벤트 — UI에서 collect해 스낵바 표시 */
    private val _saveEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saveEvent: SharedFlow<Unit> = _saveEvent.asSharedFlow()

    // ─────────────────────────────────────────────────────────────
    // 내부 변수
    // ─────────────────────────────────────────────────────────────

    /** 편집 모드일 때 수정 대상 일기의 Firestore ID. null이면 신규 작성 */
    private val _existingEntryId = MutableStateFlow<String?>(null)

    /** 과거 날짜 일기 작성 시 대상 날짜. null이면 오늘(DiaryDateUtil.diaryDate()) 기준 */
    private var targetDate: LocalDate? = null

    private val _writingDate = MutableStateFlow<LocalDate>(DiaryDateUtil.diaryDate())
    val writingDate: StateFlow<LocalDate> = _writingDate.asStateFlow()

    fun setTargetDate(date: LocalDate?) {
        targetDate = date
        _writingDate.value = date ?: DiaryDateUtil.diaryDate()
    }

    /** AI 프롬프트에 문체 참고용으로 넘길 최근 일기 샘플 (최대 2개) */
    private var recentDiarySamples: String = ""

    /**
     * 실제 DataSource로부터 오늘 데이터를 수집하고
     * DailyDataRepository에 저장한 뒤 블록 목록을 구성합니다.
     * 각 항목은 병렬로 수집되어 실패해도 다른 항목에 영향을 주지 않습니다.
     */
    fun loadBlocks(userId: String) {
        viewModelScope.launch {
            loadRecentDiaryStyle(userId)

            _isLoadingBlocks.value = true
            _blocks.value = emptyList()
            _photos.value = emptyList()
            _calendarEvents.value = emptyList()
            _upcomingEvents.value = emptyList()
            _payments.value = emptyList()


            // 과거 날짜 지정 시 그 날짜, 아니면 오늘(오전 4시 이전이면 전날) 기준
            val target = targetDate
            val date = (target ?: DiaryDateUtil.diaryDate()).toString()
            val blocks = mutableListOf<ContentBlock>()


            // --- 날씨, 캘린더, 사진, 건강 병렬 수집 ---
            val weatherDeferred = async { runCatching { weatherDataSource.fetchWeather() } }
            // 과거 날짜면 해당 날짜 일정만 조회, 오늘이면 3일치(오늘/내일/모레) 조회
            val calendarDeferred = async {
                runCatching {
                    if (target != null) calendarDataSource.fetchEventsForDate(target)
                    else calendarDataSource.fetchUpcomingEvents()
                }
            }
            val photoDeferred = async { runCatching { photoDataSource.fetchTodayPhotos() } }
            val healthDeferred = async { runCatching { healthDataSource.fetchTodayHealth() } }

            // 날씨 — 생성 시점 1회만 수집 (재시도 없음, 추후 백그라운드 정기 수집으로 이전 예정)
            weatherDeferred.await()
                .onSuccess { weather ->
                    Log.d(TAG, "🌤️ 날씨 수집 완료: ${weather.description} ${weather.temperature}°C")
                    dailyDataRepository.updateWeather(userId, date, weather)
                    blocks.add(ContentBlock(
                        id = "weather", type = BlockType.WEATHER,
                        content = localizedContext().getString(
                            R.string.block_weather_content,
                            weather.city,
                            localizedWeatherDescription(weather.description),
                            weather.temperature.toInt(),
                            weather.humidity
                        )
                    ))
                }
                .onFailure {
                    Log.w(TAG, "⚠️ 날씨 수집 실패", it)
                    blocks.add(ContentBlock(id = "weather", type = BlockType.WEATHER, content = localizedContext().getString(R.string.block_weather_unavailable)))
                }

            // 캘린더
            calendarDeferred.await()
                .onSuccess { events ->
                    Log.d(TAG, "📅 캘린더 수집 완료: ${events.size}개")
                    dailyDataRepository.updateCalendar(userId, date, events)
                    if (events.isEmpty()) {
                        blocks.add(ContentBlock(
                            id = "calendar_summary", type = BlockType.CALENDAR,
                            content = localizedContext().getString(R.string.block_calendar_empty),
                            isSelected = false
                        ))
                    } else {
                        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                        val diaryDate = target ?: DiaryDateUtil.diaryDate()
                        val zone = ZoneId.systemDefault()

                        fun toSelectableItem(event: CalendarEvent, id: Int): CalendarSelectableItem {
                            val eventDate = Instant.ofEpochMilli(event.startTime).atZone(zone).toLocalDate()
                            val dayLabel = when (eventDate) {
                                diaryDate             -> "오늘"
                                diaryDate.plusDays(1) -> "내일"
                                diaryDate.plusDays(2) -> "모레"
                                else -> eventDate.format(DateTimeFormatter.ofPattern("M/d"))
                            }
                            val startStr = Instant.ofEpochMilli(event.startTime).atZone(zone).format(timeFormatter)
                            val endStr = Instant.ofEpochMilli(event.endTime).atZone(zone).format(timeFormatter)
                            val locationPart = if (event.location.isNotBlank()) " · ${event.location}" else ""
                            return CalendarSelectableItem(
                                id = id,
                                displayText = "[$dayLabel] ${event.title} $startStr~$endStr$locationPart",
                                startTime = event.startTime,
                                isSelected = true
                            )
                        }

                        // 과거 날짜: 전체가 해당 날짜 일정 / 오늘: 날짜별 분리
                        val todayEvents = if (target != null) events
                            else events.filter { Instant.ofEpochMilli(it.startTime).atZone(zone).toLocalDate() == diaryDate }
                        val futureEvents = if (target != null) emptyList()
                            else events.filter { Instant.ofEpochMilli(it.startTime).atZone(zone).toLocalDate() > diaryDate }

                        // 오늘 일정 블록
                        if (todayEvents.isNotEmpty()) {
                            _calendarEvents.value = todayEvents.mapIndexed { i, event -> toSelectableItem(event, i) }
                            blocks.add(ContentBlock(
                                id = "calendar_summary", type = BlockType.CALENDAR,
                                content = buildCalendarSummary(_calendarEvents.value),
                                isSelected = true
                            ))
                        } else {
                            blocks.add(ContentBlock(
                                id = "calendar_summary", type = BlockType.CALENDAR,
                                content = localizedContext().getString(R.string.block_calendar_empty),
                                isSelected = false
                            ))
                        }

                        // 향후 일정 블록 (오늘 일기 작성 시에만)
                        if (futureEvents.isNotEmpty()) {
                            _upcomingEvents.value = futureEvents.mapIndexed { i, event -> toSelectableItem(event, i) }
                            blocks.add(ContentBlock(
                                id = "calendar_upcoming", type = BlockType.CALENDAR_UPCOMING,
                                content = buildUpcomingCalendarSummary(_upcomingEvents.value),
                                isSelected = true
                            ))
                        }
                    }
                }
                .onFailure {
                    Log.w(TAG, "⚠️ 캘린더 수집 실패 (권한 문제)", it)
                    blocks.add(ContentBlock(
                        id = "calendar_summary", type = BlockType.CALENDAR,
                        content = localizedContext().getString(R.string.block_calendar_unavailable),
                        isSelected = false
                    ))
                }

            // 사진
            photoDeferred.await()
                .onSuccess { photos ->
                    Log.d(TAG, "🖼️ 사진 수집 완료: ${photos.size}장")
                    dailyDataRepository.updatePhotos(userId, date, photos)

                    _photos.value = photos.map { photo ->
                        PhotoSelectableItem(
                            uri = photo.uri,
                            isSelected = true
                        )
                    }

                        blocks.add(
                            ContentBlock(
                                id = "photo",
                                type = BlockType.PHOTO,
                                content = localizedContext().getString(R.string.block_photo_selection_content, photos.size, photos.size),
                                isSelected = photos.isNotEmpty()
                                )
                            )

                }
                .onFailure {
                    Log.w(TAG, "⚠️ 사진 수집 실패 (권한 문제)", it)
                    blocks.add(
                        ContentBlock(
                            id = "photo_error",
                            type = BlockType.PHOTO,
                            content = "사진을 불러오지 못했습니다"
                        )
                    )
                }

            // 건강 데이터 (Health Connect)
            healthDeferred.await()
                .onSuccess { health ->
                    Log.d(TAG, "🏃 건강 수집 완료 | 걸음=${health.steps} | 수면=${health.sleepDurationMinutes}분")
                    dailyDataRepository.updateHealth(userId, date, health)

                    // 데이터가 모두 0이면 블록 표시 안 함 (Health Connect 미설정 사용자)
                    if (health.steps > 0 || health.sleepDurationMinutes > 0) {
                        val stepsText = if (health.steps > 0) "${String.format("%,d", health.steps)}보" else null
                        val sleepText = if (health.sleepDurationMinutes > 0) {
                            val h = health.sleepDurationMinutes / 60
                            val m = health.sleepDurationMinutes % 60
                            "수면 ${h}시간 ${m}분"
                        } else null

                        val content = listOfNotNull(stepsText, sleepText).joinToString(" · ")
                        blocks.add(ContentBlock(id = "health", type = BlockType.HEALTH, content = content))
                    }
                }
                .onFailure { Log.w(TAG, "⚠️ 건강 수집 실패", it) }

            // 결제 내역 (NotificationListenerService가 Firestore에 저장해둔 데이터를 읽어옴)
            dailyDataRepository.getDailyData(userId, date)
                .onSuccess { dailyData ->
                    val payments = dailyData?.payments ?: emptyList()

                    Log.d(TAG, "💳 결제 내역 로드 완료: ${payments.size}건")

                    if (payments.isNotEmpty()) {
                        val timeFormatter =
                            DateTimeFormatter.ofPattern("HH:mm")

                        _payments.value =
                            payments.mapIndexed { index, payment ->
                                val timeText =
                                    Instant.ofEpochMilli(payment.paidAt)
                                        .atZone(ZoneId.systemDefault())
                                        .format(timeFormatter)

                                val category =
                                    payment.category.ifBlank {
                                        "기타"
                                    }

                                PaymentSelectableItem(
                                    id = index,
                                    displayText = "$timeText ${categoryEmoji(category)} ${payment.merchant} ${String.format("%,d", payment.amount)}원",
                                    amount = payment.amount,
                                    category = category,
                                    isSelected = true
                                )
                            }

                        blocks.add(
                            ContentBlock(
                                id = "payment_summary",
                                type = BlockType.PAYMENT,
                                content = buildPaymentSummary(_payments.value),
                                isSelected = true
                            )
                        )
                    }
                }
                .onFailure {
                    Log.w(TAG, "⚠️ 결제 내역 로드 실패", it)
                }
                .onFailure { Log.w(TAG, "⚠️ 결제 내역 로드 실패", it) }

            Log.d(TAG, "✅ 전체 수집 완료 | 블록 수=${blocks.size}")
            _blocks.value = blocks
            _isLoadingBlocks.value = false
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 내부 유틸 — 날씨·결제 표시
    // ─────────────────────────────────────────────────────────────

    /** 한국어 canonical 날씨명을 현재 언어 설정에 맞는 문자열로 변환 */
    private fun localizedWeatherDescription(canonical: String): String = when (canonical) {
        "맑음" -> localizedContext().getString(R.string.weather_sunny)
        "흐림" -> localizedContext().getString(R.string.weather_cloudy)
        "비"   -> localizedContext().getString(R.string.weather_rain)
        "눈"   -> localizedContext().getString(R.string.weather_snow)
        "바람" -> localizedContext().getString(R.string.weather_wind)
        else   -> canonical
    }
    /** 사진 URI를 Base64 문자열로 변환 — Claude Vision API 전달용 */
    private fun encodeImage(uriString: String): EncodedImage? {
        return try {
            val uri = Uri.parse(uriString)

            val mimeTypeFromResolver =
                context.contentResolver.getType(uri)

            val bytes =
                context.contentResolver
                    .openInputStream(uri)
                    ?.use { input ->
                        input.readBytes()
                    } ?: return null

            val mediaType =
                detectImageMediaType(bytes, mimeTypeFromResolver)

            val base64 =
                Base64.encodeToString(
                    bytes,
                    Base64.NO_WRAP
                )

            EncodedImage(
                base64 = base64,
                mediaType = mediaType
            )

        } catch (e: Exception) {
            Log.e(
                TAG,
                "이미지 인코딩 실패",
                e
            )
            null
        }
    }

    private fun detectImageMediaType(
        bytes: ByteArray,
        resolverMimeType: String?
    ): String {
        val normalized =
            when (resolverMimeType?.lowercase()) {
                "image/jpeg", "image/jpg" -> "image/jpeg"
                "image/png" -> "image/png"
                "image/webp" -> "image/webp"
                else -> null
            }

        if (normalized != null) return normalized

        return when {
            bytes.size >= 3 &&
                    bytes[0] == 0xFF.toByte() &&
                    bytes[1] == 0xD8.toByte() &&
                    bytes[2] == 0xFF.toByte() -> "image/jpeg"

            bytes.size >= 4 &&
                    bytes[0] == 0x89.toByte() &&
                    bytes[1] == 0x50.toByte() &&
                    bytes[2] == 0x4E.toByte() &&
                    bytes[3] == 0x47.toByte() -> "image/png"

            else -> "image/jpeg"
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 블록 / 사진 / 결제 선택 상태 관리
    // ─────────────────────────────────────────────────────────────

    /** 블록 선택 토글 — 날씨·캘린더 등 단일 블록 on/off */
    fun toggleBlock(id: String) {
        _blocks.update { list ->
            list.map { if (it.id == id) it.copy(isSelected = !it.isSelected) else it }
        }
    }

    /**
     * 결제 블록 내용을 selectedPayments 기준으로 필터링.
     * PaymentDetailSelector에서 체크박스 변경 시 호출됨 (현재는 togglePayment로 대체되는 추세)
     */
    fun updatePaymentSelection(
        selectedPayments: List<String>
    ) {

        _blocks.update { list ->

            list.map { block ->

                if (
                    block.type ==
                    BlockType.PAYMENT
                ) {

                    val lines =
                        block.content
                            .split("\n")

                    val header =
                        lines.first()

                    val filtered =
                        lines.drop(1)
                            .filter {

                                selectedPayments.any {
                                        p ->
                                    it.contains(p)
                                }

                            }

                    block.copy(
                        content =
                            buildString {
                                appendLine(header)
                                filtered.forEach {
                                    appendLine(it)
                                }
                            }
                    )

                }

                else {
                    block
                }

            }

        }

    }

    /** 사진 개별 선택 토글 — BlockSelectionScreen의 PhotoDetailSelector와 연결 */
    fun togglePhoto(uri: String) {
        _photos.update { list ->
            list.map { photo ->
                if (photo.uri == uri) {
                    photo.copy(isSelected = !photo.isSelected)
                } else {
                    photo
                }
            }
        }
    }

    /** 블록 선택 화면에서 사진을 목록에서 제거 */
    fun removeSelectablePhoto(uri: String) {
        _photos.update { list -> list.filter { it.uri != uri } }
        syncPhotoBlockSelection()
    }

    /** 갤러리에서 직접 고른 사진을 목록에 추가 (중복 방지 포함) */
    fun addSelectablePhoto(uri: String) {
        val alreadyExists =
            _photos.value.any {
                it.uri == uri
            }

        if (alreadyExists) return

        _photos.update { list ->
            list + PhotoSelectableItem(
                uri = uri,
                isSelected = true
            )
        }

        syncPhotoBlockSelection()
    }

    /** 향후 일정 개별 선택 토글 → 완료 후 향후 일정 블록 요약 텍스트 자동 갱신 */
    fun toggleUpcomingEvent(id: Int) {
        _upcomingEvents.update { list ->
            list.map { if (it.id == id) it.copy(isSelected = !it.isSelected) else it }
        }
        syncUpcomingBlockSelection()
    }

    private fun syncUpcomingBlockSelection() {
        val selected = _upcomingEvents.value.filter { it.isSelected }
        _blocks.update { list ->
            list.map { block ->
                if (block.type == BlockType.CALENDAR_UPCOMING) {
                    block.copy(
                        content = buildUpcomingCalendarSummary(_upcomingEvents.value),
                        isSelected = selected.isNotEmpty()
                    )
                } else block
            }
        }
    }

    private fun buildUpcomingCalendarSummary(events: List<CalendarSelectableItem>): String {
        val selected = events.filter { it.isSelected }
        if (selected.isEmpty()) return "선택된 향후 일정이 없습니다"
        val lines = selected.joinToString("\n") { "- ${it.displayText}" }
        return "향후 일정 ${selected.size}개\n$lines"
    }

    /** 캘린더 일정 개별 선택 토글 → 완료 후 캘린더 블록 요약 텍스트 자동 갱신 */
    fun toggleCalendarEvent(id: Int) {
        _calendarEvents.update { list ->
            list.map { if (it.id == id) it.copy(isSelected = !it.isSelected) else it }
        }
        syncCalendarBlockSelection()
    }

    /**
     * _calendarEvents 선택 상태를 기준으로 캘린더 블록의 content와
     * isSelected(선택 건이 하나도 없으면 false)를 동기화.
     */
    private fun syncCalendarBlockSelection() {
        val selected = _calendarEvents.value.filter { it.isSelected }
        _blocks.update { list ->
            list.map { block ->
                if (block.type == BlockType.CALENDAR) {
                    block.copy(
                        content = buildCalendarSummary(_calendarEvents.value),
                        isSelected = selected.isNotEmpty()
                    )
                } else block
            }
        }
    }

    /** 선택된 일정 항목으로 요약 문자열 생성 */
    private fun buildCalendarSummary(events: List<CalendarSelectableItem>): String {
        val selected = events.filter { it.isSelected }
        if (selected.isEmpty()) return localizedContext().getString(R.string.block_calendar_empty)
        val lines = selected.joinToString("\n") { "- ${it.displayText}" }
        return "일정 ${selected.size}개\n$lines"
    }

    /** 결제 건 개별 선택 토글 → 완료 후 결제 블록 요약 텍스트 자동 갱신 */
    fun togglePayment(
        id: Int
    ) {

        _payments.update { list ->

            list.map { payment ->

                if (
                    payment.id == id
                ) {

                    payment.copy(
                        isSelected =
                            !payment.isSelected
                    )

                }

                else {
                    payment
                }

            }

        }

        syncPaymentBlockSelection()

    }

    /**
     * _payments 선택 상태를 기준으로 결제 블록의 content(요약 텍스트)와
     * isSelected(선택 건이 하나도 없으면 false)를 동기화.
     * togglePayment() 호출 시 내부적으로 실행됨.
     */
    private fun syncPaymentBlockSelection() {

        val selected =
            _payments.value
                .filter {
                    it.isSelected
                }

        _blocks.update { list ->

            list.map { block ->

                if (
                    block.type ==
                    BlockType.PAYMENT
                ) {

                    block.copy(

                        content =
                            buildPaymentSummary(
                                _payments.value
                            ),

                        isSelected =
                            selected.isNotEmpty()

                    )

                }

                else {
                    block
                }

            }

        }

    }

    /** 선택된 결제 항목으로 "N건 · 총 M원\n- 상세" 형태의 요약 문자열 생성 */
    private fun buildPaymentSummary(
        payments: List<PaymentSelectableItem>
    ): String {
        val selected =
            payments.filter { it.isSelected }

        if (selected.isEmpty()) {
            return "선택된 결제 내역이 없습니다"
        }

        val totalAmount =
            selected.sumOf { it.amount }

        val paymentLines =
            selected.joinToString("\n") {
                "- ${it.displayText}"
            }

        return "오늘 결제 ${selected.size}건 · 총 ${String.format("%,d", totalAmount)}원\n$paymentLines"
    }

    /** 결제 카테고리명 → 이모지 변환 (결제 블록 표시용) */
    private fun categoryEmoji(category: String): String {
        return when (category) {
            "카페" -> "☕"
            "편의점" -> "🛒"
            "교통" -> "🚌"
            "식사" -> "🍔"
            else -> "💳"
        }
    }


    // ─────────────────────────────────────────────────────────────
    // 사진 블록 동기화
    // ─────────────────────────────────────────────────────────────

    /** 사진 전체 선택 / 전체 해제 */
    fun setAllPhotosSelected(selected: Boolean) {
        _photos.update { list ->
            list.map { photo ->
                photo.copy(isSelected = selected)
            }
        }
    }

    /**
     * _photos 선택 상태를 기준으로 사진 블록의 content("N장 · 선택 M장")와
     * isSelected를 동기화. togglePhoto / addSelectablePhoto 호출 후 실행됨.
     */
    fun syncPhotoBlockSelection() {
        val totalCount = _photos.value.size
        val selectedCount = _photos.value.count { it.isSelected }
        val hasSelectedPhoto = _photos.value.any { it.isSelected }

        _blocks.update { list ->
            list.map { block ->
                if (block.type == BlockType.PHOTO) {
                    block.copy(
                        content = localizedContext().getString(R.string.block_photo_selection_content, totalCount, selectedCount),
                        isSelected = hasSelectedPhoto
                    )
                } else {
                    block
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // AI 초안 생성
    // ─────────────────────────────────────────────────────────────

    /**
     * 선택된 블록을 기반으로 Claude API에 일기 초안 생성을 요청.
     *
     * 처리 순서:
     * 1. 선택된 사진을 Base64로 변환 후 Claude Vision으로 분석 (photoSummary 추출)
     * 2. user_settings에서 MBTI 읽어오기
     * 3. AiRepository를 통해 Claude에 블록 + MBTI + 사진 요약 + 문체 샘플 전달
     * 4. 실패 시 fallbackTemplate으로 대체 초안 생성
     * 5. 완료 시 _draft에 결과 저장 → UI가 DraftPreviewScreen으로 자동 전환
     */
    /**
     * 블록 선택 완료 후 첫 번째 단계 — 선택된 블록을 분석해 맥락 질문을 생성.
     * 완료되면 _contextQuestions에 결과를 세팅하고 UI가 ContextQnAScreen으로 이동.
     * 질문이 0개면 빈 리스트가 세팅되어 질답 단계를 자동 스킵.
     */
    fun prepareGeneration() = viewModelScope.launch {
        _photoAnalysis.value = null
        val selected = _blocks.value.filter { it.isSelected }
        if (selected.isEmpty()) {
            _contextQuestions.value = emptyList()
            return@launch
        }
        _isGeneratingQuestions.value = true
        _contextQuestions.value = null
        try {
            val questions = aiRepository.generateContextQuestions(selected)
            _contextQuestions.value = questions
        } catch (e: Exception) {
            Log.e(TAG, "❌ 질문 생성 실패 — 질답 단계 스킵", e)
            _contextQuestions.value = emptyList()
        } finally {
            _isGeneratingQuestions.value = false
        }
    }

    /**
     * ContextQnAScreen에서 사용자가 답변을 완료하거나 건너뛴 뒤 호출.
     * 수집된 answers를 포함해 일기 초안 생성을 시작.
     */
    fun submitAnswers(answers: Map<String, String>) {
        generateDraft(qaAnswers = answers)
    }

    fun generateDraft(qaAnswers: Map<String, String> = emptyMap()) = viewModelScope.launch {
        val selected = _blocks.value.filter { it.isSelected }
        val today = (targetDate ?: DiaryDateUtil.diaryDate()).toString()

        if (selected.isEmpty()) {
            _draft.value = DiaryDraft(
                date = today,
                aiContent = "오늘 하루를 기록해보세요.",
                editedContent = "오늘 하루를 기록해보세요.",
                photos = emptyList()
            )
            return@launch
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _generateError.value = null

            if (selected.isEmpty()) {
                _draft.value = DiaryDraft(
                    date = today,
                    aiContent = "",
                    editedContent = "",
                    photos = emptyList()
                )
                return@launch
            }

            val prefs = context.getSharedPreferences("daiary_settings", android.content.Context.MODE_PRIVATE)
            val savedLang = prefs.getString("language", "한국어")
            val locale = if (savedLang == "English") "en" else "ko"
            android.util.Log.d(TAG, "🌐 저장된 언어: $savedLang → locale: $locale")

            val selectedEncodedImages =
                _photos.value
                    .filter { it.isSelected }
                    .mapNotNull { encodeImage(it.uri) }

            Log.d(TAG, "📸 선택된 사진 수: ${_photos.value.count { it.isSelected }}")
            Log.d(TAG, "📦 인코딩 성공 수: ${selectedEncodedImages.size}")

            val photoSummary = try {
                aiRepository.analyzePhotos(selectedEncodedImages)
            } catch (e: Exception) {
                Log.e(TAG, "❌ 사진 분석 실패", e)
                "사진 ${selectedEncodedImages.size}장이 선택됨"
            }

            Log.d(TAG, "📸 사진 분석 결과: $photoSummary")
            _photoAnalysis.value = photoSummary

            val mbti = getApplication<Application>()
                .getSharedPreferences(
                    "user_settings",
                    android.content.Context.MODE_PRIVATE
                )
                .getString("mbti", "INFP") ?: "INFP"

            val selectedDebugText = selected.joinToString("\n") {
                "- [${it.type.label}] ${it.content}"
            }

            Log.d(TAG, "===== SELECTED BLOCKS =====")
            Log.d(TAG, selectedDebugText)
            Log.d(TAG, "===========================")


            val result =
                aiRepository.generateDraft(
                    blocks = selected,
                    locale = locale,
                    mbti = mbti,
                    photoSummary = photoSummary,
                    recentDiarySamples = recentDiarySamples,
                    qaAnswers = qaAnswers
                )

            val content = result.getOrElse { fallbackTemplate(selected) }
            if (result.isFailure) {
                _generateError.value = if (locale == "en")
                    "AI generation failed. Using default template."
                else
                    "초안 생성에 실패했습니다. 기본 템플릿으로 대체합니다."
            }

            val selectedPhotoUris =
                _photos.value
                    .filter { it.isSelected }
                    .map { it.uri }

            _draft.value = DiaryDraft(
                date = today,
                aiContent = content,
                photos = selectedPhotoUris
            )

            _isGenerating.value = false
        }
    }




    private fun encodeImage(uri: Uri): EncodedImage? {
        return try {
            val mimeTypeFromResolver = context.contentResolver.getType(uri)

            val bytes = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            } ?: return null

            val mediaType = detectImageMediaType(bytes, mimeTypeFromResolver)
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

            EncodedImage(
                base64 = base64,
                mediaType = mediaType
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    /** AI 생성 실패 시 블록 내용을 단순 나열한 기본 초안 반환 */
    private fun fallbackTemplate(selected: List<ContentBlock>): String = buildString {
        appendLine(localizedContext().getString(R.string.draft_intro))
        appendLine()
        selected.forEach { block ->
            when (block.type) {
                BlockType.PAYMENT  -> appendLine(localizedContext().getString(R.string.draft_block_payment, block.content))
                BlockType.PHOTO    -> appendLine(localizedContext().getString(R.string.draft_block_photo, block.content))
                BlockType.CALENDAR          -> appendLine(localizedContext().getString(R.string.draft_block_calendar, block.content))
                BlockType.CALENDAR_UPCOMING -> appendLine(localizedContext().getString(R.string.draft_block_calendar, block.content))
                BlockType.HEALTH            -> appendLine(localizedContext().getString(R.string.draft_block_health, block.content))
                BlockType.WEATHER  -> appendLine(localizedContext().getString(R.string.draft_block_weather, block.content))
            }
        }
        appendLine()
        append(localizedContext().getString(R.string.draft_outro))
    }

    // ─────────────────────────────────────────────────────────────
    // 초안 편집 & 저장
    // ─────────────────────────────────────────────────────────────

    /** 에러 스낵바 닫힌 뒤 호출 — generateError 초기화 */
    fun clearGenerateError() { _generateError.value = null }

    /** DraftPreviewScreen에서 텍스트 수정 시 초안 content 업데이트 */
    fun updateEditedContent(content: String) {
        _draft.update { it?.copy(editedContent = content) }
    }

    /** 초안에 사진 URI 추가 (DraftPreviewScreen에서 추가 첨부 시) */
    fun addPhoto(uri: String) {
        _draft.update { it?.copy(photos = it.photos + uri) }
    }

    /** 초안에서 사진 URI 제거 */
    fun removePhoto(uri: String) {
        _draft.update { it?.copy(photos = it.photos.filter { p -> p != uri }) }
    }

    /**
     * 현재 초안을 DiaryEntry로 변환해 Firestore에 저장.
     * - existingEntryId가 있으면 updateDiary, 없으면 addDiary
     * - 저장 성공 시 saveEvent emit → UI가 홈으로 이동
     */
    fun saveDraft(userId: String, onComplete: (Boolean) -> Unit) {
        val d = _draft.value ?: return
        _isSaving.value = true
        viewModelScope.launch {
            val localDate = runCatching { LocalDate.parse(d.date) }.getOrNull()
            val formattedTitle = if (localDate != null)
                localizedContext().getString(R.string.diary_entry_title, localDate.year, localDate.monthValue, localDate.dayOfMonth)
            else d.date
            val mood = when (_selectedEmotion.value) {
                localizedContext().getString(R.string.emotion_joy),
                localizedContext().getString(R.string.emotion_excited) -> "happy"
                localizedContext().getString(R.string.emotion_sad),
                localizedContext().getString(R.string.emotion_angry) -> "sad"
                else -> "neutral"
            }
            val existingId = _existingEntryId.value

            val selectedPhotoUris = _photos.value
                .filter { it.isSelected }
                .map { it.uri }

            val entry = DiaryEntry(
                id = existingId ?: "",
                title = formattedTitle,
                content = d.editedContent ?: d.aiContent,
                date = d.date,
                mood = mood,
                emotion = _selectedEmotion.value ?: "",
                weather = _selectedWeather.value ?: "",
                photos = selectedPhotoUris
            )
            val result = if (existingId != null) {
                diaryRepository.updateDiary(userId, entry)
            } else {
                diaryRepository.addDiary(userId, entry)
            }
            _isSaving.value = false
            _draft.update { it?.copy(status = if (result.isSuccess) DraftStatus.SAVED else DraftStatus.IDLE) }
            if (result.isSuccess) _saveEvent.tryEmit(Unit)
            onComplete(result.isSuccess)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 날씨·감정 선택 / 편집 모드 진입 / 초기화
    // ─────────────────────────────────────────────────────────────

    fun updateWeatherSelection(weather: String?) { _selectedWeather.value = weather }
    fun updateEmotionSelection(emotion: String?) { _selectedEmotion.value = emotion }

    /**
     * 홈 화면에서 기존 일기를 편집하러 들어올 때 호출.
     * DiaryEntry 내용을 DiaryDraft로 변환해 _draft에 세팅.
     */
    fun loadExistingEntry(entry: DiaryEntry) {
        _existingEntryId.value = entry.id
        _draft.value = DiaryDraft(
            date = entry.date,
            aiContent = entry.content,
            editedContent = entry.content,
            photos = entry.photos
        )
        _selectedWeather.value = entry.weather.ifEmpty { null }
        _selectedEmotion.value = entry.emotion.ifEmpty { null }
    }

    /** DraftPreviewScreen 재진입 시 이전 초안만 날리고 블록은 유지 */
    fun clearDraftOnly() {
        _draft.value = null
        _contextQuestions.value = null
        _photoAnalysis.value = null
    }

    /**
     * 최근 일기 2개의 마지막 500자를 읽어 recentDiarySamples에 저장.
     * generateDraft() 호출 전 loadBlocks() 내부에서 실행되며,
     * Claude 프롬프트에 문체 참고 샘플로 전달됨.
     */
    fun loadRecentDiaryStyle(
        userId: String
    ) {

        viewModelScope.launch {

            diaryRepository
                .getDiaries(userId)
                .collect { diaries ->

                    recentDiarySamples =
                        diaries
                            .take(2)
                            .map {
                                it.content
                                    .takeLast(500)
                            }
                            .joinToString(
                                "\n\n---\n\n"
                            )

                }

        }

    }

    /** 일기 작성 완료 또는 취소 시 모든 상태 초기화 */
    fun resetDraft() {
        targetDate = null
        _draft.value = null
        _blocks.value = emptyList()
        _calendarEvents.value = emptyList()
        _upcomingEvents.value = emptyList()
        _contextQuestions.value = null
        _selectedWeather.value = null
        _selectedEmotion.value = null
        _existingEntryId.value = null
    }
}
