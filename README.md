# 荐片TV (JianpianTV)

Android TV 流媒体播放应用，通过解析 [vodjp.com](https://vodjp.com) 网页内容提供视频搜索、浏览和播放功能。无后端 API 依赖，纯 HTML 抓取方案。

## 功能

- **分类浏览** — 首页 / 电影 / 电视剧 / 动漫 / 综艺 五类切换
- **视频搜索** — 对话框输入关键字，最多取 2 页搜索结果
- **影片详情** — 评分、类型、地区、年份、演员、导演、简介
- **剧集选择** — 从播放列表 `<ul>` 解析剧集，支持切换
- **HLS 播放** — m3u8 流媒体，ExoPlayer + LRU 本地缓存（500MB）
- **遥控器适配** — D-pad 快进/快退（±15s），确认键暂停/恢复
- **Cloudflare 反爬检测** — 响应体中识别 `"just a moment"` / `"cf-browser"`，返回友好提示

## 技术栈

| 层 | 技术 |
| --- | --- |
| 语言 | Kotlin |
| 构建 | Gradle 8.13, AGP 8.6.0, Kotlin 1.9.22 |
| UI | Fragment + RecyclerView（GridLayoutManager, 6 列） |
| 播放器 | Media3 ExoPlayer 1.2.1（HLS） |
| 网络 | OkHttp 4.12.0 + Jsoup 1.17.2 HTML 解析 |
| DI | Hilt 2.50（kapt） |
| 图片加载 | Coil 2.6.0 |
| 缓存 | Media3 SimpleCache（LRU, 500MB, `cacheDir/media_cache`） |
| 生命周期 | Kotlin Coroutines + StateFlow + Lifecycle 2.7.0 |
| 混淆 | ProGuard（仅 release） |
| 最低 API | 21（Android 5.0） |
| 目标 API | 35（Android 15） |

## 项目结构

```
```text
app/src/main/java/com/jianpian/tv/
├── JianpianTVApp.kt              # Application，Hilt 入口
├── MainActivity.kt                # FragmentActivity 容器，宿主 BrowseFragment
├── data/
│   ├── remote/
│   │   ├── VodjpApi.kt            # OkHttp + Jsoup 网络层（所有 API 方法）
│   │   └── model/
│   │       ├── VideoItem.kt       # 搜索结果卡片（title, detailUrl, posterUrl）
│   │       ├── MovieDetail.kt     # 影片详情（score, type, area, year, actors...）
│   │       ├── EpisodeItem.kt     # 剧集项（title, url）
│   │       └── StreamInfo.kt      # 流信息（m3u8Url, refererUrl）
│   └── repository/
│       ├── VideoRepository.kt     # 封装搜索、首页、详情、剧集
│       └── StreamRepository.kt    # 封装 m3u8 地址提取
├── di/
│   ├── NetworkModule.kt           # 提供 VodjpApi 单例
│   └── PlayerModule.kt            # 提供 SimpleCache 单例
├── ui/
│   ├── browse/
│   │   ├── BrowseFragment.kt      # 主界面（RecyclerView + 分类栏 + 搜索）
│   │   └── MainViewModel.kt       # 分类/搜索 ViewModel，StateFlow 驱动
│   ├── common/
│   │   ├── PosterAdapter.kt       # RecyclerView 适配器（当前使用）
│   │   └── CardPresenter.kt       # Leanback Presenter（未使用，保留兼容）
│   ├── detail/
│   │   ├── DetailFragment.kt      # 详情页（信息 + 剧集列表）
│   │   └── DetailViewModel.kt     # 并行加载详情和剧集
│   └── player/
│       ├── PlaybackActivity.kt    # 全屏播放 Activity
│       └── PlayerViewModel.kt     # ExoPlayer 管理 + 缓存清理
└── util/
    ├── Constants.kt               # BASE_URL, User-Agent, 超时等常量
    └── HtmlParser.kt              # Jsoup CSS 选择器 + 正则解析
```

> `BrowseViewModel` 是一个更简单的备选 ViewModel（单一视频列表，无分类行），目前未被任何 Fragment 引用。
> `CardPresenter` 是 Leanback `Presenter` 实现，当前 UI 使用 `PosterAdapter`（RecyclerView.Adapter），两者保留为后续 UI 方案切换提供兼容。

## 架构

```text
UI（Fragment → ViewModel / StateFlow）
        ↓
Repository（VideoRepository, StreamRepository）
        ↓
VodjpApi（OkHttp + Jsoup HTML 抓取）
```

### 导航

- **MainActivity** — 单 Activity 容器，通过 `FragmentManager` 管理 Fragment 导航
- **BrowseFragment** — 主界面，顶部分类栏（首页/电影/电视剧/动漫/综艺）+ 6 列 GridLayoutManager 视频网格 + 搜索按钮
- **DetailFragment** — 点击卡片后进入，显示影片详情 + 剧集列表
- **PlaybackActivity** — 独立全屏 Activity，运行 ExoPlayer

## 数据流

1. `VodjpApi` 请求 vodjp.com HTML 页面 → `HtmlParser`（Jsoup CSS 选择器 + 正则）→ `Result<T>`
2. `VideoRepository` 封装搜索、首页、详情、剧集接口
3. `StreamRepository` 从播放页 `<script>` 块提取 m3u8 地址：`player_aaaa` 块 → `.m3u8` 字面量 → JSON `"url"` 字段回退
4. ViewModel 对外暴露 `StateFlow<UiState>`，Fragment 在 `lifecycleScope` 中收集

## 抓取要点

| 项目 | 值 |
| --- | --- |
| BASE_URL | `https://vodjp.com` |
| 搜索 URL | `/jpsearch/{keyword}----------{page}---.html`（最多 2 页） |
| 搜索页 CSS | `li.stui-vodlist__item` → `a.stui-vodlist__thumb` |
| 海报优先级 | `data-original` → `data-src` → `src`，相对路径自动补全 |
| 剧列表 CSS | `ul.stui-content__playlist` → `li > a` |
| 详情字段 | `h3.title`（标题+评分），`p.data`（类型/地区/年份/演员/导演），`div.stui-content__desc`（简介） |
| Cloudflare | 检测 `"just a moment"` / `"cf-browser"` 关键字 |
| 超时 | connect/read 均 15 秒 |

## 播放器

- **ExoPlayer** 通过 `HlsMediaSource.Factory` + `CacheDataSource.Factory` 创建
- **Referer/Origin** 按 stream 设置，指向 `vodjp.com`
- **缓存策略** — `SimpleCache` 单例，LRU 驱逐，最大 500MB
- **清理策略** — 播放器销毁时仅清除当前流的缓存条目，不全量清除
- **D-pad 控制** — 快进/快退 15 秒增量

## 构建

```bash
# 调试 APK
./gradlew assembleDebug

# 发布 APK（混淆）
./gradlew assembleRelease

# 安装到已连接设备/模拟器
./gradlew installDebug

# 代码检查
./gradlew lint

# 清理
./gradlew clean
```

> SDK 路径配置于 `local.properties`（`sdk.dir`）。项目使用 Android Studio 内置 JBR。

## ProGuard

Release 构建已开启混淆，`proguard-rules.pro` 保留了 Hilt、OkHttp、Jsoup、Media3、Leanback、Coil 及所有数据模型类。
