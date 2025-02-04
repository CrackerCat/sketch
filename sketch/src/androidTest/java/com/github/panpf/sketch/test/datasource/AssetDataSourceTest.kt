package com.github.panpf.sketch.test.datasource

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.panpf.sketch.datasource.AssetDataSource
import com.github.panpf.sketch.datasource.DataFrom
import com.github.panpf.sketch.fetch.newAssetUri
import com.github.panpf.sketch.request.LoadRequest
import com.github.panpf.sketch.test.utils.getContextAndNewSketch
import com.github.panpf.tools4j.test.ktx.assertThrow
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileNotFoundException

@RunWith(AndroidJUnit4::class)
class AssetDataSourceTest {

    @Test
    fun testConstructor() {
        val (context, sketch) = getContextAndNewSketch()

        val request = LoadRequest(context, newAssetUri("sample.jpeg"))
        AssetDataSource(
            sketch = sketch,
            request = request,
            assetFileName = "sample.jpeg"
        ).apply {
            Assert.assertTrue(request === this.request)
            Assert.assertEquals("sample.jpeg", this.assetFileName)
            Assert.assertEquals(DataFrom.LOCAL, this.dataFrom)
        }
    }

    @Test
    fun testLength() {
        val (context, sketch) = getContextAndNewSketch()

        AssetDataSource(
            sketch = sketch,
            request = LoadRequest(context, newAssetUri("sample.jpeg")),
            assetFileName = "sample.jpeg"
        ).apply {
            Assert.assertEquals(540456, length())
        }

        assertThrow(FileNotFoundException::class) {
            AssetDataSource(
                sketch = sketch,
                request = LoadRequest(context, newAssetUri("not_found.jpeg")),
                assetFileName = "not_found.jpeg"
            ).apply {
                length()
            }
        }
    }

    @Test
    fun testNewFileDescriptor() {
        val (context, sketch) = getContextAndNewSketch()

        AssetDataSource(
            sketch = sketch,
            request = LoadRequest(context, newAssetUri("sample.jpeg")),
            assetFileName = "sample.jpeg"
        ).apply {
            Assert.assertNotNull(newFileDescriptor())
        }

        assertThrow(FileNotFoundException::class) {
            AssetDataSource(
                sketch = sketch,
                request = LoadRequest(context, newAssetUri("not_found.jpeg")),
                assetFileName = "not_found.jpeg"
            ).apply {
                newFileDescriptor()
            }
        }
    }

    @Test
    fun testNewInputStream() {
        val (context, sketch) = getContextAndNewSketch()

        AssetDataSource(
            sketch = sketch,
            request = LoadRequest(context, newAssetUri("sample.jpeg")),
            assetFileName = "sample.jpeg"
        ).apply {
            newInputStream().close()
        }

        assertThrow(FileNotFoundException::class) {
            AssetDataSource(
                sketch = sketch,
                request = LoadRequest(context, newAssetUri("not_found.jpeg")),
                assetFileName = "not_found.jpeg"
            ).apply {
                newInputStream()
            }
        }
    }

    @Test
    fun testToString() {
        val (context, sketch) = getContextAndNewSketch()

        AssetDataSource(
            sketch = sketch,
            request = LoadRequest(context, newAssetUri("sample.jpeg")),
            assetFileName = "sample.jpeg"
        ).apply {
            Assert.assertEquals(
                "AssetDataSource(assetFileName='sample.jpeg')",
                toString()
            )
        }

        AssetDataSource(
            sketch = sketch,
            request = LoadRequest(context, newAssetUri("not_found.jpeg")),
            assetFileName = "not_found.jpeg"
        ).apply {
            Assert.assertEquals("AssetDataSource(assetFileName='not_found.jpeg')", toString())
        }
    }
}