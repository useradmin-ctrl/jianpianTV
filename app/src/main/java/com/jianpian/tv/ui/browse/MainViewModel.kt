package com.jianpian.tv.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jianpian.tv.data.remote.model.VideoItem
import com.jianpian.tv.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryRow(
    val title: String,
    val videos: List<VideoItem>
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: VideoRepository
) : ViewModel() {

    private val _rows = MutableStateFlow<List<CategoryRow>>(emptyList())
    val rows: StateFlow<List<CategoryRow>> = _rows.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val isInSearchMode: Boolean
        get() = _rows.value.size == 1 && _rows.value[0].title.startsWith("搜索")

    init {
        loadAllCategories()
    }

    fun loadAllCategories() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            val categories = listOf("" to "首页", "电影" to "电影", "电视剧" to "电视剧", "动漫" to "动漫", "综艺" to "综艺")

            val deferred = categories.map { (kw, title) ->
                async {
                    val result = if (kw.isEmpty()) {
                        repository.getHomeVideos()
                    } else {
                        repository.search(kw)
                    }
                    result.fold(
                        onSuccess = { videos -> CategoryRow(title, videos) },
                        onFailure = { CategoryRow(title, emptyList()) }
                    )
                }
            }

            _rows.value = deferred.map { it.await() }
            _isLoading.value = false
        }
    }

    fun search(keyword: String) {
        if (keyword.isBlank()) return
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            val result = repository.search(keyword)
            result.fold(
                onSuccess = { videos ->
                    _rows.value = listOf(CategoryRow("搜索: $keyword", videos))
                },
                onFailure = { e ->
                    _error.value = "搜索失败: ${e.message}"
                }
            )
            _isLoading.value = false
        }
    }
}
