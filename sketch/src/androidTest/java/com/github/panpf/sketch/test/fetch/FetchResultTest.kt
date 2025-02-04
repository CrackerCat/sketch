package com.github.panpf.sketch.test.fetch

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.panpf.sketch.datasource.ByteArrayDataSource
import com.github.panpf.sketch.datasource.DataFrom
import com.github.panpf.sketch.datasource.FileDataSource
import com.github.panpf.sketch.fetch.FetchResult
import com.github.panpf.sketch.request.LoadRequest
import com.github.panpf.sketch.test.utils.getContextAndNewSketch
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FetchResultTest {

    @Test
    fun testFrom() {
        val (context, sketch) = getContextAndNewSketch()
        val request = LoadRequest(context, "")

        FetchResult(
            FileDataSource(sketch, request, File("/sdcard/sample.jpeg")),
            "image/jpeg"
        ).apply {
            Assert.assertEquals(DataFrom.LOCAL, dataFrom)
        }

        FetchResult(
            ByteArrayDataSource(sketch, request, DataFrom.NETWORK, byteArrayOf()),
            "image/jpeg"
        ).apply {
            Assert.assertEquals(DataFrom.NETWORK, dataFrom)
        }
    }

    @Test
    fun testToString() {
        val (context, sketch) = getContextAndNewSketch()
        val request = LoadRequest(context, "")

        FetchResult(
            FileDataSource(sketch, request, File("/sdcard/sample.jpeg")),
            "image/jpeg"
        ).apply {
            Assert.assertEquals(
                "FetchResult(source=FileDataSource(file='/sdcard/sample.jpeg'),mimeType='image/jpeg')",
                this.toString()
            )
        }

        FetchResult(
            ByteArrayDataSource(sketch, request, DataFrom.NETWORK, byteArrayOf()),
            "image/jpeg"
        ).apply {
            Assert.assertEquals(
                "FetchResult(source=ByteArrayDataSource(from=NETWORK,length=0),mimeType='image/jpeg')",
                this.toString()
            )
        }
    }
}