package com.jianpian.tv.ui.common

import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.leanback.widget.Presenter
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Scale
import com.jianpian.tv.R
import com.jianpian.tv.data.remote.model.EpisodeItem
import com.jianpian.tv.data.remote.model.VideoItem

class CardPresenter : Presenter() {

    private var defaultCardColor = 0

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        defaultCardColor = ContextCompat.getColor(parent.context, android.R.color.darker_gray)

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_video, parent, false)

        // Leanback rows use a fixed card size; set poster height proportionally
        val cardWidth = 180 * parent.resources.displayMetrics.density.toInt()
        val posterHeight = (cardWidth * 1.42).toInt()
        view.findViewById<ImageView>(R.id.poster_image).layoutParams.height = posterHeight

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        val posterView = viewHolder.view.findViewById<ImageView>(R.id.poster_image)
        val titleView = viewHolder.view.findViewById<TextView>(R.id.title_text)

        // 确保 ImageView 不抢占焦点
        posterView.isFocusable = false
        posterView.isClickable = false

        when (item) {
            is VideoItem -> {
                titleView.text = item.title
                loadPoster(posterView, item.posterUrl)
            }
            is EpisodeItem -> {
                titleView.text = item.title
                posterView.setImageDrawable(ColorDrawable(defaultCardColor))
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val posterView = viewHolder.view.findViewById<ImageView>(R.id.poster_image)
        posterView.setImageDrawable(null)
    }

    private fun loadPoster(imageView: ImageView, url: String) {
        if (url.isBlank()) {
            imageView.setImageDrawable(ColorDrawable(defaultCardColor))
            return
        }

        val request = ImageRequest.Builder(imageView.context)
            .data(url)
            .crossfade(true)
            .scale(Scale.FILL)
            .error(defaultCardColor)
            .placeholder(defaultCardColor)
            .target(imageView)
            .build()

        imageView.context.imageLoader.enqueue(request)
    }
}
