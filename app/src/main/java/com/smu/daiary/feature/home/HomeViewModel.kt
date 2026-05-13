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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isDeletingDiary = MutableStateFlow(false)
    val isDeletingDiary: StateFlow<Boolean> = _isDeletingDiary.asStateFlow()

    fun loadDiaries(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                repository.getDiaries(userId).collect { list ->
                    _diaries.value = list
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = e.message
            }
        }
    }

    fun deleteDiary(userId: String, diaryId: String, onResult: (Boolean) -> Unit) {
        _isDeletingDiary.value = true
        viewModelScope.launch {
            val result = repository.deleteDiary(userId, diaryId)
            _isDeletingDiary.value = false
            onResult(result.isSuccess)
        }
    }
}
