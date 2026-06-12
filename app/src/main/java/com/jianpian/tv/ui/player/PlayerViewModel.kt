package com.jianpian.tv.ui.player

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import com.jianpian.tv.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    application: Application,
    private val cache: SimpleCache
) : AndroidViewModel(application) {

    var exoPlayer: ExoPlayer? = null
        private set

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private val _isBuffering = MutableStateFlow(true)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private var currentCacheKey: String? = null

    fun play(m3u8Url: String, refererUrl: String) {
        _playbackError.value = null
        _isBuffering.value = true
        currentCacheKey = Uri.parse(m3u8Url).lastPathSegment ?: m3u8Url

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(Constants.USER_AGENT)
            .setDefaultRequestProperties(mapOf("Referer" to refererUrl, "Origin" to Constants.BASE_URL))
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)
            .setAllowCrossProtocolRedirects(true)

        val cacheFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val mediaSource = HlsMediaSource.Factory(cacheFactory)
            .createMediaSource(MediaItem.fromUri(m3u8Url))

        // 释放旧播放器再创建新的，避免串流残留
        exoPlayer?.release()
        exoPlayer = ExoPlayer.Builder(getApplication())
            .setSeekBackIncrementMs(15_000)
            .setSeekForwardIncrementMs(15_000)
            .build()
            .apply {
                playWhenReady = true
                addListener(playbackListener)
                setMediaSource(mediaSource)
                prepare()
            }
    }

    fun pause() { exoPlayer?.playWhenReady = false; _isPlaying.value = false }
    fun resume() { exoPlayer?.playWhenReady = true; _isPlaying.value = true }

    fun seek(deltaMs: Long) {
        exoPlayer?.let {
            it.seekTo((it.currentPosition + deltaMs).coerceIn(0, it.duration))
        }
    }

    fun stopAndClearCache() {
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        _isPlaying.value = false
        _isBuffering.value = false
        clearCurrentCache()
    }

    /** 只清除当前资源的缓存，不清除其他视频的 */
    private fun clearCurrentCache() {
        val key = currentCacheKey ?: return
        try {
            cache.keys.filter { it.contains(key) }.forEach {
                try { cache.removeResource(it) } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }

    private val playbackListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            _isPlaying.value = state == Player.STATE_READY && exoPlayer?.playWhenReady == true
            _isBuffering.value = state == Player.STATE_BUFFERING
        }
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            _playbackError.value = "播放失败: ${error.localizedMessage ?: "未知错误"}"
            _isBuffering.value = false
        }
    }
}
