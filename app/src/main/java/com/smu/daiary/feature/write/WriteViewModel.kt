package com.smu.daiary.feature.write

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smu.daiary.data.model.DiaryEntry
import com.smu.daiary.data.repository.DailyDataRepository
import com.smu.daiary.data.repository.DiaryRepository
import com.smu.daiary.data.source.CalendarDataSource
import com.smu.daiary.data.source.PhotoDataSource
import com.smu.daiary.data.source.WeatherDataSource
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val TAG = "WriteViewModel"

class WriteViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // Repositories & DataSources
    private val diaryRepository = DiaryRepository()
    private val dailyDataRepository = DailyDataRepository()
    private val weatherDataSource = WeatherDataSource(context)
    private val photoDataSource = PhotoDataSource(context)
    private val calendarDataSource = CalendarDataSource(context)

    // 블록 목록 (실제 수집 데이터로 채워짐)
    private val _blocks = MutableStateFlow<List<ContentBlock>>(emptyList())
    val blocks: StateFlow<List<ContentBlock>> = _blocks.asStateFlow()

    // 데이터 로딩 상태
    private val _isLoadingBlocks = MutableStateFlow(false)
    val isLoadingBlocks: StateFlow<Boolean> = _isLoadingBlocks.asStateFlow()

    // 초안 상태
    private val _draft = MutableStateFlow<DiaryDraft?>(null)
    val draft: StateFlow<DiaryDraft?> = _draft.asStateFlow()

    // 저장 중 상태
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // 날씨·감정 선택
    private val _selectedWeather = MutableStateFlow<String?>(null)
    val selectedWeather: StateFlow<String?> = _selectedWeather.asStateFlow()

    private val _selectedEmotion = MutableStateFlow<String?>(null)
    val selectedEmotion: StateFlow<String?> = _selectedEmotion.asStateFlow()

    /**
     * 실제 DataSource로부터 오늘 데이터를 수집하고
     * DailyDataRepository에 저장한 뒤 블록 목록을 구성합니다.
     * 각 항목은 병렬로 수집되어 실패해도 다른 항목에 영향을 주지 않습니다.
     */
    fun loadBlocks(userId: String) {
        viewModelScope.launch {
            _isLoadingBlocks.value = true
            _blocks.value = emptyList()

            val date = LocalDate.now().toString()
            val blocks = mutableListOf<ContentBlock>()

            Log.d(TAG, "📡 데이터 수집 시작 | userId=$userId, date=$date")

            // --- 날씨, 캘린더, 사진 병렬 수집 ---
            val weatherDeferred = async { runCatching { weatherDataSource.fetchWeather() } }
            val calendarDeferred = async { runCatching { calendarDataSource.fetchTodayEvents() } }
            val photoDeferred = async { runCatching { photoDataSource.fetchTodayPhotos() } }

            // 날씨
            weatherDeferred.await()
                .onSuccess { weather ->
                    Log.d(TAG, "🌤️ 날씨 수집 완료: ${weather.description} ${weather.temperature}°C")
                    dailyDataRepository.updateWeather(userId, date, weather)
                    blocks.add(ContentBlock(
                        id = "weather", type = BlockType.WEATHER,
                        content = "${weather.description} ${weather.temperature.toInt()}°C · 습도 ${weather.humidity}%"
                    ))
                }
                .onFailure {
                    Log.w(TAG, "⚠️ 날씨 수집 실패 (권한 또는 네트워크 문제)", it)
                    blocks.add(ContentBlock(id = "weather", type = BlockType.WEATHER, content = "날씨 정보를 가져올 수 없습니다"))
                }

            // 캘린더
            calendarDeferred.await()
                .onSuccess { events ->
                    Log.d(TAG, "📅 캘린더 수집 완료: ${events.size}개")
                    dailyDataRepository.updateCalendar(userId, date, events)
                    if (events.isEmpty()) {
                        blocks.add(ContentBlock(id = "calendar_0", type = BlockType.CALENDAR, content = "오늘 등록된 일정이 없습니다"))
                    } else {
                        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                        events.forEachIndexed { i, event ->
                            val startStr = Instant.ofEpochMilli(event.startTime)
                                .atZone(ZoneId.systemDefault()).format(timeFormatter)
                            val locationPart = if (event.location.isNotBlank()) " · ${event.location}" else ""
                            blocks.add(ContentBlock(
                                id = "calendar_$i", type = BlockType.CALENDAR,
                                content = "${event.title} $startStr$locationPart"
                            ))
                        }
                    }
                }
                .onFailure {
                    Log.w(TAG, "⚠️ 캘린더 수집 실패 (권한 문제)", it)
                    blocks.add(ContentBlock(id = "calendar_0", type = BlockType.CALENDAR, content = "캘린더 정보를 가져올 수 없습니다"))
                }

            // 사진
            photoDeferred.await()
                .onSuccess { photos ->
                    Log.d(TAG, "🖼️ 사진 수집 완료: ${photos.size}장")
                    dailyDataRepository.updatePhotos(userId, date, photos)
                    if (photos.isNotEmpty()) {
                        blocks.add(ContentBlock(id = "photo", type = BlockType.PHOTO, content = "오늘 찍은 사진 ${photos.size}장"))
                    }
                }
                .onFailure { Log.w(TAG, "⚠️ 사진 수집 실패 (권한 문제)", it) }

            // 결제 내역 (NotificationListenerService가 Firestore에 저장해둔 데이터를 읽어옴)
            dailyDataRepository.getDailyData(userId, date)
                .onSuccess { dailyData ->
                    val payments = dailyData?.payments ?: emptyList()
                    Log.d(TAG, "💳 결제 내역 로드 완료: ${payments.size}건")
                    payments.forEachIndexed { i, payment ->
                        blocks.add(ContentBlock(
                            id = "payment_$i", type = BlockType.PAYMENT,
                            content = "${payment.merchant} ${String.format("%,d", payment.amount)}원"
                        ))
                    }
                }
                .onFailure { Log.w(TAG, "⚠️ 결제 내역 로드 실패", it) }

            Log.d(TAG, "✅ 전체 수집 완료 | 블록 수=${blocks.size}")
            _blocks.value = blocks
            _isLoadingBlocks.value = false
        }
    }

    fun toggleBlock(id: String) {
        _blocks.update { list ->
            list.map { if (it.id == id) it.copy(isSelected = !it.isSelected) else it }
        }
    }

    fun generateDraft() {
        val selected = _blocks.value.filter { it.isSelected }
        if (selected.isEmpty()) return
        val today = LocalDate.now().toString()
        val content = buildString {
            appendLine("오늘 하루를 되돌아보면,")
            appendLine()
            selected.forEach { block ->
                when (block.type) {
                    BlockType.PAYMENT  -> appendLine("• ${block.content}를 이용했다.")
                    BlockType.PHOTO    -> appendLine("• ${block.content}을 남겼다.")
                    BlockType.CALENDAR -> appendLine("• ${block.content}이 있었다.")
                    BlockType.HEALTH   -> appendLine("• 오늘의 건강: ${block.content}")
                    BlockType.WEATHER  -> appendLine("• 날씨는 ${block.content}이었다.")
                }
            }
            appendLine()
            append("오늘도 수고했다.")
        }
        _draft.value = DiaryDraft(date = today, aiContent = content)
    }

    fun updateEditedContent(content: String) {
        _draft.update { it?.copy(editedContent = content) }
    }

    fun addPhoto(uri: String) {
        _draft.update { it?.copy(photos = it.photos + uri) }
    }

    fun removePhoto(uri: String) {
        _draft.update { it?.copy(photos = it.photos.filter { p -> p != uri }) }
    }

    fun saveDraft(userId: String, onComplete: (Boolean) -> Unit) {
        val d = _draft.value ?: return
        _isSaving.value = true
        viewModelScope.launch {
            val entry = DiaryEntry(
                title = "${d.date} 일기",
                content = d.editedContent ?: d.aiContent,
                date = d.date
            )
            val result = diaryRepository.addDiary(userId, entry)
            _isSaving.value = false
            _draft.update { it?.copy(status = if (result.isSuccess) DraftStatus.SAVED else DraftStatus.IDLE) }
            onComplete(result.isSuccess)
        }
    }

    fun updateWeatherSelection(weather: String?) { _selectedWeather.value = weather }
    fun updateEmotionSelection(emotion: String?) { _selectedEmotion.value = emotion }

    fun resetDraft() {
        _draft.value = null
        _blocks.value = emptyList()
        _selectedWeather.value = null
        _selectedEmotion.value = null
    }
}
