# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (with ProGuard)
./gradlew assembleRelease

# Install debug build to connected device/emulator
./gradlew installDebug

# Lint
./gradlew lint

# Clean build
./gradlew clean
```

The Android SDK is expected at the path in `local.properties` (`sdk.dir`). The project uses Gradle 8.13 with Kotlin 1.9.22 and AGP 8.6.0.

## Architecture

**JianpianTV** is an Android TV streaming app that scrapes [vodjp.com](https://vodjp.com) for video content. There is no REST API — the entire data layer works by fetching HTML pages and parsing them with Jsoup.

### Layer stack

```
UI (Fragments → ViewModels/StateFlow)
        ↓
Repository (VideoRepository, StreamRepository)
        ↓
VodjpApi (OkHttp + Jsoup HTML scraping)
```

### Navigation

- `MainActivity` — container Activity, hosts browse/detail fragments via `FragmentManager`
- `PlaybackActivity` — separate fullscreen Activity for ExoPlayer playback
- `MainFragment` (Leanback `BrowseSupportFragment`) and `BrowseFragment` (custom `Fragment` with `RecyclerView`) are **two alternative browse UIs** that share `MainViewModel`
- `BrowseViewModel` is a simpler alternative ViewModel (single flat video list, no category rows) — currently unused by any Fragment

### Data flow

1. `VodjpApi` fetches HTML from vodjp.com (search, detail page, player page), parses with `HtmlParser` which uses Jsoup selectors + regex. Returns `Result<T>`.
2. `StreamRepository` wraps `VodjpApi.getStreamUrl()` — extracts m3u8 URL from player page `<script>` blocks (targeting `player_aaaa` blocks, regex for `.m3u8` literals, fallback to `"url":"https?://..."` JSON).
3. `VideoRepository` wraps search, home, detail, and episodes endpoints — all HTML-scraped.
4. ViewModels expose `StateFlow` for data/loading/error. Fragments collect these in `lifecycleScope`.
5. DI: Hilt `@Singleton` components in `SingletonComponent`. `NetworkModule` provides `VodjpApi`, `PlayerModule` provides `SimpleCache` (LRU eviction, 500MB, stored in `cacheDir/media_cache`).

### Key models

- `VideoItem(title, detailUrl, posterUrl)` — search result card
- `MovieDetail(...)` — parsed detail page fields (score, type, area, year, actors, director, description, posterUrl)
- `EpisodeItem(title, url)` — episode link from the playlist `<ul>`
- `StreamInfo(m3u8Url, refererUrl)` — HLS stream URL + Referer for Media3

### Player

`PlayerViewModel` creates an `ExoPlayer` with `HlsMediaSource` backed by `CacheDataSource.Factory`. The cache (`SimpleCache`) is a singleton. Referer/origin headers are set per-stream. The player supports D-pad seek (±15s) and center-button pause/resume. On destroy, only the current stream's cache entries are evicted (not the entire cache).

### Scraping details

- `Constants.BASE_URL` = `https://vodjp.com`
- Search URL pattern: `/jpsearch/{keyword}----------{page}---.html` (2 pages max)
- Cloudflare detection: checks response body for "just a moment" or "cf-browser"
- Poster URL resolution: attrs checked in order `data-original` → `data-src` → `src`, then resolved relative-to-absolute

### ProGuard

Release builds are minified. `proguard-rules.pro` keeps Hilt, OkHttp, Jsoup, Media3, Leanback, Coil, and all model classes.
