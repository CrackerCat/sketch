/*
 * Copyright (C) 2019 panpf <panpfpanpf@outlook.com>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.panpf.sketch.sample

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.multidex.MultiDexApplication
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.SketchFactory
import com.github.panpf.sketch.decode.ApkIconBitmapDecoder
import com.github.panpf.sketch.decode.AppIconBitmapDecoder
import com.github.panpf.sketch.decode.FFmpegVideoFrameBitmapDecoder
import com.github.panpf.sketch.decode.GifAnimatedDrawableDecoder
import com.github.panpf.sketch.decode.GifDrawableDrawableDecoder
import com.github.panpf.sketch.decode.GifMovieDrawableDecoder
import com.github.panpf.sketch.decode.HeifAnimatedDrawableDecoder
import com.github.panpf.sketch.decode.SvgBitmapDecoder
import com.github.panpf.sketch.decode.VideoFrameBitmapDecoder
import com.github.panpf.sketch.decode.WebpAnimatedDrawableDecoder
import com.github.panpf.sketch.fetch.AppIconUriFetcher
import com.github.panpf.sketch.http.OkHttpStack
import com.github.panpf.sketch.request.PauseLoadWhenScrollingDisplayInterceptor
import com.github.panpf.sketch.request.SaveCellularTrafficDisplayInterceptor
import com.github.panpf.sketch.sample.util.SettingsDisplayRequestInterceptor
import com.github.panpf.sketch.util.Logger
import com.tencent.mmkv.MMKV

class MyApplication : MultiDexApplication(), SketchFactory {

    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
    }

    override fun createSketch(): Sketch = Sketch.Builder(this).apply {
        logger(Logger(Logger.Level.valueOf(prefsService.logLevel.value)))
        httpStack(OkHttpStack.Builder().build())
        components {
            addRequestInterceptor(SettingsDisplayRequestInterceptor())
            addRequestInterceptor(SaveCellularTrafficDisplayInterceptor())
            addRequestInterceptor(PauseLoadWhenScrollingDisplayInterceptor())

            addFetcher(AppIconUriFetcher.Factory())

            addBitmapDecoder(SvgBitmapDecoder.Factory())
            addBitmapDecoder(ApkIconBitmapDecoder.Factory())
            addBitmapDecoder(AppIconBitmapDecoder.Factory())
            addBitmapDecoder(
                if (VERSION.SDK_INT >= VERSION_CODES.O_MR1) {
                    VideoFrameBitmapDecoder.Factory()
                } else {
                    FFmpegVideoFrameBitmapDecoder.Factory()
                }
            )

            addDrawableDecoder(
                when {
                    VERSION.SDK_INT >= VERSION_CODES.P -> GifAnimatedDrawableDecoder.Factory()
                    VERSION.SDK_INT >= VERSION_CODES.KITKAT -> GifMovieDrawableDecoder.Factory()
                    else -> GifDrawableDrawableDecoder.Factory()
                }
            )
            if (VERSION.SDK_INT >= VERSION_CODES.P) {
                addDrawableDecoder(WebpAnimatedDrawableDecoder.Factory())
            }
            if (VERSION.SDK_INT >= VERSION_CODES.R) {
                addDrawableDecoder(HeifAnimatedDrawableDecoder.Factory())
            }
        }
    }.build()
}