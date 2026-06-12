# JianpianTV ProGuard Rules

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-dontwarn okio.**

# Keep Jsoup
-keep class org.jsoup.** { *; }

# Keep Media3
-keep class androidx.media3.** { *; }

# Keep Leanback
-keep class androidx.leanback.** { *; }

# Keep Coil
-keep class coil.** { *; }

# Keep data models (used by Gson/reflection in Media3)
-keep class com.jianpian.tv.data.remote.model.** { *; }
