package com.hllous.plantravel

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.hilt.android.HiltAndroidApp
import okhttp3.Interceptor
import okhttp3.OkHttpClient

@HiltAndroidApp
class PlanTravelApp : Application(), SingletonImageLoader.Factory {

    override fun newImageLoader(context: coil3.PlatformContext): ImageLoader {
        val client = OkHttpClient.Builder()
            .addNetworkInterceptor(
                Interceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", "PlanTravelApp/1.0 (Android)")
                        .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                        .build()
                    chain.proceed(request)
                },
            )
            .build()

        return ImageLoader.Builder(context)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = { client },
                    ),
                )
            }
            .build()
    }
}

