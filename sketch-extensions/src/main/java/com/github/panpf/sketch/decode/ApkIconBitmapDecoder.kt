package com.github.panpf.sketch.decode

import android.content.pm.PackageManager
import androidx.annotation.WorkerThread
import androidx.exifinterface.media.ExifInterface
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.datasource.DataFrom.LOCAL
import com.github.panpf.sketch.decode.internal.applyResize
import com.github.panpf.sketch.fetch.FetchResult
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.request.internal.RequestContext
import com.github.panpf.sketch.util.toBitmap
import java.io.IOException

/**
 * Extract the icon of the Apk file and convert it to Bitmap
 */
class ApkIconBitmapDecoder(
    private val sketch: Sketch,
    private val request: ImageRequest,
    private val fetchResult: FetchResult
) : BitmapDecoder {

    companion object {
        const val MIME_TYPE = "application/vnd.android.package-archive"
    }

    @WorkerThread
    override suspend fun decode(): BitmapDecodeResult {
        val file = fetchResult.dataSource.file()
        val packageManager = request.context.packageManager
        val packageInfo =
            packageManager.getPackageArchiveInfo(file.path, PackageManager.GET_ACTIVITIES)
                ?: throw IOException("getPackageArchiveInfo return null. ${file.path}")
        packageInfo.applicationInfo.sourceDir = file.path
        packageInfo.applicationInfo.publicSourceDir = file.path
        val drawable = packageManager.getApplicationIcon(packageInfo.applicationInfo)
        val bitmap = drawable.toBitmap(
            request.bitmapConfig?.getConfig(MIME_TYPE),
            bitmapPool = sketch.bitmapPool
        )
        val imageInfo = ImageInfo(bitmap.width, bitmap.height, MIME_TYPE)
        return BitmapDecodeResult(bitmap, imageInfo, ExifInterface.ORIENTATION_UNDEFINED, LOCAL)
            .applyResize(sketch, request.resize)
    }

    class Factory : BitmapDecoder.Factory {

        override fun create(
            sketch: Sketch,
            request: ImageRequest,
            requestContext: RequestContext,
            fetchResult: FetchResult
        ): BitmapDecoder? = if (MIME_TYPE.equals(fetchResult.mimeType, ignoreCase = true)) {
            ApkIconBitmapDecoder(sketch, request, fetchResult)
        } else {
            null
        }

        override fun toString(): String = "ApkIconBitmapDecoder"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            return true
        }

        override fun hashCode(): Int {
            return javaClass.hashCode()
        }
    }
}