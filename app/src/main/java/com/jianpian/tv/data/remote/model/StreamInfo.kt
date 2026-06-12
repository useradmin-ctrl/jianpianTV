package com.jianpian.tv.data.remote.model

/**
 * 流媒体信息（传递给播放器）
 */
data class StreamInfo(
    val m3u8Url: String,
    val refererUrl: String  // 播放页 URL，用作 Referer
)
