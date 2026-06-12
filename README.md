# 荐片TV (JianpianTV)

Android TV 流媒体播放应用，通过解析 [vodjp.com](https://vodjp.com) 网页内容提供视频搜索、浏览和播放功能。无后端 API 依赖，纯 HTML 抓取方案。

## 功能

- 视频搜索与分类浏览
- 影片详情（评分、类型、地区、年份、演员、导演、简介）
- 剧集选择与切换
- HLS 流媒体播放（m3u8，ExoPlayer + 本地缓存）
- 遥控器 D-pad 快进/快退（±15 秒），中键暂停/播放
- Cloudflare 反爬检测与应对

## 技术栈

| 层 | 技术 |
| --- | --- |
| 语言 | Kotlin |
| 构建 | Gradle 8.13, AGP 8.6.0, Kotlin 1.9.22 |
| UI | Android Leanback, Fragment + RecyclerView |
| 播放器 | Media3 ExoPlayer, HlsMediaSource |
| 网络 | OkHttp + Jsoup HTML 解析 |
| DI | Hilt 2.50 |
| 图片加载 | Coil |
| 缓存 | Media3 SimpleCache（LRU, 500MB） |
| 混淆 | ProGuard（仅 release） |

## 构建

```bash
# 调试 APK
./gradlew assembleDebug

# 发布 APK（已混淆）
./gradlew assembleRelease

# 安装到已连接设备
./gradlew installDebug

# 代码检查
./gradlew lint

# 清理
./gradlew clean
```

> SDK 路径配置于 `local.properties`。项目使用 Android Studio 内置 JBR。

## 架构

```text
UI（Fragment → ViewModel / StateFlow）
        ↓
Repository（VideoRepository, StreamRepository）
        ↓
VodjpApi（OkHttp + Jsoup HTML 抓取）
```

- **MainActivity** — 主容器，管理浏览/详情 Fragment
- **PlaybackActivity** — 独立全屏播放页
- **MainFragment**（Leanback BrowseSupportFragment）/ **BrowseFragment**（RecyclerView）— 两种可选浏览 UI，共享 MainViewModel
- 数据通过 `StateFlow` 驱动，Fragments 在 `lifecycleScope` 中收集

## 数据流

1. `VodjpApi` 请求 vodjp.com HTML 页面 → `HtmlParser`（Jsoup CSS 选择器 + 正则）→ `Result<T>`
2. `VideoRepository` 封装搜索、首页、详情、剧集接口
3. `StreamRepository` 从播放页 `<script>` 块提取 m3u8 地址（正则匹配 `player_aaaa` 块 → `.m3u8` 字面量 → JSON `"url"` 字段回退）
4. ViewModel 对外暴露 `StateFlow<UiState>`，含 loading/error 状态

## 抓取要点

- `BASE_URL` = `https://vodjp.com`
- 搜索 URL：`/jpsearch/{keyword}----------{page}---.html`
- 防 Cloudflare：检查响应体是否含 `"just a moment"` 或 `"cf-browser"`
- 海报 URL 解析：`data-original` → `data-src` → `src`，再处理相对路径

## 播放器缓存

- `SimpleCache` 单例，LRU 驱逐，最大 500MB
- 仅播放当前流时缓存，播放器销毁时仅清除当前流的缓存条目（不全量清除）
