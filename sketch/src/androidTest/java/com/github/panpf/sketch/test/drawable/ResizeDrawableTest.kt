package com.github.panpf.sketch.test.drawable

import android.graphics.Bitmap
import android.graphics.Bitmap.Config.RGB_565
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.panpf.sketch.datasource.DataFrom.LOCAL
import com.github.panpf.sketch.decode.ImageInfo
import com.github.panpf.sketch.drawable.SketchAnimatableDrawable
import com.github.panpf.sketch.drawable.SketchBitmapDrawable
import com.github.panpf.sketch.drawable.internal.ResizeAnimatableDrawable
import com.github.panpf.sketch.drawable.internal.ResizeDrawable
import com.github.panpf.sketch.drawable.internal.tryToResizeDrawable
import com.github.panpf.sketch.fetch.newAssetUri
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.resize.Resize
import com.github.panpf.sketch.resize.Scale.CENTER_CROP
import com.github.panpf.sketch.resize.Scale.END_CROP
import com.github.panpf.sketch.resize.Scale.FILL
import com.github.panpf.sketch.resize.Scale.START_CROP
import com.github.panpf.sketch.sketch
import com.github.panpf.sketch.test.utils.getContext
import com.github.panpf.sketch.test.utils.intrinsicSize
import com.github.panpf.sketch.util.Size
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResizeDrawableTest {

    @Test
    fun testTryToResizeDrawable() {
        val context = getContext()
        val resources = context.resources
        val sketch = context.sketch

        val imageUri = newAssetUri("sample.jpeg")
        val bitmapDrawable = BitmapDrawable(resources, Bitmap.createBitmap(100, 200, RGB_565))

        Assert.assertSame(
            bitmapDrawable,
            bitmapDrawable.tryToResizeDrawable(sketch, DisplayRequest(context, imageUri))
        )
        Assert.assertSame(
            bitmapDrawable,
            bitmapDrawable.tryToResizeDrawable(sketch, DisplayRequest(context, imageUri) {
                resizeApplyToDrawable(true)
            })
        )
        Assert.assertSame(
            bitmapDrawable,
            bitmapDrawable.tryToResizeDrawable(sketch, DisplayRequest(context, imageUri) {
                resize(500, 300)
            })
        )
        bitmapDrawable.tryToResizeDrawable(sketch, DisplayRequest(context, imageUri) {
            resizeApplyToDrawable(true)
            resize(500, 300)
        }).let { it as ResizeDrawable }.apply {
            Assert.assertNotSame(bitmapDrawable, this)
            Assert.assertSame(bitmapDrawable, wrappedDrawable)
            Assert.assertEquals(Resize(500, 300), resize)
        }

        val animDrawable = SketchAnimatableDrawable(
            imageUri = imageUri,
            requestKey = imageUri,
            requestCacheKey = imageUri,
            imageInfo = ImageInfo(100, 200, "image/jpeg"),
            imageExifOrientation = 0,
            dataFrom = LOCAL,
            transformedList = null,
            animatableDrawable = bitmapDrawable,
            animatableDrawableName = "TestDrawable",
        )
        animDrawable.tryToResizeDrawable(sketch, DisplayRequest(context, imageUri) {
            resizeApplyToDrawable(true)
            resize(500, 300)
        }).let { it as ResizeAnimatableDrawable }.apply {
            Assert.assertNotSame(animDrawable, this)
            Assert.assertSame(animDrawable, wrappedDrawable)
            Assert.assertEquals(Resize(500, 300), resize)
        }
    }

    @Test
    fun testIntrinsicSize() {
        val context = getContext()
        val resources = context.resources
        val sketch = context.sketch

        val bitmapDrawable = BitmapDrawable(resources, Bitmap.createBitmap(100, 200, RGB_565))
            .apply {
                Assert.assertEquals(Size(100, 200), intrinsicSize)
            }

        ResizeDrawable(sketch, bitmapDrawable, Resize(500, 300)).apply {
            Assert.assertEquals(Size(500, 300), intrinsicSize)
            Assert.assertEquals(Resize(500, 300), resize)
            Assert.assertSame(bitmapDrawable, wrappedDrawable)
        }
    }

    @Test
    fun testSetBounds() {
        val context = getContext()
        val resources = context.resources
        val sketch = context.sketch

        val imageUri = newAssetUri("sample.jpeg")
        val bitmapDrawable = BitmapDrawable(resources, Bitmap.createBitmap(100, 200, RGB_565))
            .apply {
                Assert.assertEquals(Size(100, 200), intrinsicSize)
            }

        ResizeDrawable(sketch, bitmapDrawable, Resize(500, 300)).apply {
            Assert.assertEquals(Rect(0, 0, 0, 0), bounds)
            Assert.assertEquals(Rect(0, 0, 0, 0), bitmapDrawable.bounds)
        }
        ResizeDrawable(sketch, bitmapDrawable, Resize(500, 300, START_CROP)).apply {
            setBounds(0, 0, 500, 300)
            Assert.assertEquals(Rect(0, 0, 500, 300), bounds)
            Assert.assertEquals(Rect(0, 0, 500, 1000), bitmapDrawable.bounds)
        }
        ResizeDrawable(sketch, bitmapDrawable, Resize(500, 300, CENTER_CROP)).apply {
            setBounds(0, 0, 500, 300)
            Assert.assertEquals(Rect(0, 0, 500, 300), bounds)
            Assert.assertEquals(Rect(0, -350, 500, 650), bitmapDrawable.bounds)
        }
        ResizeDrawable(sketch, bitmapDrawable, Resize(500, 300, END_CROP)).apply {
            setBounds(0, 0, 500, 300)
            Assert.assertEquals(Rect(0, 0, 500, 300), bounds)
            Assert.assertEquals(Rect(0, -700, 500, 300), bitmapDrawable.bounds)
        }
        ResizeDrawable(sketch, bitmapDrawable, Resize(500, 300, FILL)).apply {
            setBounds(0, 0, 500, 300)
            Assert.assertEquals(Rect(0, 0, 500, 300), bounds)
            Assert.assertEquals(Rect(0, 0, 500, 300), bitmapDrawable.bounds)
        }

        ResizeDrawable(
            sketch,
            ResizeDrawable(sketch, bitmapDrawable, Resize(0, 300)),
            Resize(500, 300, CENTER_CROP)
        ).apply {
            setBounds(0, 0, 500, 300)
            Assert.assertEquals(Rect(0, 0, 500, 300), bounds)
            Assert.assertEquals(Rect(-75, 0, 75, 300), bitmapDrawable.bounds)
        }
        ResizeDrawable(
            sketch,
            ResizeDrawable(sketch, bitmapDrawable, Resize(300, 0)),
            Resize(500, 300, CENTER_CROP)
        ).apply {
            setBounds(0, 0, 500, 300)
            Assert.assertEquals(Rect(0, 0, 500, 300), bounds)
            Assert.assertEquals(Rect(0, -300, 300, 300), bitmapDrawable.bounds)
        }
        ResizeDrawable(
            sketch,
            ResizeDrawable(sketch, bitmapDrawable, Resize(0, 0)),
            Resize(500, 300, CENTER_CROP)
        ).apply {
            setBounds(0, 0, 500, 300)
            Assert.assertEquals(Rect(0, 0, 500, 300), bounds)
            Assert.assertEquals(Rect(0, 0, 0, 0), bitmapDrawable.bounds)
        }

        val sketchDrawable = SketchBitmapDrawable(
            imageUri = imageUri,
            requestKey = imageUri,
            requestCacheKey = imageUri,
            imageInfo = ImageInfo(100, 200, "image/jpeg"),
            imageExifOrientation = 0,
            dataFrom = LOCAL,
            transformedList = null,
            resources = resources,
            bitmap = Bitmap.createBitmap(100, 200, RGB_565)
        )
        ResizeDrawable(sketch, sketchDrawable, Resize(500, 300, CENTER_CROP)).apply {
            setBounds(0, 0, 500, 300)
            Assert.assertEquals(Rect(0, 0, 500, 300), bounds)
            Assert.assertEquals(Rect(0, 0, 0, 0), bitmapDrawable.bounds)
        }
    }

    @Test
    fun testToString() {
        val context = getContext()
        val resources = context.resources
        val sketch = context.sketch

        val bitmapDrawable = BitmapDrawable(resources, Bitmap.createBitmap(100, 200, RGB_565))
            .apply {
                Assert.assertEquals(Size(100, 200), intrinsicSize)
            }

        ResizeDrawable(sketch, bitmapDrawable, Resize(500, 300)).apply {
            Assert.assertEquals("ResizeDrawable($bitmapDrawable)", toString())
        }
    }
}