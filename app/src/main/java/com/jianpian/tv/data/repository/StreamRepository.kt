package com.jianpian.tv.data.repository

import com.jianpian.tv.data.remote.VodjpApi
import com.jianpian.tv.data.remote.model.StreamInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 流媒体地址仓库 — 提取 m3u8 播放地址
 */
@Singleton
class StreamRepository @Inject constructor(
    private val api: VodjpApi
) {
    suspend fun getStreamUrl(playUrl: String): Result<StreamInfo> = api.getStreamUrl(playUrl)
}
