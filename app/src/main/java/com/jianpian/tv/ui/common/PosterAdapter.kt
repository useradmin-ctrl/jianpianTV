package com.jianpian.tv.ui.common

import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Scale
import com.jianpian.tv.R
import com.jianpian.tv.data.remote.model.VideoItem

class PosterAdapter(
    private val onItemClick: (VideoItem) -> Unit
) : RecyclerView.Adapter<PosterAdapter.VH>() {

    private val items = mutableListOf<VideoItem>()
    private val placeholderColor = 0xFF444444.toInt()

    fun submitList(newItems: List<VideoItem>) {
        items.clear()
        items.addAll(newItems)
        Log.d("PosterAdapter", "submitList: ${newItems.size} items, first poster=[${newItems.firstOrNull()?.posterUrl?.take(80)}]")
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_video, parent, false)
        val screenWidth = parent.resources.displayMetrics.widthPixels
        val cardWidth = screenWidth / 6
        val posterHeight = (cardWidth * 1.5).toInt()
        view.findViewById<ImageView>(R.id.poster_image).layoutParams.height = posterHeight
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        val placeholder = ColorDrawable(placeholderColor)
        holder.poster.setImageDrawable(placeholder)

        // 同时给 itemView 和 poster 设置点击监听，防止 TV 上 ImageView 拦截点击后不传递
        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.poster.setOnClickListener { onItemClick(item) }

        if (item.posterUrl.isBlank()) {
            Log.w("PosterAdapter", "[$position] blank poster for: ${item.title}")
            return
        }

        val request = ImageRequest.Builder(holder.itemView.context)
            .data(item.posterUrl)
            .crossfade(true)
            .scale(Scale.FILL)
            .placeholder(placeholder)
            .error(placeholder)
            .listener(
                onError = { _, result ->
                    Log.e("PosterAdapter", "[$position] FAIL ${item.title}: ${result.throwable.message}")
                },
                onSuccess = { _, _ ->
                    Log.d("PosterAdapter", "[$position] OK ${item.title}")
                }
            )
            .target(holder.poster)
            .build()

        holder.itemView.context.imageLoader.enqueue(request)
    }

    override fun getItemCount() = items.size

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val poster: ImageView = view.findViewById(R.id.poster_image)
        val title: TextView = view.findViewById(R.id.title_text)
    }
}
