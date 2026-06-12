package com.jianpian.tv.data.repository

import com.jianpian.tv.data.remote.VodjpApi
import com.jianpian.tv.data.remote.model.EpisodeItem
import com.jianpian.tv.data.remote.model.MovieDetail
import com.jianpian.tv.data.remote.model.VideoItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 视频数据仓库 — 封装搜索、详情、剧集获取
 */
@Singleton
class VideoRepository @Inject constructor(
    private val api: VodjpApi
) {
    suspend fun getHomeVideos(): Result<List<VideoItem>> = api.getHomeVideos()
    suspend fun search(keyword: String): Result<List<VideoItem>> = api.search(keyword)
    suspend fun getMovieDetail(detailUrl: String): Result<MovieDetail> = api.getMovieDetail(detailUrl)
    suspend fun getEpisodes(detailUrl: String): Result<List<EpisodeItem>> = api.getEpisodes(detailUrl)
}
