package com.github.panpf.sketch.resize

import androidx.annotation.Keep
import com.github.panpf.sketch.decode.Transformed
import com.github.panpf.sketch.util.JsonSerializable
import com.github.panpf.sketch.util.JsonSerializer
import org.json.JSONObject

class ResizeTransformed constructor(val resize: Resize) : Transformed {

    override val key: String by lazy {
        resize.key.replace("Resize", "ResizeTransformed")
    }
    override val cacheResultToDisk: Boolean = true

    override fun toString(): String = key

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ResizeTransformed

        if (resize != other.resize) return false

        return true
    }

    override fun hashCode(): Int {
        return resize.hashCode()
    }

    override fun <T : JsonSerializable, T1 : JsonSerializer<T>> getSerializerClass(): Class<T1> {
        @Suppress("UNCHECKED_CAST")
        return Serializer::class.java as Class<T1>
    }

    @Keep
    class Serializer : JsonSerializer<ResizeTransformed> {
        override fun toJson(t: ResizeTransformed): JSONObject =
            JSONObject().apply {
                t.apply {
                    val resizeSerializerClass =
                        resize.getSerializerClass<JsonSerializable, JsonSerializer<JsonSerializable>>()
                    val resizeSerializer = resizeSerializerClass.newInstance()
                    put("resizeSerializerClassName", resizeSerializerClass.name)
                    put("resizeContent", resizeSerializer.toJson(resize))
                }
            }

        override fun fromJson(jsonObject: JSONObject): ResizeTransformed =
            ResizeTransformed(
                (Class.forName(jsonObject.getString("resizeSerializerClassName"))
                    .newInstance() as JsonSerializer<*>)
                    .fromJson(jsonObject.getJSONObject("resizeContent")) as Resize,
            )
    }
}

fun List<Transformed>.getResizeTransformed(): ResizeTransformed? =
    find { it is ResizeTransformed } as ResizeTransformed?