package com.jianpian.tv

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.jianpian.tv.util.Constants
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class JianpianTVApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        Log.d("JianpianTVApp", "Creating Coil ImageLoader with custom OkHttp client")
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val req = chain.request()
                Log.d("CoilNet", "--> ${req.url}")
                val response = chain.proceed(
                    req.newBuilder()
                        .header("User-Agent", Constants.USER_AGENT)
                        .header("Referer", Constants.BASE_URL)
                        .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                        .build()
                )
                Log.d("CoilNet", "<-- ${response.code} ${req.url}")
                response
            }
            .build()
        return ImageLoader.Builder(this)
            .okHttpClient(client)
            .crossfade(true)
            .memoryCache(MemoryCache.Builder(this).maxSizePercent(0.125).build())
            .diskCache(DiskCache.Builder().directory(cacheDir.resolve("coil_cache")).maxSizePercent(0.02).build())
            .build()
    }
}
