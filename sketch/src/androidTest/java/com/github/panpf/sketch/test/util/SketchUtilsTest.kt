package com.github.panpf.sketch.test.util

import android.graphics.Bitmap
import android.graphics.Bitmap.Config.RGB_565
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.TransitionDrawable
import android.os.Build
import android.widget.ImageView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.panpf.sketch.cache.CountBitmap
import com.github.panpf.sketch.datasource.DataFrom.LOCAL
import com.github.panpf.sketch.decode.ImageInfo
import com.github.panpf.sketch.displayAssetImage
import com.github.panpf.sketch.drawable.SketchBitmapDrawable
import com.github.panpf.sketch.drawable.SketchCountBitmapDrawable
import com.github.panpf.sketch.drawable.internal.CrossfadeDrawable
import com.github.panpf.sketch.sketch
import com.github.panpf.sketch.test.utils.InternalDrawableWrapperImpl
import com.github.panpf.sketch.util.SketchUtils
import com.github.panpf.sketch.util.findLastSketchDrawable
import com.github.panpf.sketch.util.foreachSketchCountDrawable
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import androidx.appcompat.graphics.drawable.DrawableWrapper as CompatDrawableWrapper

@RunWith(AndroidJUnit4::class)
class SketchUtilsTest {

    @Test
    fun testRequestManagerOrNull() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val imageView = ImageView(context)

