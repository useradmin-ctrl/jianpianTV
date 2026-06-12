package com.jianpian.tv.ui.detail

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ItemBridgeAdapter
import androidx.leanback.widget.VerticalGridView
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.size.Scale
import com.jianpian.tv.R
import com.jianpian.tv.data.remote.model.EpisodeItem
import com.jianpian.tv.data.remote.model.MovieDetail
import com.jianpian.tv.data.remote.model.VideoItem
import com.jianpian.tv.ui.player.PlaybackActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 影片详情页 — 展示详细信息 + 剧集网格
 */
@AndroidEntryPoint
class DetailFragment : Fragment() {

    private val viewModel: DetailViewModel by viewModels()
    private lateinit var videoItem: VideoItem

    private lateinit var titleText: TextView
    private lateinit var infoText: TextView
    private lateinit var descText: TextView
    private lateinit var posterImage: ImageView
    private lateinit var episodesGrid: VerticalGridView
    private lateinit var episodesAdapter: ArrayObjectAdapter

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_DETAIL_URL = "detailUrl"
        private const val ARG_POSTER_URL = "posterUrl"

        fun newInstance(videoItem: VideoItem): DetailFragment {
            return DetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, videoItem.title)
                    putString(ARG_DETAIL_URL, videoItem.detailUrl)
                    putString(ARG_POSTER_URL, videoItem.posterUrl)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        videoItem = VideoItem(
            title = arguments?.getString(ARG_TITLE) ?: "",
            detailUrl = arguments?.getString(ARG_DETAIL_URL) ?: "",
            posterUrl = arguments?.getString(ARG_POSTER_URL) ?: ""
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        titleText = view.findViewById(R.id.detail_title)
        infoText = view.findViewById(R.id.detail_info)
        descText = view.findViewById(R.id.detail_desc)
        posterImage = view.findViewById(R.id.detail_poster)
        episodesGrid = view.findViewById(R.id.episodes_grid)

        // 剧集适配器 — 点击由 EpisodePresenter 中的 setOnClickListener 处理
        val episodePresenter = EpisodePresenter { episode ->
            startActivity(
                Intent(requireContext(), PlaybackActivity::class.java).apply {
                    putExtra("episode_url", episode.url)
                    putExtra("episode_title", episode.title)
                    putExtra("video_title", videoItem.title)
                }
            )
        }
        episodesAdapter = ArrayObjectAdapter(episodePresenter)
        episodesGrid.adapter = ItemBridgeAdapter(episodesAdapter)

        // 数据观察
        lifecycleScope.launch {
            viewModel.detail.collect { detail -> detail?.let { updateUI(it) } }
        }
        lifecycleScope.launch {
            viewModel.episodes.collect { episodes ->
                episodesAdapter.clear()
                episodes.forEach { episodesAdapter.add(it) }
            }
        }
        lifecycleScope.launch {
            viewModel.error.collect { error ->
                if (error != null) descText.text = error
            }
        }

        viewModel.loadVideoInfo(videoItem)
    }

    private fun updateUI(detail: MovieDetail) {
        titleText.text = detail.title

        if (detail.posterUrl.isNotBlank()) {
            posterImage.load(detail.posterUrl) {
                crossfade(true)
                scale(Scale.FILL)
            }
        } else {
            posterImage.setImageDrawable(ColorDrawable(android.graphics.Color.DKGRAY))
        }

        val parts = mutableListOf<String>()
        if (detail.score.isNotBlank()) parts.add("⭐ ${detail.score}")
        if (detail.year.isNotBlank()) parts.add(detail.year)
        if (detail.type.isNotBlank()) parts.add(detail.type)
        if (detail.area.isNotBlank()) parts.add(detail.area)
        if (detail.director.isNotBlank()) parts.add("导演: ${detail.director}")
        if (detail.actors.isNotBlank()) parts.add("主演: ${detail.actors}")
        infoText.text = parts.joinToString("  |  ")

        descText.text = if (detail.description.isNotBlank()) {
            detail.description.take(250) + if (detail.description.length > 250) "..." else ""
        } else {
            ""
        }
    }
}

/**
 * 剧集 Presenter — 渲染单个剧集按钮，通过回调处理点击
 */
class EpisodePresenter(
    private val onEpisodeClicked: (EpisodeItem) -> Unit
) : androidx.leanback.widget.Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.episode_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        val episode = item as? EpisodeItem ?: return
        val tv = viewHolder.view.findViewById<TextView>(R.id.episode_text)
        tv.text = episode.title

        tv.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                tv.setBackgroundColor(android.graphics.Color.parseColor("#FF6B35"))
            } else {
                tv.setBackgroundColor(android.graphics.Color.argb(60, 255, 255, 255))
            }
        }

        viewHolder.view.setOnClickListener {
            onEpisodeClicked(episode)
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        viewHolder.view.setOnFocusChangeListener(null)
        viewHolder.view.setOnClickListener(null)
    }
}
