package com.github.panpf.sketch.decode.resize

import androidx.annotation.Keep
import org.json.JSONObject

/**
 * Always return specified precision
 */
@Keep
data class FixedPrecisionDecider(private val precision: Precision) : PrecisionDecider {

    override fun precision(
        imageWidth: Int, imageHeight: Int, resizeWidth: Int, resizeHeight: Int
    ): Precision {
        return precision
    }

    @Keep
    constructor(jsonObject: JSONObject) : this(Precision.valueOf(jsonObject.getString("precision")))

    override fun serializationToJSON(): JSONObject =
        JSONObject().apply {
            put("precision", precision.name)
        }

    override fun toString(): String = "FixedPrecisionDecider($precision)"
}

fun fixedPrecision(precision: Precision): FixedPrecisionDecider = FixedPrecisionDecider(precision)