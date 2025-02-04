package com.github.panpf.sketch.test.decode.internal

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.panpf.sketch.decode.internal.BitmapDecodeException
import com.github.panpf.sketch.util.SketchException
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BitmapDecodeExceptionTest {

    @Test
    fun test() {
        Assert.assertTrue(SketchException::class.java.isAssignableFrom(BitmapDecodeException::class.java))
    }
}