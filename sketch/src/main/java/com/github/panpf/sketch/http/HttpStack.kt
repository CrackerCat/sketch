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
package com.github.panpf.sketch.http

import com.github.panpf.sketch.request.ImageRequest
import java.io.IOException
import java.io.InputStream

/**
 * Responsible for sending HTTP requests and returning responses
 */
interface HttpStack {

    companion object {
        const val DEFAULT_TIMEOUT = 7 * 1000
    }

    @Throws(IOException::class)
    fun getResponse(request: ImageRequest, url: String): Response

    interface Response {
        @get:Throws(IOException::class)
        val code: Int

        @get:Throws(IOException::class)
        val message: String?

        val contentLength: Long

        val contentType: String?

        fun getHeaderField(name: String): String?

        @get:Throws(IOException::class)
        val content: InputStream
    }
}