        Assert.assertNull(SketchUtils.requestManagerOrNull(imageView))
        imageView.displayAssetImage("sample.jpeg")
        Assert.assertNotNull(SketchUtils.requestManagerOrNull(imageView))
    }

    @Test
    fun testFindLastSketchDrawable() {
        val bitmap = Bitmap.createBitmap(100, 200, RGB_565)
        val resources = InstrumentationRegistry.getInstrumentation().context.resources
        val sketchDrawable = SketchBitmapDrawable(
            imageUri = "uri",
            requestKey = "key",
            requestCacheKey = "cacheKey",
            imageInfo = ImageInfo(bitmap.width, bitmap.height, "image/jpeg"),
            imageExifOrientation = 0,
            dataFrom = LOCAL,
            transformedList = null,
            resources = resources,
            bitmap = bitmap
        )
        val colorDrawable = ColorDrawable(Color.BLUE)
        val colorDrawable2 = ColorDrawable(Color.GREEN)

        Assert.assertSame(
            sketchDrawable,
            sketchDrawable.findLastSketchDrawable()
        )
        Assert.assertSame(
            sketchDrawable,
            CrossfadeDrawable(
                colorDrawable,
                CrossfadeDrawable(colorDrawable2, sketchDrawable)
            ).findLastSketchDrawable()
        )
        Assert.assertSame(
            sketchDrawable,
            TransitionDrawable(
                arrayOf(
                    colorDrawable,
                    TransitionDrawable(arrayOf(colorDrawable2, sketchDrawable))
                )
            ).findLastSketchDrawable()
        )
        Assert.assertSame(
            sketchDrawable,
            CompatDrawableWrapper(sketchDrawable).findLastSketchDrawable()
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Assert.assertSame(
                sketchDrawable,
                InternalDrawableWrapperImpl(sketchDrawable).findLastSketchDrawable()
            )
        }

        Assert.assertNull(
            colorDrawable.findLastSketchDrawable()
        )
        Assert.assertNull(
            CrossfadeDrawable(
                colorDrawable,
                CrossfadeDrawable(sketchDrawable, colorDrawable2)
            ).findLastSketchDrawable()
        )
        Assert.assertNull(
            TransitionDrawable(
                arrayOf(
                    colorDrawable,
                    TransitionDrawable(arrayOf(sketchDrawable, colorDrawable2))
                )
            ).findLastSketchDrawable()
        )
        Assert.assertNull(
            CompatDrawableWrapper(colorDrawable).findLastSketchDrawable()
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Assert.assertNull(
                InternalDrawableWrapperImpl(colorDrawable).findLastSketchDrawable()
            )
        }
    }

    @Test
    fun testForeachSketchCountDrawable() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val sketch = context.sketch
        val bitmap = Bitmap.createBitmap(100, 200, RGB_565)
        val countBitmap = CountBitmap(
            bitmap,
            imageUri = "uri",
            requestKey = "key",
            requestCacheKey = "cacheKey",
            imageInfo = ImageInfo(bitmap.width, bitmap.height, "image/jpeg"),
            imageExifOrientation = 0,
            transformedList = null,
            logger = sketch.logger,
            bitmapPool = sketch.bitmapPool,
        )
        val resources = context.resources
        val countDrawable = SketchCountBitmapDrawable(
            countBitmap = countBitmap,
            dataFrom = LOCAL,
            resources = resources,
        )
        val colorDrawable = ColorDrawable(Color.BLUE)
        val colorDrawable2 = ColorDrawable(Color.GREEN)

        countDrawable.let { drawable ->
            mutableListOf<String>().also { list ->
                drawable.foreachSketchCountDrawable { list.add(it::class.java.simpleName) }
            }
        }.apply {
            Assert.assertEquals(listOf("SketchCountBitmapDrawable"), this)
        }

        CrossfadeDrawable(
            countDrawable,
            CrossfadeDrawable(colorDrawable2, countDrawable)
        ).let { drawable ->
            mutableListOf<String>().also { list ->
                drawable.foreachSketchCountDrawable { list.add(it::class.java.simpleName) }
            }
        }.apply {
            Assert.assertEquals(
                listOf("SketchCountBitmapDrawable", "SketchCountBitmapDrawable"),
                this
            )
        }

        CrossfadeDrawable(
            countDrawable,
            CrossfadeDrawable(countDrawable, colorDrawable2)
        ).let { drawable ->
            mutableListOf<String>().also { list ->
                drawable.foreachSketchCountDrawable { list.add(it::class.java.simpleName) }
            }
        }.apply {
            Assert.assertEquals(
                listOf("SketchCountBitmapDrawable", "SketchCountBitmapDrawable"),
                this
            )
        }

        TransitionDrawable(
            arrayOf(
                countDrawable,
                TransitionDrawable(arrayOf(colorDrawable2, countDrawable))
            )
        ).let { drawable ->
            mutableListOf<String>().also { list ->
                drawable.foreachSketchCountDrawable { list.add(it::class.java.simpleName) }
            }
        }.apply {
            Assert.assertEquals(
                listOf("SketchCountBitmapDrawable", "SketchCountBitmapDrawable"),
                this
            )
        }

        TransitionDrawable(
            arrayOf(
                countDrawable,
                TransitionDrawable(arrayOf(countDrawable, colorDrawable2))
            )
        ).let { drawable ->
            mutableListOf<String>().also { list ->
                drawable.foreachSketchCountDrawable { list.add(it::class.java.simpleName) }
            }
        }.apply {
            Assert.assertEquals(
                listOf("SketchCountBitmapDrawable", "SketchCountBitmapDrawable"),
                this
            )
        }

        CompatDrawableWrapper(countDrawable).let { drawable ->
            mutableListOf<String>().also { list ->
                drawable.foreachSketchCountDrawable { list.add(it::class.java.simpleName) }
            }
        }.apply {
            Assert.assertEquals(listOf("SketchCountBitmapDrawable"), this)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            InternalDrawableWrapperImpl(countDrawable).let { drawable ->
                mutableListOf<String>().also { list ->
                    drawable.foreachSketchCountDrawable { list.add(it::class.java.simpleName) }
                }
            }.apply {
                Assert.assertEquals(listOf("SketchCountBitmapDrawable"), this)
            }
        }


        colorDrawable.let { drawable ->
            mutableListOf<String>().also { list ->
                drawable.foreachSketchCountDrawable { list.add(it::class.java.simpleName) }
            }
        }.apply {
            Assert.assertEquals(listOf<String>(), this)
        }

        CrossfadeDrawable(
            colorDrawable,
            CrossfadeDrawable(colorDrawable2, colorDrawable)
        ).let { drawable ->
            mutableListOf<String>().also { list ->
                drawable.foreachSketchCountDrawable { list.add(it::class.java.simpleName) }
            }
        }.apply {
            Assert.assertEquals(listOf<String>(), this)
        }

        TransitionDrawable(
            arrayOf(
                colorDrawable,
                TransitionDrawable(arrayOf(colorDrawable2, colorDrawable))
            )
        ).let { drawable ->
            mutableListOf<String>().also { list ->
                drawable.foreachSketchCountDrawable { list.add(it::class.java.simpleName) }
            }
        }.apply {
            Assert.assertEquals(listOf<String>(), this)
        }

        CompatDrawableWrapper(colorDrawable).let { drawable ->
            mutableListOf<String>().also { list ->
                drawable.foreachSketchCountDrawable { list.add(it::class.java.simpleName) }
            }
        }.apply {
            Assert.assertEquals(listOf<String>(), this)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            InternalDrawableWrapperImpl(colorDrawable).let { drawable ->
                mutableListOf<String>().also { list ->
                    drawable.foreachSketchCountDrawable { list.add(it::class.java.simpleName) }
                }
            }.apply {
                Assert.assertEquals(listOf<String>(), this)
            }
        }
    }
}