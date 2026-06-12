package com.jianpian.tv.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jianpian.tv.data.remote.model.EpisodeItem
import com.jianpian.tv.data.remote.model.MovieDetail
import com.jianpian.tv.data.remote.model.VideoItem
import com.jianpian.tv.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 详情页 ViewModel — 管理影片详情和剧集数据
 */
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: VideoRepository
) : ViewModel() {

    private val _detail = MutableStateFlow<MovieDetail?>(null)
    val detail: StateFlow<MovieDetail?> = _detail.asStateFlow()

    private val _episodes = MutableStateFlow<List<EpisodeItem>>(emptyList())
    val episodes: StateFlow<List<EpisodeItem>> = _episodes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var _selectedEpisodeIndex = MutableStateFlow(-1)
    val selectedEpisodeIndex: StateFlow<Int> = _selectedEpisodeIndex.asStateFlow()

    fun loadVideoInfo(videoItem: VideoItem) {
        _isLoading.value = true
        _error.value = null

        // 先填充基本信息（从搜索结果中已有）
        _detail.value = MovieDetail(
            title = videoItem.title,
            posterUrl = videoItem.posterUrl
        )

        // 并行加载详情和剧集
        viewModelScope.launch {
            val detailDeferred = launch { loadDetail(videoItem.detailUrl) }
            val episodesDeferred = launch { loadEpisodes(videoItem.detailUrl) }
            detailDeferred.join()
            episodesDeferred.join()
            _isLoading.value = false
        }
    }

    private suspend fun loadDetail(detailUrl: String) {
        val result = repository.getMovieDetail(detailUrl)
        result.fold(
            onSuccess = { detail ->
                _detail.value = _detail.value?.copy(
                    score = detail.score,
                    type = detail.type,
                    area = detail.area,
                    year = detail.year,
                    actors = detail.actors,
                    director = detail.director,
                    description = detail.description,
                    posterUrl = detail.posterUrl.ifBlank { _detail.value?.posterUrl ?: "" }
                )
            },
            onFailure = { e ->
                _error.value = "加载详情失败: ${e.message}"
            }
        )
    }

    private suspend fun loadEpisodes(detailUrl: String) {
        val result = repository.getEpisodes(detailUrl)
        result.fold(
            onSuccess = { episodes ->
                _episodes.value = episodes
            },
            onFailure = { e ->
                _error.value = "加载剧集失败: ${e.message}"
            }
        )
    }

    fun selectEpisode(index: Int) {
        _selectedEpisodeIndex.value = index
    }
}
