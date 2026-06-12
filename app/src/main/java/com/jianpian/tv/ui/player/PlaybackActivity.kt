package com.jianpian.tv.ui.player

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.ui.PlayerView
import com.jianpian.tv.data.repository.StreamRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackActivity : FragmentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    @Inject lateinit var streamRepository: StreamRepository

    private lateinit var playerView: PlayerView
    private lateinit var loadingText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var titleText: TextView
    private lateinit var errorText: TextView

    private var controlsVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(buildLayout())

        val episodeUrl = intent.getStringExtra("episode_url") ?: ""
        val episodeTitle = intent.getStringExtra("episode_title") ?: ""
        val videoTitle = intent.getStringExtra("video_title") ?: ""
        titleText.text = if (videoTitle.isNotBlank()) "$videoTitle - $episodeTitle" else episodeTitle

        if (episodeUrl.isNotBlank()) loadAndPlay(episodeUrl)
        else showError("无效的播放地址")

        lifecycleScope.launch {
            viewModel.playbackError.collect { error -> if (error != null) showError(error) }
        }
        lifecycleScope.launch {
            viewModel.isBuffering.collect { buffering ->
                progressBar.visibility = if (buffering) View.VISIBLE else View.GONE
                loadingText.visibility = if (buffering) View.VISIBLE else View.GONE
            }
        }
    }

    private fun loadAndPlay(episodeUrl: String) {
        showLoading()
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { streamRepository.getStreamUrl(episodeUrl) }
            result.fold(
                onSuccess = { info ->
                    viewModel.play(info.m3u8Url, info.refererUrl)
                    viewModel.exoPlayer?.let { playerView.player = it }
                    hideLoading()
                },
                onFailure = { e -> showError("获取播放地址失败: ${e.message}") }
            )
        }
    }

    private fun showLoading() { progressBar.visibility = View.VISIBLE; loadingText.visibility = View.VISIBLE; errorText.visibility = View.GONE }
    private fun hideLoading() { progressBar.visibility = View.GONE; loadingText.visibility = View.GONE }
    private fun showError(msg: String) { hideLoading(); errorText.text = msg; errorText.visibility = View.VISIBLE }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
            if (viewModel.isPlaying.value) viewModel.pause() else viewModel.resume()
            toggleControls(); true
        }
        KeyEvent.KEYCODE_DPAD_LEFT -> { viewModel.seek(-15_000); toggleControls(); true }
        KeyEvent.KEYCODE_DPAD_RIGHT -> { viewModel.seek(15_000); toggleControls(); true }
        KeyEvent.KEYCODE_BACK -> { viewModel.stopAndClearCache(); finish(); true }
        else -> { toggleControls(); super.onKeyDown(keyCode, event) }
    }

    private fun toggleControls() {
        controlsVisible = !controlsVisible
        titleText.visibility = if (controlsVisible) View.VISIBLE else View.GONE
        playerView.useController = controlsVisible
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopAndClearCache()
    }

    // ---- layout ---------------------------------------------------------------

    private fun buildLayout() = FrameLayout(this).apply {
        setBackgroundColor(0xFF000000.toInt())

        playerView = PlayerView(context).apply { useController = true; controllerAutoShow = false }
        addView(playerView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        titleText = label(20f, android.view.Gravity.TOP).also {
            it.setBackgroundColor(0x96000000.toInt())
            it.setShadowLayer(4f, 0f, 2f, 0xFF000000.toInt())
        }

        progressBar = ProgressBar(context).also {
            addView(it, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, android.view.Gravity.CENTER))
        }

        loadingText = label(18f, android.view.Gravity.CENTER_HORIZONTAL or android.view.Gravity.BOTTOM).also {
            it.text = "正在加载视频..."
            (it.layoutParams as FrameLayout.LayoutParams).bottomMargin = 80
        }

        errorText = label(18f, android.view.Gravity.CENTER).also {
            it.setTextColor(0xFFFF6B35.toInt())
            it.gravity = android.view.Gravity.CENTER
            it.setPadding(40, 0, 40, 0)
            it.visibility = View.GONE
        }
    }

    private fun FrameLayout.label(size: Float, gravity: Int) = TextView(context).apply {
        textSize = size; setTextColor(0xFFFFFFFF.toInt()); setPadding(32, 16, 32, 16)
        addView(this, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, gravity))
    }
}