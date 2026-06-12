package com.jianpian.tv.util

import org.jsoup.Jsoup

/**
 * HTML 解析工具 — 对应 Python 版 BeautifulSoup 解析逻辑
 */
object HtmlParser {

    private fun resolveUrl(url: String, baseUrl: String): String {
        if (url.isBlank()) return ""
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        if (url.startsWith("//")) return "https:$url"
        return baseUrl + url
    }

    // 匹配 m3u8 URL 的正则（双引号+单引号）
    private val M3U8_URL_REGEX = Regex("""["'](https?://[^"']*\.m3u8[^"']*)["']""")
    private val URL_IN_JSON_REGEX = Regex(""""url"\s*:\s*"(https?://[^"]+)"""")

    /**
     * 从播放页 HTML 中提取 m3u8 地址
     *
     * 策略：
     * 1. 在 player_aaaa 脚本块中优先搜 .m3u8 字面量（最精确）
     * 2. 回退：搜 "url":"https?://..." JSON 字段（限制在 player 块内）
     */
    fun extractM3u8Url(html: String): String? {
        val doc = Jsoup.parse(html)
        val scripts = doc.select("script")

        for (script in scripts) {
            var text = script.html().takeIf { it.isNotBlank() } ?: continue
            if ("player_aaaa" !in text) continue

            // JSON 中 URL 可能被转义: https:\/\/... → 先还原
            text = text.replace("\\/", "/")

            // 1) 直接搜 m3u8 字面量（最可靠）
            M3U8_URL_REGEX.find(text)?.let { return it.groupValues[1] }

            // 2) 回退：搜 "url":"https?://..." JSON
            URL_IN_JSON_REGEX.find(text)?.let { return it.groupValues[1] }
            return null
        }
        return null
    }

    /**
     * 从剧集列表 HTML 片段中提取剧集
     */
    fun extractEpisodes(html: String, baseUrl: String): List<Pair<String, String>> {
        val doc = Jsoup.parse(html)
        val episodeList = doc.selectFirst("ul.stui-content__playlist") ?: return emptyList()

        return episodeList.select("li").mapNotNull { li ->
            val link = li.selectFirst("a") ?: return@mapNotNull null
            val title = link.text().trim()
            val url = link.attr("href").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val fullUrl = if (url.startsWith("http")) url else baseUrl + url
            title to fullUrl
        }
    }

    /**
     * 从搜索结果页提取视频列表
     */
    fun extractSearchResults(html: String, baseUrl: String): List<Triple<String, String, String>> {
        val doc = Jsoup.parse(html)
        val results = doc.select("li.stui-vodlist__item").mapNotNull { item ->
            val link = item.selectFirst("a.stui-vodlist__thumb") ?: return@mapNotNull null
            val title = link.attr("title").trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val href = link.attr("href").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val img = link.selectFirst("img")
            var poster = link.attr("data-original").trim()
            if (poster.isBlank()) poster = img?.attr("data-original")?.trim() ?: ""
            if (poster.isBlank()) poster = img?.attr("data-src")?.trim() ?: ""
            if (poster.isBlank()) poster = img?.attr("src")?.trim() ?: ""
            poster = resolveUrl(poster, baseUrl)
            Triple(title, if (href.startsWith("http")) href else baseUrl + href, poster)
        }
        if (results.isNotEmpty()) {
            val first = results.first()
            android.util.Log.d("HtmlParser", "extracted ${results.size} items, first=[title=${first.first}, poster=${first.third.take(100)}]")
        } else {
            android.util.Log.w("HtmlParser", "extracted 0 items from HTML (${html.length}B)")
        }
        return results
    }

    /**
     * 提取影片详情信息
     */
    fun extractMovieDetail(html: String): Map<String, String> {
        val doc = Jsoup.parse(html)
        val info = mutableMapOf<String, String>()

        doc.selectFirst("h3.title")?.let { titleElem ->
            val text = titleElem.text()
            val scoreText = titleElem.selectFirst("span.score")?.text() ?: ""
            info["title"] = text.replace(scoreText, "").trim()
            if (scoreText.isNotBlank()) info["score"] = scoreText
        }

        doc.select("p.data").forEach { elem ->
            val text = elem.text()
            text.split("：").let { parts ->
                if (parts.size < 2) return@let
                val label = parts[0]
                val value = parts.drop(1).joinToString("：").trim()
                when {
                    "类型" in label -> info["type"] = value.split("地区：").firstOrNull()?.trim() ?: value
                    "地区" in label -> {
                        val v = value.split("地区：").lastOrNull()?.trim() ?: value
                        info["area"] = v.split("年份：").firstOrNull()?.trim() ?: v
                    }
                    "年份" in label -> info["year"] = value
                    "主演" in label -> info["actors"] = value
                    "导演" in label -> info["director"] = value
                }
            }
        }

        doc.selectFirst("div.stui-content__desc")?.let {
            info["description"] = it.text().trim()
        }

        // 提取海报：先从 detail-pic 容器取 img data-original/src
        doc.selectFirst("div.stui-content__thumb, div.detail-pic, a.stui-vodlist__thumb")?.let { container ->
            val img = container.selectFirst("img")
            val poster = img?.attr("data-original")?.trim()?.takeIf { it.isNotBlank() }
                ?: img?.attr("data-src")?.trim()?.takeIf { it.isNotBlank() }
                ?: img?.attr("src")?.trim()?.takeIf { it.isNotBlank() }
                ?: ""
            if (poster.isNotBlank()) info["posterUrl"] = resolveUrl(poster, Constants.BASE_URL)
        }

        return info
    }
}
