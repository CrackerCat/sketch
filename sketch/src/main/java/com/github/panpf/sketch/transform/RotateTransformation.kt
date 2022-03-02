package com.github.panpf.sketch.transform

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.annotation.Keep
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.cache.BitmapPool
import com.github.panpf.sketch.decode.Transformed
import com.github.panpf.sketch.request.LoadRequest
import org.json.JSONObject

class RotateTransformation(val degrees: Int) : Transformation {

    override val key: String = "RotateTransformation($degrees)"

    override suspend fun transform(
        sketch: Sketch,
        request: LoadRequest,
        input: Bitmap
    ): TransformResult? {
        if (degrees % 360 == 0) return null
        return TransformResult(
            rotate(input, degrees, sketch.bitmapPool),
            RotateTransformed(degrees)
        )
    }

    companion object {
        fun rotate(bitmap: Bitmap, degrees: Int, bitmapPool: BitmapPool): Bitmap {
            val matrix = Matrix()
            matrix.setRotate(degrees.toFloat())

            // 根据旋转角度计算新的图片的尺寸
            val newRect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            matrix.mapRect(newRect)
            val newWidth = newRect.width().toInt()
            val newHeight = newRect.height().toInt()

            // 角度不能整除90°时新图片会是斜的，因此要支持透明度，这样倾斜导致露出的部分就不会是黑的
            var config = bitmap.config ?: Bitmap.Config.ARGB_8888
            if (degrees % 90 != 0 && config != Bitmap.Config.ARGB_8888) {
                config = Bitmap.Config.ARGB_8888
            }
            val result = bitmapPool.getOrCreate(newWidth, newHeight, config)
            matrix.postTranslate(-newRect.left, -newRect.top)
            val canvas = Canvas(result)
            val paint = Paint(Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(bitmap, matrix, paint)
            return result
        }
    }
}

@Keep
class RotateTransformed(val degrees: Int) : Transformed {
    override val key: String = "RotateTransformed($degrees)"
    override val cacheResultToDisk: Boolean = true

    @Keep
    constructor(jsonObject: JSONObject) : this(jsonObject.getInt("degrees"))

    override fun serializationToJSON(): JSONObject =
        JSONObject().apply {
            put("degrees", degrees)
        }

    override fun toString(): String = key
}

fun List<Transformed>.getRotateTransformed(): RotateTransformed? =
    find { it is RotateTransformed } as RotateTransformed?