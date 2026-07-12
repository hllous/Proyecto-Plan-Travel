package com.hllous.plantravel

import android.app.Activity
import android.app.Application
import android.os.Bundle
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.hllous.plantravel.data.AppForegroundSignal
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.OkHttpClient

@HiltAndroidApp
class PlanTravelApp : Application(), SingletonImageLoader.Factory {

    @Inject lateinit var appForegroundSignal: AppForegroundSignal

    private val startedActivityCount = AtomicInteger(0)

    override fun onCreate() {
        super.onCreate()
        // No activity was started yet before this (0 -> 1 transition) covers both cold start
        // and returning from background: force every active realtime observer to resubscribe
        // and re-fetch, since Postgres Changes/Broadcast events fired while backgrounded are lost.
        registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                if (startedActivityCount.getAndIncrement() == 0) {
                    appForegroundSignal.notifyForeground()
                }
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivityCount.decrementAndGet()
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

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

