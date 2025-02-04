package com.github.panpf.sketch.resize

import androidx.annotation.Keep
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.util.JsonSerializable
import com.github.panpf.sketch.util.JsonSerializer
import org.json.JSONObject

/**
 * The long image uses the specified precision, use the '[Precision.LESS_PIXELS]' for others.
 *
 * Note: The precision parameter can only be [Precision.EXACTLY] or [Precision.SAME_ASPECT_RATIO].
 */
fun longImageClipPrecision(precision: Precision): LongImageClipPrecisionDecider =
    LongImageClipPrecisionDecider(precision)

/**
 * The long image uses the specified precision, use the '[Precision.LESS_PIXELS]' for others.
 *
 * Note: The precision parameter can only be [Precision.EXACTLY] or [Precision.SAME_ASPECT_RATIO].
 */
@Keep
data class LongImageClipPrecisionDecider constructor(
    private val precision: Precision = Precision.SAME_ASPECT_RATIO,
) : PrecisionDecider {

    init {
        require(precision == Precision.EXACTLY || precision == Precision.SAME_ASPECT_RATIO) {
            "precision must be EXACTLY or SAME_ASPECT_RATIO"
        }
    }

    override val key: String by lazy { "LongImageClipPrecisionDecider($precision)" }

    override fun get(
        sketch: Sketch, imageWidth: Int, imageHeight: Int, resizeWidth: Int, resizeHeight: Int
    ): Precision {
        val longImageDecider = sketch.longImageDecider
        return if (longImageDecider.isLongImage(imageWidth, imageHeight, resizeWidth, resizeHeight))
            precision else Precision.LESS_PIXELS
    }

    override fun toString(): String = key

    override fun <T : JsonSerializable, T1 : JsonSerializer<T>> getSerializerClass(): Class<T1> {
        @Suppress("UNCHECKED_CAST")
        return Serializer::class.java as Class<T1>
    }

    @Keep
    class Serializer : JsonSerializer<LongImageClipPrecisionDecider> {
        override fun toJson(t: LongImageClipPrecisionDecider): JSONObject =
            JSONObject().apply {
                t.apply {
                    put("precision", precision.name)
                }
            }

        override fun fromJson(jsonObject: JSONObject): LongImageClipPrecisionDecider =
            LongImageClipPrecisionDecider(
                Precision.valueOf(jsonObject.getString("precision")),
            )
    }
}