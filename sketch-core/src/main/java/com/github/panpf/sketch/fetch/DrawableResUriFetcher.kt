package com.github.panpf.sketch.fetch

import android.net.Uri
import android.util.TypedValue
import android.webkit.MimeTypeMap
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.datasource.DrawableResDataSource
import com.github.panpf.sketch.request.LoadRequest
import com.github.panpf.sketch.request.internal.ImageRequest
import com.github.panpf.sketch.request.internal.UriInvalidException
import com.github.panpf.sketch.util.getMimeTypeFromUrl

fun newDrawableResUri(@DrawableRes drawableResId: Int): Uri =
    DrawableResUriFetcher.newUri(drawableResId)

/**
 * Support 'drawable.resource://5353453' uri
 */
class DrawableResUriFetcher(
    val sketch: Sketch,
    val request: LoadRequest,
    @RawRes @DrawableRes val drawableResId: Int
) : Fetcher {

    companion object {
        const val SCHEME = "drawable.resource"

        @JvmStatic
        fun newUri(@DrawableRes drawableResId: Int): Uri = Uri.parse("$SCHEME://$drawableResId")
    }

    override suspend fun fetch(): FetchResult {
        val resources = sketch.appContext.resources
        val path = TypedValue().apply { resources.getValue(drawableResId, this, true) }.string
        val entryName = path.substring(path.lastIndexOf('/'))
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromUrl(entryName)
        // todo 如果图片是 xml 的这里就会返回 text/xml 的 mimeType，并且后续解码时失败，应该新搞一个专门解码 xml drawable 的解码器，先读取 drawable 然后转成 bitmap
        return FetchResult(DrawableResDataSource(sketch, request, drawableResId), mimeType)
    }

    class Factory : Fetcher.Factory {
        override fun create(sketch: Sketch, request: ImageRequest): DrawableResUriFetcher? {
            val uri = request.uri
            return if (request is LoadRequest && SCHEME.equals(uri.scheme, ignoreCase = true)) {
                val drawableResId = uri.authority?.toIntOrNull()
                    ?: throw UriInvalidException(
                        request,
                        "Drawable resource uri 'drawableResId' part invalid. ${request.uriString}"
                    )
                DrawableResUriFetcher(sketch, request, drawableResId)
            } else {
                null
            }
        }
    }
}