package com.github.panpf.sketch.sample.util

import android.graphics.ColorSpace
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.MainThread
import com.github.panpf.sketch.cache.CachePolicy.DISABLED
import com.github.panpf.sketch.cache.CachePolicy.ENABLED
import com.github.panpf.sketch.decode.BitmapConfig
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.request.ImageData
import com.github.panpf.sketch.request.RequestInterceptor
import com.github.panpf.sketch.request.RequestInterceptor.Chain
import com.github.panpf.sketch.request.pauseLoadWhenScrolling
import com.github.panpf.sketch.request.saveCellularTraffic
import com.github.panpf.sketch.sample.prefsService
import com.github.panpf.sketch.sample.widget.MyListImageView
import com.github.panpf.sketch.target.ViewTarget

class SettingsDisplayRequestInterceptor : RequestInterceptor {

    @MainThread
    override suspend fun intercept(chain: Chain): ImageData {
        val request = chain.request
        if (request !is DisplayRequest) {
            return chain.proceed(request)
        }

        val newRequest = request.newDisplayRequest {
            val prefsService = request.context.prefsService
            if (request.definedOptions.memoryCachePolicy == null) {
                if (prefsService.disabledBitmapMemoryCache.value) {
                    memoryCachePolicy(DISABLED)
                } else {
                    memoryCachePolicy(ENABLED)
                }
            }
            if (request.definedOptions.downloadCachePolicy == null) {
                if (prefsService.disabledDownloadDiskCache.value) {
                    downloadCachePolicy(DISABLED)
                } else {
                    downloadCachePolicy(ENABLED)
                }
            }
            if (request.definedOptions.resultCachePolicy == null) {
                if (prefsService.disabledBitmapResultDiskCache.value) {
                    resultCachePolicy(DISABLED)
                } else {
                    resultCachePolicy(ENABLED)
                }
            }
            if (request.definedOptions.disallowReuseBitmap == null) {
                disallowReuseBitmap(prefsService.disallowReuseBitmap.value)
            }
            if (request.definedOptions.ignoreExifOrientation == null) {
                ignoreExifOrientation(prefsService.ignoreExifOrientation.value)
            }
            @Suppress("DEPRECATION")
            if (request.definedOptions.preferQualityOverSpeed == null) {
                preferQualityOverSpeed(VERSION.SDK_INT < VERSION_CODES.N && prefsService.inPreferQualityOverSpeed.value)
            }
            if (request.definedOptions.bitmapConfig == null) {
                when (prefsService.bitmapQuality.value) {
                    "LOW" -> bitmapConfig(BitmapConfig.LowQuality)
                    "HIGH" -> bitmapConfig(BitmapConfig.HighQuality)
                    else -> bitmapConfig(null)
                }
            }
            if (VERSION.SDK_INT >= VERSION_CODES.O && request.definedOptions.colorSpace == null) {
                when (val value = prefsService.colorSpace.value) {
                    "Default" -> {
                        colorSpace(null)
                    }
                    else -> {
                        colorSpace(ColorSpace.get(ColorSpace.Named.valueOf(value)))
                    }
                }
            }
            val target = chain.request.target
            if (target is ViewTarget<*>) {
                val view = target.view
                if (view is MyListImageView) {
                    if (request.definedOptions.disallowAnimatedImage == null) {
                        disallowAnimatedImage(prefsService.disallowAnimatedImageInList.value)
                    }
                    pauseLoadWhenScrolling(prefsService.pauseLoadWhenScrollInList.value)
                    saveCellularTraffic(prefsService.saveCellularTrafficInList.value)
                }
            }
        }
        return chain.proceed(newRequest)
    }

    override fun toString(): String = "SettingsDisplayRequestInterceptor"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}