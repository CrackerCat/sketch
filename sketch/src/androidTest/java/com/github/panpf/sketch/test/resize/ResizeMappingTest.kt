package com.github.panpf.sketch.test.resize

import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.panpf.sketch.resize.Precision.EXACTLY
import com.github.panpf.sketch.resize.Precision.SAME_ASPECT_RATIO
import com.github.panpf.sketch.resize.Precision.LESS_PIXELS
import com.github.panpf.sketch.resize.ResizeMapping
import com.github.panpf.sketch.resize.Scale.CENTER_CROP
import com.github.panpf.sketch.resize.Scale.END_CROP
import com.github.panpf.sketch.resize.Scale.FILL
import com.github.panpf.sketch.resize.Scale.START_CROP
import com.github.panpf.sketch.resize.calculateResizeMapping
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResizeMappingTest {

    @Test
    fun testCalculatorResizeMappingLessPixels() {
        /* resize < imageSize */
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 200, 50), Rect(0, 0, 56, 14)),
            calculateResizeMapping(200, 50, 40, 20, LESS_PIXELS, START_CROP)
        )
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 200, 50), Rect(0, 0, 56, 14)),
            calculateResizeMapping(200, 50, 20, 40, LESS_PIXELS, START_CROP)
        )
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 50, 200), Rect(0, 0, 14, 56)),
            calculateResizeMapping(50, 200, 40, 20, LESS_PIXELS, START_CROP)
        )
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 50, 200), Rect(0, 0, 14, 56)),
            calculateResizeMapping(50, 200, 20, 40, LESS_PIXELS, START_CROP)
        )

        /* resize > imageSize */
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 200, 50), Rect(0, 0, 200, 50)),
            calculateResizeMapping(200, 50, 100, 150, LESS_PIXELS, START_CROP)
        )
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 200, 50), Rect(0, 0, 200, 50)),
            calculateResizeMapping(200, 50, 150, 100, LESS_PIXELS, START_CROP)
        )
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 50, 200), Rect(0, 0, 50, 200)),
            calculateResizeMapping(50, 200, 100, 150, LESS_PIXELS, START_CROP)
        )
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 50, 200), Rect(0, 0, 50, 200)),
            calculateResizeMapping(50, 200, 150, 100, LESS_PIXELS, START_CROP)
        )
    }

    @Test
    fun testCalculatorResizeMappingKeepAspectRatio() {
        /* resize < imageSize */
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 100, 50), Rect(0, 0, 40, 20)),
            calculateResizeMapping(200, 50, 40, 20, SAME_ASPECT_RATIO, START_CROP)
        )
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 25, 50), Rect(0, 0, 20, 40)),
            calculateResizeMapping(200, 50, 20, 40, SAME_ASPECT_RATIO, START_CROP)
        )
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 50, 25), Rect(0, 0, 40, 20)),
            calculateResizeMapping(50, 200, 40, 20, SAME_ASPECT_RATIO, START_CROP)
        )
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 50, 100), Rect(0, 0, 20, 40)),
            calculateResizeMapping(50, 200, 20, 40, SAME_ASPECT_RATIO, START_CROP)
        )

        /* resize > imageSize */
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 33, 50), Rect(0, 0, 33, 50)),
            calculateResizeMapping(200, 50, 100, 150, SAME_ASPECT_RATIO, START_CROP)
        )
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 75, 50), Rect(0, 0, 75, 50)),
            calculateResizeMapping(200, 50, 150, 100, SAME_ASPECT_RATIO, START_CROP)
        )
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 50, 75), Rect(0, 0, 50, 75)),
            calculateResizeMapping(50, 200, 100, 150, SAME_ASPECT_RATIO, START_CROP)
        )
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 50, 33), Rect(0, 0, 50, 33)),
            calculateResizeMapping(50, 200, 150, 100, SAME_ASPECT_RATIO, START_CROP)
        )
    }

    @Test
    fun testCalculatorResizeMappingExactly() {
        /* resize < imageSize */
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 100, 50), Rect(0, 0, 40, 20)),
            calculateResizeMapping(200, 50, 40, 20, EXACTLY, START_CROP)
        )
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 25, 50), Rect(0, 0, 20, 40)),
            calculateResizeMapping(200, 50, 20, 40, EXACTLY, START_CROP)
        )
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 50, 25), Rect(0, 0, 40, 20)),
            calculateResizeMapping(50, 200, 40, 20, EXACTLY, START_CROP)
        )
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 50, 100), Rect(0, 0, 20, 40)),
            calculateResizeMapping(50, 200, 20, 40, EXACTLY, START_CROP)
        )

        /* resize > imageSize */
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 33, 50), Rect(0, 0, 100, 150)),
            calculateResizeMapping(200, 50, 100, 150, EXACTLY, START_CROP)
        )
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 75, 50), Rect(0, 0, 150, 100)),
            calculateResizeMapping(200, 50, 150, 100, EXACTLY, START_CROP)
        )
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 50, 75), Rect(0, 0, 100, 150)),
            calculateResizeMapping(50, 200, 100, 150, EXACTLY, START_CROP)
        )
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 50, 33), Rect(0, 0, 150, 100)),
            calculateResizeMapping(50, 200, 150, 100, EXACTLY, START_CROP)
        )
    }

    @Test
    fun testCalculatorResizeMappingStartCrop() {
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 200, 50), Rect(0, 0, 56, 14)),
            calculateResizeMapping(200, 50, 40, 20, LESS_PIXELS, START_CROP)
        )
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 100, 50), Rect(0, 0, 40, 20)),
            calculateResizeMapping(200, 50, 40, 20, SAME_ASPECT_RATIO, START_CROP)
        )
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 100, 50), Rect(0, 0, 40, 20)),
            calculateResizeMapping(200, 50, 40, 20, EXACTLY, START_CROP)
        )
    }

    @Test
    fun testCalculatorResizeMappingCenterCrop() {
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 200, 50), Rect(0, 0, 56, 14)),
            calculateResizeMapping(200, 50, 40, 20, LESS_PIXELS, CENTER_CROP)
        )
        Assert.assertEquals(
            ResizeMapping(Rect(50, 0, 150, 50), Rect(0, 0, 40, 20)),
            calculateResizeMapping(200, 50, 40, 20, SAME_ASPECT_RATIO, CENTER_CROP)
        )
        Assert.assertEquals(
            ResizeMapping(Rect(50, 0, 150, 50), Rect(0, 0, 40, 20)),
            calculateResizeMapping(200, 50, 40, 20, EXACTLY, CENTER_CROP)
        )
    }

    @Test
    fun testCalculatorResizeMappingEndCrop() {
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 200, 50), Rect(0, 0, 56, 14)),
            calculateResizeMapping(200, 50, 40, 20, LESS_PIXELS, END_CROP)
        )
        Assert.assertEquals(
            ResizeMapping(Rect(100, 0, 200, 50), Rect(0, 0, 40, 20)),
            calculateResizeMapping(200, 50, 40, 20, SAME_ASPECT_RATIO, END_CROP)
        )
        Assert.assertEquals(
            ResizeMapping(Rect(100, 0, 200, 50), Rect(0, 0, 40, 20)),
            calculateResizeMapping(200, 50, 40, 20, EXACTLY, END_CROP)
        )
    }

    @Test
    fun testCalculatorResizeMappingFill() {
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 200, 50), Rect(0, 0, 56, 14)),
            calculateResizeMapping(200, 50, 40, 20, LESS_PIXELS, FILL)
        )
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 200, 50), Rect(0, 0, 40, 20)),
            calculateResizeMapping(200, 50, 40, 20, SAME_ASPECT_RATIO, FILL)
        )
        Assert.assertEquals(
            ResizeMapping(Rect(0, 0, 200, 50), Rect(0, 0, 40, 20)),
            calculateResizeMapping(200, 50, 40, 20, EXACTLY, FILL)
        )
    }
}