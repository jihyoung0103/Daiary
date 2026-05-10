package com.smu.daiary.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smu.daiary.data.model.DiaryEntry
import com.smu.daiary.data.repository.DiaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val repository = DiaryRepository()
    private val _diaries = MutableStateFlow<List<DiaryEntry>>(emptyList())
    val diaries: StateFlow<List<DiaryEntry>> = _diaries.asStateFlow()

    // userId에 해당하는 diaries를 로드하는 함수.
    fun loadDiaries(userId: String) {
        viewModelScope.launch { // 비동기 작업
            repository.getDiaries(userId).collect { list -> _diaries.value = list }
            // repository.getDiaries(userId): userId의 일기를 읽어옴
            // .collect: 일기를 수집
            // { list -> _diaries.value = list }: 수집한 일기를 _diaries에 저장.
        }
    }
}
