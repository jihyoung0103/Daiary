package com.smu.daiary.feature.write

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.location.Geocoder
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
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
import com.smu.daiary.data.source.FollowUpResult
import com.smu.daiary.data.source.GeneratedBlock
import kotlinx.coroutines.delay
import java.util.Collections
import java.util.UUID


private const val TAG = "WriteViewModel"

/** 감정 선택지. 답변 키로 그대로 쓰이는 canonical 값이라 번역하지 않는다(emotionList와 동일) */
private val EMOTION_OPTIONS = listOf("기쁨", "설렘", "평온", "슬픔", "화남", "기타")

/** 날씨 선택지. weatherIconMap(DiaryEditScreen/DraftPreviewScreen)과 동일한 canonical 값 */
private val WEATHER_OPTIONS = listOf("맑음", "흐림", "비", "뇌우", "눈")

/**
 * 질문을 만들지 않는 블록 타입.
 * 내일 일정·내일 날씨는 아직 일어나지 않은 일이라 답변에 담길 경험이 없다.
 * 프롬프트에도 같은 규칙이 있지만, 구조적으로 확실한 건 여기서 걸러 토큰도 아낀다.
 */
private val NO_QUESTION_TYPES = setOf(BlockType.CALENDAR_UPCOMING, BlockType.WEATHER_TOMORROW)

/**
 * 세션 전체에서 허용할 후속 질문 수.
 * 첫 질문은 상한이 없다(물을 가치가 있으면 다 묻고, 넘길지는 사용자가 정한다).
 * 대신 후속은 사용자가 이미 답한 뒤에 붙는 추가 타이핑이라 여기서 총량을 묶는다.
 * 대화를 언제 끝낼지는 모델과 답변 성의가 먼저 판단하고, 이 숫자는 안전망이다.
 */
private const val MAX_FOLLOW_UPS = 6

/** 한 소재를 두고 이어갈 수 있는 최대 후속 질문 수 */
private const val MAX_FOLLOW_UPS_PER_CARD = 3

/**
 * 넓은 소재를 먼저 묻는 순서. 여기 없는 소재는 원래 순서대로 뒤에 붙는다.
 *
 * 날씨와 걸음 수는 하루의 윤곽(밖에 있었는지, 집에 있었는지)을 잡아준다.
 * 그 답이 뒤 카드의 질문 갱신에 반영되어야 구체적인 질문이 나오므로 먼저 묻는다.
 */
private val CARD_ORDER_FIRST = listOf("weather", "health")

/** 마무리 한마디를 보여주고 다음 카드로 넘어가기까지의 시간 */
private const val CLOSING_DISPLAY_MS = 1600L

/** 마무리 멘트 없이 넘어갈 때의 최소 간격. 답하자마자 화면이 튀는 느낌을 막는다 */
private const val CARD_ADVANCE_DELAY_MS = 500L

/**
 * 후속 질문을 붙일 최소 답변 길이.
 * 짧게 끊었다는 건 그 주제에 할 말이 없다는 신호지만, 고유명사는 짧으면서 가장 값진 답이라
 * ("누나홀닭") 기준을 낮게 잡는다. 성의 없는 답은 DISMISSIVE_ANSWERS와 모델이 걸러낸다.
 */
private const val MIN_ANSWER_LENGTH_FOR_FOLLOW_UP = 3

/** 이 답변들은 길이와 무관하게 더 묻지 않는다 (공백 제거 후 비교) */
private val DISMISSIVE_ANSWERS =
    setOf("없음", "딱히없음", "특별히없음", "몰라", "모름", "몰라요", "그냥", "기억안남", "없어", "없어요")

