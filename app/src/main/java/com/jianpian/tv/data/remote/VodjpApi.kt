package com.jianpian.tv.data.remote

import com.jianpian.tv.data.remote.model.EpisodeItem
import com.jianpian.tv.data.remote.model.MovieDetail
import com.jianpian.tv.data.remote.model.StreamInfo
import com.jianpian.tv.data.remote.model.VideoItem
import com.jianpian.tv.util.Constants
import com.jianpian.tv.util.HtmlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * vodjp.com API 封装 — OkHttp + Jsoup 网络层
 */
@Singleton
class VodjpApi @Inject constructor() {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(Constants.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(Constants.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .followRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    /** 获取首页视频列表，解析失败则用默认关键词搜索兜底 */
    suspend fun getHomeVideos(): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        val homeResult = fetchHtml(Constants.BASE_URL)
        if (homeResult.isSuccess) {
            val items = HtmlParser.extractSearchResults(homeResult.getOrThrow(), Constants.BASE_URL)
            if (items.isNotEmpty()) {
                return@withContext Result.success(items.map { (t, u, p) -> VideoItem(t, u, p) })
            }
        }
        // 首页无结果 → 用预设关键词聚合
        val seen = mutableSetOf<String>()
        val allVideos = mutableListOf<VideoItem>()
        for (kw in listOf("电影", "电视剧", "动漫")) {
            val result = search(kw)
            result.onSuccess { videos ->
                videos.forEach { v ->
                    if (seen.add(v.detailUrl)) allVideos.add(v)
                }
            }
        }
        if (allVideos.isEmpty()) Result.failure(IOException("获取首页数据失败"))
        else Result.success(allVideos)
    }

    /** 搜索最多取前2页（每页~36条，已足够） */
    suspend fun search(keyword: String): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        val allVideos = mutableListOf<VideoItem>()

        for (page in 1..2) {
            val url = "${Constants.BASE_URL}/jpsearch/$keyword----------$page---.html"
            val result = fetchHtml(url)

            if (result.isFailure) {
                return@withContext if (allVideos.isEmpty()) Result.failure(result.exceptionOrNull()!!)
                else Result.success(allVideos)
            }

            val items = HtmlParser.extractSearchResults(result.getOrThrow(), Constants.BASE_URL)
            if (items.isEmpty()) break
            items.forEach { (t, u, p) -> allVideos.add(VideoItem(t, u, p)) }
        }
        Result.success(allVideos)
    }

    suspend fun getEpisodes(detailUrl: String): Result<List<EpisodeItem>> = withContext(Dispatchers.IO) {
        fetchHtml(detailUrl).map { html ->
            HtmlParser.extractEpisodes(html, Constants.BASE_URL).map { (t, u) -> EpisodeItem(t, u) }
        }.mapCatching { list ->
            if (list.isEmpty()) throw IOException("未找到剧集列表") else list
        }
    }

    suspend fun getMovieDetail(detailUrl: String): Result<MovieDetail> = withContext(Dispatchers.IO) {
        fetchHtml(detailUrl).map { html ->
            val info = HtmlParser.extractMovieDetail(html)
            MovieDetail(
                title = info["title"] ?: "", score = info["score"] ?: "",
                type = info["type"] ?: "",   area = info["area"] ?: "",
                year = info["year"] ?: "",   actors = info["actors"] ?: "",
                director = info["director"] ?: "", description = info["description"] ?: "",
                posterUrl = info["posterUrl"] ?: ""
            )
        }
    }

    /** 从播放页提取 m3u8 地址，同时返回播放页 URL 作为 Referer */
    suspend fun getStreamUrl(playUrl: String): Result<StreamInfo> = withContext(Dispatchers.IO) {
        fetchHtml(playUrl).map { html ->
            HtmlParser.extractM3u8Url(html)
                ?: throw IOException("未找到播放地址")
        }.map { m3u8 -> StreamInfo(m3u8, playUrl) }
    }

    private fun fetchHtml(url: String): Result<String> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", Constants.USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "zh-CN,zh;q=0.9")
            .build()

        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string()
                ?: return Result.failure(IOException("响应体为空"))
            if (!response.isSuccessful) {
                // 检测 Cloudflare 拦截
                if ("just a moment" in body.lowercase() || "cf-browser" in body.lowercase()) {
                    return Result.failure(IOException("被Cloudflare拦截，请稍后重试"))
                }
                return Result.failure(IOException("HTTP ${response.code}"))
            }
            if (body.length < 500) {
                return Result.failure(IOException("响应内容过短 (${body.length}B)"))
            }
            Result.success(body)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }
}
