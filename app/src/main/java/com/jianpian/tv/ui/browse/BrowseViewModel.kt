package com.jianpian.tv.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jianpian.tv.data.remote.model.VideoItem
import com.jianpian.tv.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val repository: VideoRepository
) : ViewModel() {

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _keyword = MutableStateFlow("")
    val keyword: StateFlow<String> = _keyword.asStateFlow()

    init { loadHomeVideos() }

    fun loadHomeVideos() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            repository.getHomeVideos().fold(
                onSuccess = {
                    _videos.value = it
                    android.util.Log.d("BrowseVM", "loaded ${it.size} videos, blanks=${it.count { v -> v.posterUrl.isBlank() }}")
                },
                onFailure = { _error.value = "加载首页失败: ${it.message}" }
            )
            _isLoading.value = false
        }
    }

    fun search(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return
        _keyword.value = trimmed
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            repository.search(trimmed).fold(
                onSuccess = { _videos.value = it },
                onFailure = { _error.value = "搜索失败: ${it.message}" }
            )
            _isLoading.value = false
        }
    }
}