/**
 * 일기 작성 흐름(블록 선택 → 질답 → 초안 → 편집 → 저장) 전체의 상태와 로직을 담당하는 ViewModel.
 *
 * 화면 이동 순서와 담당 함수:
 * ```
 * BlockSelection ──prepareGeneration()──▶ ContextQnA ──submitAnswers()──▶ DraftPreview ──▶ DiaryEdit
 *       ▲                                                                                     │
 *       └────────────────────────── loadBlocks() ────────────────────────── saveDraft() ──────┘
 * ```
 *
 * 주요 책임:
 * 1. **데이터 수집** — 날씨·일정·사진·건강을 병렬 수집하고 결제는 저장된 걸 읽어 ContentBlock 목록 구성
 * 2. **선택 상태 관리** — 블록/사진/일정/결제를 개별 토글. 자식 선택이 부모 블록 상태와 동기화된다
 * 3. **AI 초안 생성** — 사진은 장별 분석 후 소스 1:1로 전개, 블록+MBTI+문체 샘플+질답을 Claude에 전달
 * 4. **초안 편집 & 저장** — 블록 단위 수정/이동/삭제 후 DiaryEntry로 변환해 Firestore에 저장
 * 5. **기존 일기 편집** — 홈에서 넘어온 DiaryEntry를 draft로 복원
 *
 * 날짜 기준은 [DiaryDateUtil]을 따른다 — 오전 4시 이전이면 전날 일기로 취급한다.
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

    /** 일기 문서 CRUD (users/{uid}/diaries) */
    private val diaryRepository = DiaryRepository()

    /** 날짜별 수집 데이터 — 날씨·사진·건강·결제 (users/{uid}/dailyData/{date}) */
    private val dailyDataRepository = DailyDataRepository()

    /** Claude API — 질문 생성 / 사진 분석 / 본문 생성 */
    private val aiRepository = com.smu.daiary.data.repository.AiRepository()

    /** 내일 예보 조회. 오늘 날씨는 백그라운드 워커가 쌓아둔 걸 Firestore에서 읽는다 */
    private val weatherDataSource = WeatherDataSource(context)

    /** MediaStore에서 대상 날짜 사진 목록 + EXIF 메타데이터 조회 */
    private val photoDataSource = PhotoDataSource(context)

    /** 저장 시 선택된 사진을 Firebase Storage에 업로드 */
    private val photoStorageDataSource = com.smu.daiary.data.source.PhotoStorageDataSource(context)

    /** 기기 캘린더 일정 조회 */
    private val calendarDataSource = CalendarDataSource(context)

    /** Health Connect 걸음 수·수면 조회 */
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

    /** 질답에서 "기타"로 자유입력한 날씨 원문. 고정 5종 밖의 값이라 selectedWeather와 별도 보관 */
    private val _customWeatherText = MutableStateFlow<String?>(null)
    val customWeatherText: StateFlow<String?> = _customWeatherText.asStateFlow()

    /** 질답에서 "기타"로 자유입력한 감정 원문. 고정 5종 밖의 값이라 selectedEmotion과 별도 보관 */
    private val _customEmotionText = MutableStateFlow<String?>(null)
    val customEmotionText: StateFlow<String?> = _customEmotionText.asStateFlow()

    // ─────────────────────────────────────────────────────────────
    // UI State — AI 초안 생성
    // ─────────────────────────────────────────────────────────────

    /** 질문 생성(prepareGeneration) 진행 중 여부 */
    private val _isGeneratingQuestions = MutableStateFlow(false)
    val isGeneratingQuestions: StateFlow<Boolean> = _isGeneratingQuestions.asStateFlow()

    /** 질의응답 카드 목록. null = 아직 생성 전, emptyList = 질문 없음 */
    private val _qnaCards = MutableStateFlow<List<QnaCard>?>(null)
    val qnaCards: StateFlow<List<QnaCard>?> = _qnaCards.asStateFlow()

    /** 지금 보여줄 카드 인덱스 */
    private val _currentCardIndex = MutableStateFlow(0)
    val currentCardIndex: StateFlow<Int> = _currentCardIndex.asStateFlow()

    /** 후속 질문을 받아오는 중 — 카드 하단에 로딩 표시 */
    private val _isLoadingFollowUp = MutableStateFlow(false)
    val isLoadingFollowUp: StateFlow<Boolean> = _isLoadingFollowUp.asStateFlow()

    /** 다음 카드의 질문을 그동안의 답변에 맞춰 손보는 중 */
    private val _isPreparingCard = MutableStateFlow(false)
    val isPreparingCard: StateFlow<Boolean> = _isPreparingCard.asStateFlow()

    /** 이번 세션에서 이미 붙인 후속 질문 수 */
    private var followUpCount = 0

    /** Claude API 호출 진행 중 여부 */
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    /** AI 초안 생성 실패 시 메시지. UI에서 스낵바로 표시 */
    private val _generateError = MutableStateFlow<String?>(null)
    val generateError: StateFlow<String?> = _generateError.asStateFlow()

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

    /**
     * 지금 작성 중인 일기의 날짜. [targetDate]가 없으면 오늘(4시 규칙 적용) 기준.
     *
     * 화면에 날짜를 띄우라고 만들어 뒀지만 **현재 이 StateFlow를 구독하는 화면이 없다.**
     * 작성 화면에 날짜 표시를 붙일 때 쓰거나, 안 쓸 거면 함께 지운다.
     */
    private val _writingDate = MutableStateFlow<LocalDate>(DiaryDateUtil.diaryDate())
    val writingDate: StateFlow<LocalDate> = _writingDate.asStateFlow()

    /**
     * 작성 대상 날짜를 지정한다. 화면 진입 직전에 호출해야 [loadBlocks]가 올바른 날짜로 수집한다.
     *
     * @param date 캘린더에서 고른 날짜. **null이면 오늘 기준**(FAB 진입 경로).
     *             오늘을 명시적으로 넘겨도 동작은 같다 — [loadBlocks]가 null 여부가 아니라
     *             실제 날짜가 오늘인지로 판단하기 때문이다.
     */
    fun setTargetDate(date: LocalDate?) {
        targetDate = date
        _writingDate.value = date ?: DiaryDateUtil.diaryDate()
        // 신규 작성 진입점은 여기 하나뿐이므로, 직전 편집 세션이 저장 없이 끝나 남아 있던
        // 편집 대상 ID를 여기서 끊는다. 남겨두면 saveDraft가 그 문서를 updateDiary로
        // 덮어써서 엉뚱한 날짜의 일기가 통째로 사라진다. (resetDraft는 저장 성공 시에만 돈다)
        _existingEntryId.value = null
    }

    /** AI 프롬프트에 문체 참고용으로 넘길 최근 일기 샘플 (최대 2개) */
    private var recentDiarySamples: String = ""

    // ─────────────────────────────────────────────────────────────
    // 데이터 수집 — 대상 날짜의 블록 목록 구성
    // ─────────────────────────────────────────────────────────────

    /**
     * 대상 날짜의 데이터를 수집해 [blocks]를 채우고 DailyDataRepository에 저장한다.
     *
     * 날씨·일정·사진·건강을 병렬로 모으고, 한 항목이 실패해도 나머지는 그대로 진행한다
     * (권한 거부가 흔해서 부분 수집을 정상 경로로 취급한다).
     * 결제는 NotificationListenerService가 미리 저장해 둔 것을 읽기만 한다.
     *
     * 수집 범위는 대상 날짜가 **오늘인지**에 따라 갈린다 — 오늘이면 내일 예보와
     * 3일치 일정까지 가져오고, 과거 날짜면 그 날 것만 본다.
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
            val diaryDate = targetDate ?: DiaryDateUtil.diaryDate()
            val date = diaryDate.toString()
            // targetDate의 null 여부가 아니라 "실제 대상 날짜가 오늘인지"로 판단한다.
            // 배너로 오늘을 선택해도 targetDate는 non-null이므로, null 검사로는
            // FAB 경로와 배너 경로의 블록 구성이 달라진다.
            val isToday = diaryDate == DiaryDateUtil.diaryDate()
            val blocks = mutableListOf<ContentBlock>()


            // --- 날씨, 캘린더, 사진, 건강 병렬 수집 ---
            // 오늘 날씨는 백그라운드 워커(WeatherCollectionWorker)가 하루 1회(14시경) 수집해 Firestore에 append 해둠.
            // 여기서는 내일 예보만 API로 조회. 과거 날짜 편집 시엔 날씨 API 호출 없음.
            val weatherDeferred = async {
                runCatching {
                    if (isToday) weatherDataSource.fetchTomorrow() else null
                }
            }                             
            // 오늘이면 3일치(오늘/내일/모레) 조회, 과거 날짜면 해당 날짜 일정만 조회
            val calendarDeferred = async {
                runCatching {
                    if (isToday) calendarDataSource.fetchUpcomingEvents()
                    else calendarDataSource.fetchEventsForDate(diaryDate)
                }
            }
            // 사진·건강도 날씨/캘린더와 같이 대상 날짜 기준으로 수집한다.
            // 오늘 것만 가져오면 과거 날짜 일기에 오늘 사진과 오늘 걸음 수가 붙는다.
            val photoDeferred = async { runCatching { photoDataSource.fetchPhotos(diaryDate) } }
            val healthDeferred = async { runCatching { healthDataSource.fetchHealth(diaryDate) } }

            // 날씨 오늘 — 워커가 쌓아둔 snapshots 우선, 비어 있으면 즉석 수집으로 보완
            val existingWeather = dailyDataRepository.getDailyData(userId, date).getOrNull()?.weather
            val snapshots = existingWeather?.snapshots ?: emptyList()

            // 워커는 하루 1회(14시)라 오전에 쓰면 스냅샷이 비어 있다. 그때는 기다리지 말고
            // 지금 한 번 직접 받아온다. 받아온 값은 Firestore에 쌓아, 같은 날 다시 들어와도
            // API를 또 부르지 않게 한다. 과거 날짜는 지금 날씨가 그날 날씨가 아니므로 제외.
            // (fetchJson에 타임아웃이 있어 최악 15초 안에 실패로 빠진다)
            val onDemand = if (snapshots.isEmpty() && isToday) {
                runCatching { weatherDataSource.fetchTodaySnapshot() }
                    .onFailure { Log.w(TAG, "⚠️ 즉석 날씨 수집 실패 — 사용자가 칩에서 직접 고르게 둔다", it) }
                    .getOrNull()
                    ?.also { dailyDataRepository.appendWeatherSnapshot(userId, date, it) }
            } else null

            val latest = snapshots.lastOrNull() ?: onDemand
            if (latest != null) {
                Log.d(TAG, "🌤️ 오늘 날씨: ${latest.description} ${latest.temperature}°C (${if (onDemand != null) "즉석 수집" else "스냅샷 ${snapshots.size}개"})")
                // 수집된 오늘 날씨를 일기 날씨로 쓴다 (canonical 값이 선택지와 동일).
                // 수집도 실패하면 비워두고, 사용자가 날짜 옆 칩에서 직접 고른다.
                _selectedWeather.value = latest.description
                blocks.add(ContentBlock(
                    id = "weather", type = BlockType.WEATHER,
                    content = localizedContext().getString(
                        R.string.block_weather_content,
                        latest.city,
                        localizedWeatherDescription(latest.description),
                        latest.temperature.toInt(),
                        latest.humidity
                    ),
                    isSelected = true
                ))

            } else {
                Log.w(TAG, "⚠️ 날씨 없음 (과거 날짜이거나, 스냅샷도 즉석 수집도 실패)")
                // 캘린더 빈 상태(block_calendar_empty)와 동일한 컨벤션: 블록은 보여주되 선택 자체를 막아
                // 초안 생성 시 "날씨는 날씨 정보를 가져올 수 없습니다이었다" 같은 어색한 문장이 포함될 수 없도록 함.
                blocks.add(ContentBlock(id = "weather", type = BlockType.WEATHER, content = localizedContext().getString(R.string.block_weather_unavailable), isSelected = false, isFallback = true))
            }

            // 날씨 내일 — 사용자 작성 시점에 예보 API 호출한 결과 사용
            weatherDeferred.await()
                .onSuccess { tomorrow ->
                    if (tomorrow != null && tomorrow.tomorrowDescription.isNotBlank()) {
                        Log.d(TAG, "🌤️ 내일 날씨: ${tomorrow.tomorrowDescription} ${tomorrow.tomorrowTemperature}°C")
                        // 오늘 스냅샷과 함께 Firestore weather에 병합 저장 (tomorrow 필드만 갱신)
                        val merged = (existingWeather ?: com.smu.daiary.data.model.WeatherData()).copy(
                            tomorrowDescription = tomorrow.tomorrowDescription,
                            tomorrowTemperature = tomorrow.tomorrowTemperature,
                            tomorrowHumidity = tomorrow.tomorrowHumidity
                        )
                        dailyDataRepository.updateWeather(userId, date, merged)
                        blocks.add(ContentBlock(
                            id = "weather_tomorrow", type = BlockType.WEATHER_TOMORROW,
                            content = localizedContext().getString(
                                R.string.block_weather_tomorrow_content,
                                localizedWeatherDescription(tomorrow.tomorrowDescription),
                                tomorrow.tomorrowTemperature.toInt(),
                                tomorrow.tomorrowHumidity
                            ),
                            isSelected = true
                        ))
                    }
                }
                .onFailure { Log.w(TAG, "⚠️ 내일 날씨 조회 실패", it) }

            // 캘린더
            calendarDeferred.await()
                .onSuccess { events ->
                    Log.d(TAG, "📅 캘린더 수집 완료: ${events.size}개")
                    dailyDataRepository.updateCalendar(userId, date, events)
                    if (events.isEmpty()) {
                        blocks.add(ContentBlock(
                            id = "calendar_summary", type = BlockType.CALENDAR,
                            content = localizedContext().getString(R.string.block_calendar_empty),
                            isSelected = false, isFallback = true
                        ))
                    } else {
                        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
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

                        // 오늘: 날짜별 분리 / 과거 날짜: 전체가 해당 날짜 일정
                        val todayEvents = if (isToday)
                            events.filter { Instant.ofEpochMilli(it.startTime).atZone(zone).toLocalDate() == diaryDate }
                            else events
                        val futureEvents = if (isToday)
                            events.filter { Instant.ofEpochMilli(it.startTime).atZone(zone).toLocalDate() > diaryDate }
                            else emptyList()

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

                    // 사진은 기본 해제. 자동 수집된 사진에는 스크린샷·수신 이미지가 섞여 있어
                    // 전부 켜두면 사용자가 지우는 쪽으로 일을 하게 된다. 쓸 사진만 고르게 한다.
                    _photos.value = photos.map { photo ->
                        PhotoSelectableItem(
                            uri = photo.uri,
                            isSelected = false,
                            takenAt = photo.takenAt,
                            latitude = photo.latitude,
                            longitude = photo.longitude,
                            isCameraPhoto = photo.isCameraPhoto
                        )
                    }

                        blocks.add(
                            ContentBlock(
                                id = "photo",
                                type = BlockType.PHOTO,
                                content = localizedContext().getString(R.string.block_photo_selection_content, photos.size, 0),
                                isSelected = false
                                )
                            )

                    // 촬영 장소 — GPS 있는 첫 번째 사진 기준, 지오코딩 실패 시 블록 미생성.
                    // 사진이 기본 해제라 선택 여부는 보지 않는다(보면 블록이 영영 안 생김).
                    _photos.value
                        .firstOrNull { it.latitude != 0.0 || it.longitude != 0.0 }
                        ?.let { photo ->
                            reverseGeocode(photo.latitude, photo.longitude)?.let { placeName ->
                                blocks.add(ContentBlock(
                                    id = "photo_location", type = BlockType.PHOTO_LOCATION,
                                    content = placeName
                                ))
                            }
                        }
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
                        blocks.add(ContentBlock(id = "health", type = BlockType.HEALTH, content = content, isSelected = true))
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
    // 내부 유틸 — 날씨 표기 / 위치 / 이미지 인코딩
    // ─────────────────────────────────────────────────────────────

    /** 한국어 canonical 날씨명을 현재 언어 설정에 맞는 문자열로 변환 */
    private fun localizedWeatherDescription(canonical: String): String = when (canonical) {
        "맑음" -> localizedContext().getString(R.string.weather_sunny)
        "흐림" -> localizedContext().getString(R.string.weather_cloudy)
        "비"   -> localizedContext().getString(R.string.weather_rain)
        "눈"   -> localizedContext().getString(R.string.weather_snow)
        "뇌우" -> localizedContext().getString(R.string.weather_thunderstorm)
        else   -> canonical
    }

    /** 좌표 → 장소 텍스트(구/동, 시). Geocoder 미지원 기기이거나 결과가 없으면 null 반환 */
    @Suppress("DEPRECATION")
    private suspend fun reverseGeocode(latitude: Double, longitude: Double): String? =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) return@withContext null
            runCatching {
                val geocoder = Geocoder(getApplication(), Locale.getDefault())
                val address = geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                    ?: return@runCatching null
                val district = address.subLocality ?: address.locality
                val city = address.locality ?: address.adminArea
                when {
                    district != null && city != null && district != city -> "$district, $city"
                    district != null -> district
                    else -> city
                }
            }.onFailure { Log.w(TAG, "⚠️ 촬영 장소 지오코딩 실패", it) }.getOrNull()
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

    /**
     * Claude Vision에 넘길 media_type을 결정한다.
     * ContentResolver가 알려준 MIME을 우선 쓰되, 없거나 지원하지 않는 형식이면
     * 바이트 앞부분의 매직 넘버로 직접 판별한다(갤러리 앱이 MIME을 비워 보내는 경우 대비).
     */
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
            list.map {
                if (it.id == id && !it.isFallback) it.copy(isSelected = !it.isSelected) else it
            }
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
        // 부모 사진 블록의 선택 상태·"선택 N장" 문구를 함께 갱신한다.
        // (추가·삭제 경로는 이미 호출하고 있었는데 토글만 빠져 있었다)
        syncPhotoBlockSelection()
    }

    /** 블록 선택 화면에서 사진을 목록에서 제거 */
    fun removeSelectablePhoto(uri: String) {
        _photos.update { list -> list.filter { it.uri != uri } }
        syncPhotoBlockSelection()
    }

    /** 갤러리에서 직접 고른 사진을 목록에 추가 (중복 방지 + EXIF 메타데이터 읽기) */
    fun addSelectablePhoto(uri: String) {
        if (_photos.value.any { it.uri == uri }) return

        viewModelScope.launch {
            // EXIF에서 촬영 시각·위치를 읽어 자동 수집 사진과 동일한 데이터로 채움
            val meta = photoDataSource.readPhotoMeta(uri)

            // 비동기 사이에 중복 추가됐을 수 있으니 재확인
            if (_photos.value.any { it.uri == uri }) return@launch

            _photos.update { list ->
                list + PhotoSelectableItem(
                    uri = uri,
                    isSelected = true,
                    takenAt = meta.takenAt,
                    latitude = meta.latitude,
                    longitude = meta.longitude,
                    isCameraPhoto = meta.isCameraPhoto
                )
            }

            syncPhotoBlockSelection()
        }
    }

    /** 향후 일정 개별 선택 토글 → 완료 후 향후 일정 블록 요약 텍스트 자동 갱신 */
    fun toggleUpcomingEvent(id: Int) {
        _upcomingEvents.update { list ->
            list.map { if (it.id == id) it.copy(isSelected = !it.isSelected) else it }
        }
        syncUpcomingBlockSelection()
    }

    /**
     * 개별 향후 일정 선택 상태를 향후 일정 블록에 반영한다.
     * 하나도 안 골랐으면 블록 자체를 해제해 초안 소스에서 빠지게 한다.
     */
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

    /** 향후 일정 블록에 보여줄 요약 텍스트. 선택된 일정이 없으면 안내 문구를 돌려준다. */
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
     * 선택된 사진을 장별로 병렬 분석하고 결과를 _photos에 캐싱한다.
     * 이미 분석된 사진은 재사용하므로 여러 번 호출해도 Vision 호출은 사진당 1회다.
     */
    private suspend fun analyzeSelectedPhotos(): List<PhotoSelectableItem> {
        val selectedPhotos = _photos.value.filter { it.isSelected }
        Log.d(TAG, "📸 선택된 사진 수: ${selectedPhotos.size}")

        val analyzed = coroutineScope {
            selectedPhotos.map { photo ->
                async {
                    if (!photo.analysis.isNullOrBlank()) return@async photo
                    val encoded = encodeImage(photo.uri) ?: return@async photo
                    val result = try {
                        aiRepository.analyzePhoto(encoded, photo.isCameraPhoto)
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ 사진 분석 실패: ${photo.uri}", e)
                        ""
                    }
                    photo.copy(analysis = result.ifBlank { null })
                }
            }.awaitAll()
        }

        _photos.update { list ->
            list.map { p -> analyzed.firstOrNull { it.uri == p.uri } ?: p }
        }
        return analyzed
    }

    /**
     * 분석된 사진을 촬영 시각 순으로 정렬해 장별 소스(photo_1..N)로 전개.
     * 사진 1장 = 소스 1개 = 본문 블록 1개가 되도록 여기서 경계를 만든다.
     * 질문 생성과 초안 생성이 같은 sourceId를 쓰도록 양쪽에서 이 함수를 공유한다.
     */
    private fun photoSourcesOf(photos: List<PhotoSelectableItem>): List<DiarySource> {
        val photoTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        return photos
            .filter { !it.analysis.isNullOrBlank() }
            .sortedBy { if (it.takenAt > 0L) it.takenAt else Long.MAX_VALUE }
            .mapIndexed { index, photo ->
                val kindLabel = if (photo.isCameraPhoto) "촬영 사진" else "화면 캡처/수신 이미지"
                // 촬영 시각은 직접 촬영한 사진에서만 의미가 있으므로 그 경우에만 표시
                val timeLabel = if (photo.isCameraPhoto && photo.takenAt > 0L) {
                    val t = Instant.ofEpochMilli(photo.takenAt)
                        .atZone(ZoneId.systemDefault())
                        .format(photoTimeFormatter)
                    ", 촬영 $t"
                } else ""
                DiarySource(
                    sourceId = "photo_${index + 1}",
                    type = BlockType.PHOTO,
                    content = "[$kindLabel$timeLabel]\n${photo.analysis}",
                    imageUri = photo.uri
                )
            }
    }

    /**
     * 초안 생성 1단계 — 선택된 블록을 분석해 맥락 질문을 만든다.
     * 완료되면 [contextQuestions]에 결과가 세팅되고 UI가 ContextQnAScreen으로 이동한다.
     *
     * 선택된 블록이 없으면 빈 리스트를 세팅해 질답 단계를 건너뛴다.
     * 질문 생성이 실패해도 감정 질문 하나는 남겨, 화면이 빈 채로 뜨지 않게 한다.
     */
    fun prepareGeneration() = viewModelScope.launch {
        val selected = _blocks.value.filter { it.isSelected }
        if (selected.isEmpty()) {
            _qnaCards.value = emptyList()
            return@launch
        }
        _isGeneratingQuestions.value = true
        _qnaCards.value = null
        _currentCardIndex.value = 0
        followUpCount = 0
        try {
            // 질문 생성 전에 사진을 먼저 분석한다. 사진 블록의 content는 "N장 · 선택 M장"뿐이라
            // 분석 없이 질문을 만들면 사진 속 내용을 모른 채 뻔한 질문만 나온다.
            val photoSources = photoSourcesOf(analyzeSelectedPhotos())

            // 사진 블록은 장별 분석 결과로 대체한다. blockId를 photo_1..N으로 두면
            // 사진마다 질문·답변이 따로 잡힌다(답변 맵이 blockId 키라 중복되면 덮어써짐).
            val questionInput = photoSources.map {
                ContentBlock(id = it.sourceId, type = it.type, content = it.content)
            } + selected.filter { it.type != BlockType.PHOTO && it.type !in NO_QUESTION_TYPES }

            // AI가 만든 질문은 전부 자유 입력으로 받는다. 선택지를 주면 답이 평이해지고
            // 질문과 무관한 선택지가 섞여서 초안의 재료로 쓸 만한 답이 안 나온다.
            // 선택지가 남는 건 감정 질문뿐 — 답변이 일기의 emotion 필드로 저장되고 회고 집계에 쓰인다.
            //
            // distinctBy: 한 블록에 질문이 2개 이상 나오면 답변 맵(blockId 키)에서 서로 덮어써
            // 답변이 조용히 사라진다. 프롬프트가 뭘 뱉든 여기서 막는다.
            val questions = aiRepository.generateContextQuestions(questionInput)
                .distinctBy { it.blockId }
                .map { it.copy(quickOptions = emptyList()) }

            Log.d(TAG, "===== 생성된 질문 ${questions.size}개 (블럭 ${questionInput.size}개) =====")
            questions.forEach { Log.d(TAG, "- [${it.blockId}] ${it.question}") }

            // 카드 = 소재 1개. 첫 질문이 1턴이 되고, 답변에 따라 후속 턴이 아래로 쌓인다.
            val sourceById = (photoSources + selected
                .filter { it.type != BlockType.PHOTO }
                .map { DiarySource(sourceId = it.id, type = it.type, content = it.content) })
                .associateBy { it.sourceId }

            val cards = questions.map { q ->
                QnaCard(
                    sourceId = q.blockId,
                    sourceContent = sourceById[q.blockId]?.content.orEmpty(),
                    sourceLabel = sourceById[q.blockId]?.type?.label.orEmpty(),
                    imageUri = sourceById[q.blockId]?.imageUri,
                    turns = listOf(QnaTurn(question = q.question))
                )
            }
            // 넓은 소재(날씨·걸음 수)를 앞으로. sortedBy는 안정 정렬이라 나머지는 원래 순서를 지킨다.
            val ordered = cards.sortedBy { card ->
                CARD_ORDER_FIRST.indexOf(card.sourceId).takeIf { it >= 0 } ?: CARD_ORDER_FIRST.size
            }
            _qnaCards.value = ordered + emotionCard()
        } catch (e: Exception) {
            Log.e(TAG, "❌ 질문 생성 실패 — 감정 질문만 남김", e)
            _qnaCards.value = listOf(emotionCard())
        } finally {
            _isGeneratingQuestions.value = false
        }
    }

    /** 항상 마지막에 붙는 감정 카드. 선택지가 있어 자유 입력 카드와 다르게 렌더된다. */
    private fun emotionCard() = QnaCard(
        sourceId = "emotion",
        sourceContent = "",
        turns = listOf(QnaTurn(question = localizedContext().getString(R.string.question_emotion))),
        options = EMOTION_OPTIONS
    )

    /**
     * 현재 카드의 답하지 않은 턴에 답변을 기록한다.
     * 답변이 충실하면 후속 질문을 받아 같은 카드에 이어 붙이고(수직),
     * 그렇지 않으면 다음 카드로 넘어간다(수평).
     */
    fun answerCurrentTurn(answer: String) = viewModelScope.launch {
        val cards = _qnaCards.value ?: return@launch
        val cardIndex = _currentCardIndex.value
        val card = cards.getOrNull(cardIndex) ?: return@launch
        val turnIndex = card.pendingTurnIndex ?: return@launch

        val answered = card.copy(
            turns = card.turns.toMutableList().also {
                it[turnIndex] = it[turnIndex].copy(answer = answer)
            }
        )
        _qnaCards.value = cards.toMutableList().also { it[cardIndex] = answered }

        // 감정 카드는 선택지 하나로 끝나면 일기 본문에 쓸 재료가 없다(칩만 붙고 글에는 안 남는다).
        // 왜 그런 기분이었는지 한 번 더 묻는다. 고정 질문이라 API 호출도 후속 예산도 쓰지 않는다.
        if (answered.sourceId == "emotion" && answered.turns.size == 1) {
            _qnaCards.value = _qnaCards.value?.toMutableList()?.also { list ->
                list[cardIndex] = answered.copy(
                    turns = answered.turns + QnaTurn(question = "왜 그런 기분이 드셨어요?")
                )
            }
            return@launch
        }

        if (!shouldAskFollowUp(answered, answer)) {
            finishCard(cardIndex, closing = "")
            return@launch
        }

        _isLoadingFollowUp.value = true
        val result = try {
            aiRepository.generateFollowUpQuestion(
                sourceContent = answered.sourceContent,
                turns = answered.turns
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ 후속 질문 실패 — 다음 카드로", e)
            FollowUpResult()
        } finally {
            _isLoadingFollowUp.value = false
        }

        if (result.question.isBlank()) {
            Log.d(TAG, "↪️ [${answered.sourceId}] 대화 종료 — ${result.closing.ifBlank { "(마무리 멘트 없음)" }}")
            finishCard(cardIndex, result.closing)
            return@launch
        }

        followUpCount++
        Log.d(TAG, "↳ [${answered.sourceId}] 후속 질문($followUpCount/$MAX_FOLLOW_UPS): ${result.question}")
        _qnaCards.value = _qnaCards.value?.toMutableList()?.also { list ->
            list[cardIndex] = answered.copy(turns = answered.turns + QnaTurn(question = result.question))
        }
    }

    /**
     * 이 소재의 대화를 마친다. 마무리 한마디를 잠깐 보여준 뒤 다음 카드로 넘어간다.
     * 답하자마자 화면이 넘어가면 마지막 답변이 무시된 느낌이 들어서 간격을 둔다.
     */
    private suspend fun finishCard(cardIndex: Int, closing: String) {
        if (closing.isNotBlank()) {
            _qnaCards.value = _qnaCards.value?.toMutableList()?.also { list ->
                list[cardIndex] = list[cardIndex].copy(closing = closing)
            }
            delay(CLOSING_DISPLAY_MS)
        } else {
            delay(CARD_ADVANCE_DELAY_MS)
        }
        advanceCard()
    }

    /** 현재 카드를 건너뛰고 다음 카드로. 후속 질문도 마무리 멘트도 없다. */
    fun skipCurrentCard() = advanceCard()

    private fun shouldAskFollowUp(card: QnaCard, answer: String): Boolean {
        if (card.options.isNotEmpty()) return false                     // 감정 등 고정 선택지 카드
        if (followUpCount >= MAX_FOLLOW_UPS) return false
        // turns에는 첫 질문이 포함되므로 후속 횟수는 turns.size - 1
        if (card.turns.size - 1 >= MAX_FOLLOW_UPS_PER_CARD) return false
        val normalized = answer.replace(" ", "")
        if (normalized in DISMISSIVE_ANSWERS) return false
        return answer.length >= MIN_ANSWER_LENGTH_FOR_FOLLOW_UP
    }

    private fun advanceCard() {
        val cards = _qnaCards.value ?: return
        val next = _currentCardIndex.value + 1
        if (next >= cards.size) {
            submitQna()
            return
        }
        _currentCardIndex.value = next
        viewModelScope.launch { reviseCurrentQuestion() }
    }

    /**
     * 카드에 도착한 시점에 그 카드의 질문을 지금까지 나온 답변에 맞춰 손본다.
     *
     * 첫 질문들은 대화가 시작되기 전에 한꺼번에 만들어지므로 중간 답변이 반영돼 있지 않다.
     * 그래서 이미 답한 걸 또 묻거나, 답변 덕에 가능해진 더 좋은 질문을 놓친다.
     * 더 물을 게 없어졌다고 판단되면 그 카드는 건너뛴다.
     */
    private suspend fun reviseCurrentQuestion() {
        val cards = _qnaCards.value ?: return
        val index = _currentCardIndex.value
        val card = cards.getOrNull(index) ?: return

        if (card.options.isNotEmpty()) return                       // 감정 카드는 고정 질문
        if (card.turns.size != 1 || card.turns[0].answer != null) return

        // 소재별로 묶어서 넘긴다. 평평하게 나열하면 한 소재에서 이어진 대화인지
        // 서로 다른 소재의 답변인지 구분이 사라져, 엉뚱한 소재의 답을 근거로 질문을 고친다.
        val prior = cards.take(index)
            .mapNotNull { done ->
                val exchange = done.turns
                    .filter { it.answer != null }
                    .joinToString("\n") { "Q: ${it.question}\nA: ${it.answer}" }
                    .ifBlank { return@mapNotNull null }
                "[${done.sourceLabel.ifBlank { "기타" }} · ${done.sourceId}]\n$exchange"
            }
        if (prior.isEmpty()) return                                 // 첫 카드면 반영할 답변이 없다

        _isPreparingCard.value = true
        val revised = try {
            aiRepository.reviseQuestion(
                sourceId = card.sourceId,
                sourceLabel = card.sourceLabel,
                sourceContent = card.sourceContent,
                question = card.turns[0].question,
                priorAnswers = prior.joinToString("\n\n")
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ 질문 갱신 실패 — 원래 질문 유지", e)
            card.turns[0].question
        } finally {
            _isPreparingCard.value = false
        }

        when {
            revised.isBlank() -> {
                Log.d(TAG, "⤫ [${card.sourceId}] 앞선 답변으로 이미 해소됨 — 카드 건너뜀")
                advanceCard()
            }
            revised != card.turns[0].question -> {
                Log.d(TAG, "✎ [${card.sourceId}] 질문 갱신: $revised")
                _qnaCards.value = cards.toMutableList().also {
                    it[index] = card.copy(turns = listOf(QnaTurn(question = revised)))
                }
            }
        }
    }

    /** 모든 카드의 답변을 모아 초안 생성을 시작한다. */
    private fun submitQna() {
        val cards = _qnaCards.value.orEmpty()

        // 감정 답변은 일기의 emotion 필드로 저장되고 회고 집계에 쓰이므로 선택지 값만 인정한다.
        val emotionAnswer = cards.firstOrNull { it.sourceId == "emotion" }
            ?.turns?.firstOrNull()?.answer
        emotionAnswer?.takeIf { it != "기타" && it in EMOTION_OPTIONS }
            ?.let { _selectedEmotion.value = it }
        // "기타" 자유입력 원문은 칩 선택과 별개로 미리보기·편집 화면에 보조 텍스트로만 노출한다.
        _customEmotionText.value = emotionAnswer?.takeIf { it !in EMOTION_OPTIONS }

        // 날씨 질답은 항상 자유 입력이라, 고정 5종 밖의 값이면 커스텀 텍스트로 별도 보관한다.
        val weatherAnswer = cards.firstOrNull { it.sourceId == "weather" }
            ?.turns?.firstOrNull()?.answer
        _customWeatherText.value = weatherAnswer?.takeIf { it !in WEATHER_OPTIONS }

        val qaAnswers = cards.flatMap { card ->
            card.turns.mapNotNull { turn ->
                val answer = turn.answer?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                QaAnswer(sourceId = card.sourceId, question = turn.question, answer = answer)
            }
        }
        Log.d(TAG, "📝 질의응답 완료 — 답변 ${qaAnswers.size}건 (후속 ${followUpCount}건)")
        generateDraft(qaAnswers = qaAnswers)
    }

    /**
     * 초안 생성 2단계 — 선택된 블록과 질답을 근거로 Claude에 본문 생성을 요청한다.
     * 현재는 [submitQna]에서만 호출된다(화면이 직접 부르지 않음).
     *
     * 처리 순서:
     * 1. 선택된 사진을 **장별로 병렬 분석** — [analyzeSelectedPhotos]가 결과를 캐싱해 재호출을 막는다
     * 2. 사진 1장 = 소스 1개(photo_1..N)로 전개하고 촬영 시각 순으로 정렬, 나머지 블록은 1:1 소스
     * 3. 소스 목록 + MBTI + 최근 문체 샘플 + 질답을 Claude에 전달해 sourceId별 본문 블록을 받는다
     * 4. 호출·파싱 실패 시 [fallbackBlocks]로 소스 내용을 그대로 문단화해 블록 구조는 유지한다
     * 5. [draft]에 세팅 → UI가 DraftPreviewScreen으로 전환
     *
     * @param qaAnswers 질의응답 카드에서 모은 답변. 비어 있으면 질답 없이 생성한다.
     */
    fun generateDraft(qaAnswers: List<QaAnswer> = emptyList()) = viewModelScope.launch {
        val selected = _blocks.value.filter { it.isSelected }
        val today = (targetDate ?: DiaryDateUtil.diaryDate()).toString()

        if (selected.isEmpty()) {
            _draft.value = DiaryDraft(
                date = today,
                aiContent = "오늘 하루를 기록해보세요.",
                editedContent = "오늘 하루를 기록해보세요.",
                // 빈 블록을 하나 깔아둬야 편집 화면에서 직접 쓸 수 있다
                blocks = listOf(DiaryBodyBlock(id = UUID.randomUUID().toString(), text = "")),
                photos = emptyList()
            )
            return@launch
        }

        val generation = viewModelScope.launch {
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

            // prepareGeneration에서 이미 분석했으면 캐시 히트라 추가 호출이 없다.
            // 질문 단계를 건너뛴 경로로 들어왔을 때만 여기서 실제 분석이 돈다.
            val photoSources = photoSourcesOf(analyzeSelectedPhotos())

            // 사진 외 블록은 1:1로 소스가 된다. PHOTO 블록("N장 · 선택 M장")은
            // 장별 소스로 대체되었으므로 제외한다.
            val otherSources = selected
                .filter { it.type != BlockType.PHOTO }
                .map { block ->
                    DiarySource(
                        sourceId = block.id,
                        type = block.type,
                        content = block.content
                    )
                }

            // 사진(구체적 장면)을 앞에 두고 나머지 데이터가 뒤따르게 한다.
            val sources = photoSources + otherSources

            // 소스 구성만 남기고 content는 찍지 않는다. content에는 결제 가맹점·금액,
            // 일정 제목, 사진 분석 결과가 그대로 들어있고 릴리스 빌드도 minify가 꺼져 있어
            // Log.d가 전부 살아서 실행된다.
            Log.d(TAG, "📝 소스 ${sources.size}개: ${sources.joinToString { "${it.sourceId}[${it.type.label}]" }}")

            val mbti = getApplication<Application>()
                .getSharedPreferences(
                    "user_settings",
                    android.content.Context.MODE_PRIVATE
                )
                .getString("mbti", "INFP") ?: "INFP"

            val result =
                aiRepository.generateDiaryBlocks(
                    sources = sources,
                    locale = locale,
                    mbti = mbti,
                    recentDiarySamples = recentDiarySamples,
                    qaAnswers = qaAnswers
                )

            result.exceptionOrNull()?.let { Log.e(TAG, "❌ 블록 생성 실패", it) }

            // AI 호출이 실패했거나, 응답은 왔지만 쓸 만한 블록이 하나도 없으면 폴백
            val generated = result.getOrNull().orEmpty()
            val usedFallback = generated.isEmpty()
            if (usedFallback) {
                _generateError.value = if (locale == "en")
                    "AI generation failed. Using default template."
                else
                    "초안 생성에 실패했습니다. 기본 템플릿으로 대체합니다."
            }

            // sourceId로 소스와 다시 이어붙여 사진 URI·출처 타입을 블록에 부착
            val sourceById = sources.associateBy { it.sourceId }
            val bodyBlocks = (if (usedFallback) fallbackBlocks(sources) else generated)
                .mapNotNull { gen ->
                    val source = sourceById[gen.sourceId] ?: return@mapNotNull null
                    DiaryBodyBlock(
                        id = UUID.randomUUID().toString(),
                        sourceId = source.sourceId,
                        text = gen.text,
                        imageUri = source.imageUri
                    )
                }

            Log.d(TAG, "✅ 본문 블록 ${bodyBlocks.size}개 생성 (소스 ${sources.size}개)")

            val selectedPhotoUris =
                _photos.value
                    .filter { it.isSelected }
                    .map { it.uri }

            _draft.value = DiaryDraft(
                date = today,
                // 블록 렌더링/편집 전까지 기존 화면이 그대로 동작하도록 평문도 함께 유지
                aiContent = bodyBlocks.joinToString("\n\n") { it.text },
                blocks = bodyBlocks,
                photos = selectedPhotoUris
            )
        }

        // 성공·예외·취소 어느 경우든 로딩 플래그를 반드시 내린다.
        // 여기서 true로 남으면 QnA 화면이 로딩만 띄우고 질문을 안 보여줘(ContextQnAScreen),
        // 답변을 못 하니 플래그를 내려줄 코드가 영영 실행되지 않는다. 앱 재시작 외엔 복구 불가.
        generation.invokeOnCompletion { _isGenerating.value = false }
    }




    /**
     * AI 생성/파싱 실패 시 소스 내용을 그대로 문단화한 폴백 블록.
     * 블록 구조는 유지되므로 편집·재배치는 정상 동작한다.
     */
    private fun fallbackBlocks(sources: List<DiarySource>): List<GeneratedBlock> =
        sources.map { source ->
            val text = when (source.type) {
                BlockType.PAYMENT  -> localizedContext().getString(R.string.draft_block_payment, source.content)
                BlockType.PHOTO    -> localizedContext().getString(R.string.draft_block_photo, source.content)
                BlockType.CALENDAR          -> localizedContext().getString(R.string.draft_block_calendar, source.content)
                BlockType.CALENDAR_UPCOMING -> localizedContext().getString(R.string.draft_block_calendar, source.content)
                BlockType.HEALTH            -> localizedContext().getString(R.string.draft_block_health, source.content)
                BlockType.WEATHER  -> localizedContext().getString(R.string.draft_block_weather, source.content)
                BlockType.WEATHER_TOMORROW -> localizedContext().getString(R.string.draft_block_weather_tomorrow, source.content)
                BlockType.PHOTO_LOCATION   -> localizedContext().getString(R.string.draft_block_photo_location, source.content)
            }
            GeneratedBlock(sourceId = source.sourceId, text = text)
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

    // ─────────────────────────────────────────────────────────────
    // 본문 블록 편집 — 수정 / 순서 이동 / 삭제
    // ─────────────────────────────────────────────────────────────

    /** 본문 블록 하나의 텍스트를 교체한다. 편집 화면에서 타이핑할 때마다 호출된다. */
    fun updateBlockText(blockId: String, text: String) {
        _draft.update { draft ->
            draft?.copy(blocks = draft.blocks.map {
                if (it.id == blockId) it.copy(text = text) else it
            })
        }
    }

    /** 본문 블록 하나를 지운다. 사진 블록을 지우면 그 사진은 초안에서 빠진다. */
    fun removeBlock(blockId: String) {
        _draft.update { draft ->
            draft?.let { d ->
                val removed = d.blocks.firstOrNull { it.id == blockId }
                val remainingBlocks = d.blocks.filterNot { it.id == blockId }
                val stillReferencedElsewhere = removed?.imageUri != null &&
                    remainingBlocks.any { it.imageUri == removed.imageUri }
                d.copy(
                    blocks = remainingBlocks,
                    photos = if (removed?.imageUri != null && !stillReferencedElsewhere)
                        d.photos - removed.imageUri
                    else d.photos
                )
            }
        }
    }

    /** 블록을 한 칸 위(offset=-1) 또는 아래(offset=+1)로 이동. 경계 밖이면 무시 */
    fun moveBlock(blockId: String, offset: Int) {
        _draft.update { draft ->
            draft ?: return@update null
            val from = draft.blocks.indexOfFirst { it.id == blockId }
            val to = from + offset
            if (from < 0 || to !in draft.blocks.indices) return@update draft
            draft.copy(blocks = draft.blocks.toMutableList().apply { Collections.swap(this, from, to) })
        }
    }

    /** 빈 블록을 맨 끝에 추가. 위치는 ↑↓로 옮긴다 */
    fun addBlock() {
        _draft.update { draft ->
            draft?.copy(blocks = draft.blocks + DiaryBodyBlock(id = UUID.randomUUID().toString()))
        }
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
            // 편집 진입 id가 없어도, 같은 날짜에 이미 저장된 일기가 있으면 그 문서를 덮어써서
            // 하루 1개만 유지한다. (FAB·블록 플로우 등 어느 경로로 들어와도 중복 방지)
            val existingId = _existingEntryId.value
                ?: diaryRepository.getDiaryByDate(userId, d.date).getOrNull()?.id

            // 선택한 사진을 Firebase Storage에 업로드하고 다운로드 URL로 치환한다.
            // (기존 https URL은 재업로드 없이 그대로 유지)
            // 업로드 실패가 일기 저장 자체를 막지 않도록 사진별로 개별 처리한다 —
            // 성공한 사진만 저장하고, 실패한 사진은 로그만 남기고 건너뛴다.
            // 원본 URI를 키로 남겨야 본문 블록의 사진도 같은 URL로 치환할 수 있다.
            // (인덱스로 짝지으면 업로드 실패 시 어긋나 엉뚱한 사진이 블록에 붙는다)
            //
            // 대상은 _photos(수집 화면의 선택 상태)가 아니라 **지금 저장하는 초안**이다.
            // _photos를 기준으로 삼으면 (1) 기존 일기 편집 시 블록의 https URI가 매핑에서
            // 빠져 사진이 통째로 떨어지고 (2) 다른 날짜를 편집·저장할 때 이전 날짜 사진으로
            // 덮어써진다. 이미 https인 URI는 uploadDiaryPhoto가 재업로드 없이 그대로 돌려준다.
            val uploadedUrlByUri = d.photoUrisToSave()
                .mapNotNull { uri ->
                    runCatching { photoStorageDataSource.uploadDiaryPhoto(userId, d.date, uri) }
                        .onFailure { Log.e(TAG, "❌ 사진 업로드 실패(건너뜀): $uri", it) }
                        .getOrNull()?.let { uri to it }
                }
                .toMap()

            // 블록의 로컬 URI(content://)는 재설치·기기 변경 시 무효하므로 Storage URL로 바꾼다.
            // 업로드에 실패한 사진은 블록에서 사진만 떼고 문장은 남긴다.
            val savedBlocks = d.blocks.map { block ->
                if (block.imageUri == null) block
                else block.copy(imageUri = uploadedUrlByUri[block.imageUri])
            }

            val entry = DiaryEntry(
                id = existingId ?: "",
                title = formattedTitle,
                // 블록이 있으면 블록이 원본. 편집 결과가 content에 반영되도록 여기서 파생한다.
                content = if (savedBlocks.isNotEmpty()) savedBlocks.joinToString("\n\n") { it.text }
                          else d.editedContent ?: d.aiContent,
                blocks = savedBlocks,
                date = d.date,
                emotion = _selectedEmotion.value ?: "",
                weather = _selectedWeather.value ?: "",
                customEmotionText = _customEmotionText.value ?: "",
                customWeatherText = _customWeatherText.value ?: "",
                photos = uploadedUrlByUri.values.toList()
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

    /**
     * 편집 화면에서 날씨 칩을 고를 때 호출. 고정 5종(WEATHER_OPTIONS) 중 하나다.
     * 사용자가 직접 골랐으므로 질답의 "기타" 자유입력 원문은 지운다.
     */
    fun updateWeatherSelection(weather: String?) {
        _selectedWeather.value = weather
        _customWeatherText.value = null
    }

    /**
     * 편집 화면에서 감정 칩을 고를 때 호출. 고정 5종(EMOTION_OPTIONS) 중 하나다.
     * 사용자가 직접 골랐으므로 질답의 "기타" 자유입력 원문은 지운다.
     */
    fun updateEmotionSelection(emotion: String?) {
        _selectedEmotion.value = emotion
        _customEmotionText.value = null
    }

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
            // 블록화 이전에 저장된 일기는 본문 전체를 블록 1개로 승격해 편집 경로를 하나로 유지한다.
            blocks = entry.blocks.ifEmpty {
                listOf(DiaryBodyBlock(id = UUID.randomUUID().toString(), text = entry.content))
            },
            photos = entry.photos
        )
        _selectedWeather.value = entry.weather.ifEmpty { null }
        _selectedEmotion.value = entry.emotion.ifEmpty { null }
        _customWeatherText.value = entry.customWeatherText.ifEmpty { null }
        _customEmotionText.value = entry.customEmotionText.ifEmpty { null }
    }

    /** DraftPreviewScreen 재진입 시 이전 초안만 날리고 블록은 유지 */
    fun clearDraftOnly() {
        _draft.value = null
        _qnaCards.value = null
        _currentCardIndex.value = 0
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
        _qnaCards.value = null
        _currentCardIndex.value = 0
        _selectedWeather.value = null
        _selectedEmotion.value = null
        _customWeatherText.value = null
        _customEmotionText.value = null
        _existingEntryId.value = null
    }
}
