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
package com.github.panpf.sketch.stateimage

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.request.RequestDepth.LOCAL
import com.github.panpf.sketch.request.internal.RequestDepthException
import com.github.panpf.sketch.request.internal.UriEmptyException
import com.github.panpf.sketch.request.isDepthFromSaveCellularTrafficDisplayInterceptor

class ErrorStateImage(
    val defaultErrorImage: StateImage,
    val emptyImage: StateImage? = null,
    val saveCellularTrafficImage: StateImage? = null,
) : StateImage {

    override fun getDrawable(
        context: Context, sketch: Sketch, request: DisplayRequest, error: Throwable?
    ): Drawable? = when {
        emptyImage != null && error is UriEmptyException -> {
            emptyImage.getDrawable(context, sketch, request, error)
        }
        saveCellularTrafficImage != null && error is RequestDepthException && error.depth == LOCAL
                && error.thenRequest.isDepthFromSaveCellularTrafficDisplayInterceptor() -> {
            saveCellularTrafficImage.getDrawable(context, sketch, request, error)
        }
        else -> {
            defaultErrorImage.getDrawable(context, sketch, request, error)
        }
    }
}