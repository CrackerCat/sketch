@file:Suppress("DEPRECATION")

package com.github.panpf.sketch.test.request.internal

import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ALPHA_8
import android.graphics.Bitmap.Config.ARGB_4444
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.Bitmap.Config.HARDWARE
import android.graphics.Bitmap.Config.RGBA_F16
import android.graphics.Bitmap.Config.RGB_565
import android.graphics.ColorSpace
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.panpf.sketch.cache.CachePolicy.DISABLED
import com.github.panpf.sketch.cache.CachePolicy.ENABLED
import com.github.panpf.sketch.cache.CachePolicy.READ_ONLY
import com.github.panpf.sketch.cache.CachePolicy.WRITE_ONLY
import com.github.panpf.sketch.datasource.DataFrom
import com.github.panpf.sketch.decode.BitmapConfig
import com.github.panpf.sketch.decode.internal.InSampledTransformed
import com.github.panpf.sketch.decode.internal.exifOrientationName
import com.github.panpf.sketch.decode.internal.newMemoryCacheKey
import com.github.panpf.sketch.decode.internal.newResultCacheDataKey
import com.github.panpf.sketch.decode.internal.samplingByTarget
import com.github.panpf.sketch.request.Depth.LOCAL
import com.github.panpf.sketch.request.Depth.MEMORY
import com.github.panpf.sketch.request.Depth.NETWORK
import com.github.panpf.sketch.request.DepthException
import com.github.panpf.sketch.request.GlobalLifecycle
import com.github.panpf.sketch.request.LoadRequest
import com.github.panpf.sketch.request.LoadResult
import com.github.panpf.sketch.resize.Precision.EXACTLY
import com.github.panpf.sketch.resize.Precision.LESS_PIXELS
import com.github.panpf.sketch.resize.Precision.SAME_ASPECT_RATIO
import com.github.panpf.sketch.resize.ResizeTransformed
import com.github.panpf.sketch.resize.Scale.CENTER_CROP
import com.github.panpf.sketch.resize.Scale.END_CROP
import com.github.panpf.sketch.resize.Scale.FILL
import com.github.panpf.sketch.resize.Scale.START_CROP
import com.github.panpf.sketch.test.utils.ExifOrientationTestFileHelper
import com.github.panpf.sketch.test.utils.LoadListenerSupervisor
import com.github.panpf.sketch.test.utils.LoadProgressListenerSupervisor
import com.github.panpf.sketch.test.utils.TestAssets
import com.github.panpf.sketch.test.utils.TestHttpStack
import com.github.panpf.sketch.test.utils.TestLoadTarget
import com.github.panpf.sketch.test.utils.corners
import com.github.panpf.sketch.test.utils.getContext
import com.github.panpf.sketch.test.utils.getContextAndNewSketch
import com.github.panpf.sketch.test.utils.newSketch
import com.github.panpf.sketch.test.utils.ratio
import com.github.panpf.sketch.test.utils.size
import com.github.panpf.sketch.transform.CircleCropTransformation
import com.github.panpf.sketch.transform.RotateTransformation
import com.github.panpf.sketch.transform.RoundedCornersTransformation
import com.github.panpf.sketch.transform.getCircleCropTransformed
import com.github.panpf.sketch.transform.getRotateTransformed
import com.github.panpf.sketch.transform.getRoundedCornersTransformed
import com.github.panpf.sketch.util.Size
import com.github.panpf.sketch.util.asOrNull
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoadRequestExecutorTest {

    @Test
    fun testDepth() {
        val context = getContext()
        val sketch = newSketch {
            httpStack(TestHttpStack(context))
        }
        val imageUri = TestHttpStack.testImages.first().uriString

        // default
        sketch.diskCache.clear()
        sketch.memoryCache.clear()
        LoadRequest(context, imageUri) {
            resultCachePolicy(DISABLED)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.NETWORK, dataFrom)
        }

        // NETWORK
        sketch.diskCache.clear()
        sketch.memoryCache.clear()
        LoadRequest(context, imageUri) {
            resultCachePolicy(DISABLED)
            depth(NETWORK)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.NETWORK, dataFrom)
        }

        // LOCAL
        sketch.diskCache.clear()
        sketch.memoryCache.clear()
        runBlocking {
            sketch.execute(LoadRequest(context, imageUri) {
                resultCachePolicy(DISABLED)
            })
        }
        sketch.memoryCache.clear()
        Assert.assertTrue(sketch.diskCache.exist(imageUri))
        LoadRequest(context, imageUri) {
            resultCachePolicy(DISABLED)
            depth(LOCAL)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.DISK_CACHE, dataFrom)
        }

        sketch.diskCache.clear()
        sketch.memoryCache.clear()
        LoadRequest(context, imageUri) {
            resultCachePolicy(DISABLED)
            depth(LOCAL)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Error>()!!.apply {
            Assert.assertTrue(exception is DepthException)
        }

        // MEMORY
        sketch.memoryCache.clear()
        runBlocking {
            sketch.execute(LoadRequest(context, imageUri) {
                resultCachePolicy(DISABLED)
            })
        }
        LoadRequest(context, imageUri) {
            resultCachePolicy(DISABLED)
            depth(MEMORY)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.DISK_CACHE, dataFrom)
        }

        sketch.memoryCache.clear()
        LoadRequest(context, imageUri) {
            resultCachePolicy(DISABLED)
            depth(MEMORY)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Error>().apply {
            Assert.assertNull(this)
        }
    }

    @Test
    fun testDownloadCachePolicy() {
        val context = getContext()
        val sketch = newSketch {
            httpStack(TestHttpStack(context))
        }
        val diskCache = sketch.diskCache
        val imageUri = TestHttpStack.testImages.first().uriString

        /* ENABLED */
        diskCache.clear()
        Assert.assertFalse(diskCache.exist(imageUri))
        LoadRequest(context, imageUri) {
            memoryCachePolicy(DISABLED)
            resultCachePolicy(DISABLED)
            downloadCachePolicy(ENABLED)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.NETWORK, dataFrom)
        }

        Assert.assertTrue(diskCache.exist(imageUri))
        LoadRequest(context, imageUri) {
            memoryCachePolicy(DISABLED)
            resultCachePolicy(DISABLED)
            downloadCachePolicy(ENABLED)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.DISK_CACHE, dataFrom)
        }

        /* DISABLED */
        diskCache.clear()
        Assert.assertFalse(diskCache.exist(imageUri))
        LoadRequest(context, imageUri) {
            memoryCachePolicy(DISABLED)
            resultCachePolicy(DISABLED)
            downloadCachePolicy(DISABLED)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.NETWORK, dataFrom)
        }

        Assert.assertFalse(diskCache.exist(imageUri))
        LoadRequest(context, imageUri) {
            memoryCachePolicy(DISABLED)
            resultCachePolicy(DISABLED)
            downloadCachePolicy(DISABLED)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.NETWORK, dataFrom)
        }

        /* READ_ONLY */
        diskCache.clear()
        Assert.assertFalse(diskCache.exist(imageUri))
        LoadRequest(context, imageUri) {
            memoryCachePolicy(DISABLED)
            resultCachePolicy(DISABLED)
            downloadCachePolicy(READ_ONLY)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.NETWORK, dataFrom)
        }

        Assert.assertFalse(diskCache.exist(imageUri))
        LoadRequest(context, imageUri) {
            memoryCachePolicy(DISABLED)
            resultCachePolicy(DISABLED)
            downloadCachePolicy(READ_ONLY)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.NETWORK, dataFrom)
        }

        Assert.assertFalse(diskCache.exist(imageUri))
        LoadRequest(context, imageUri) {
            memoryCachePolicy(DISABLED)
            resultCachePolicy(DISABLED)
            downloadCachePolicy(ENABLED)
        }.let {
            runBlocking { sketch.execute(it) }
        }
        Assert.assertTrue(diskCache.exist(imageUri))
        LoadRequest(context, imageUri) {
            memoryCachePolicy(DISABLED)
            resultCachePolicy(DISABLED)
            downloadCachePolicy(READ_ONLY)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.DISK_CACHE, dataFrom)
        }

        /* WRITE_ONLY */
        diskCache.clear()
        Assert.assertFalse(diskCache.exist(imageUri))
        LoadRequest(context, imageUri) {
            memoryCachePolicy(DISABLED)
            resultCachePolicy(DISABLED)
            downloadCachePolicy(WRITE_ONLY)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.NETWORK, dataFrom)
        }

        Assert.assertTrue(diskCache.exist(imageUri))
        LoadRequest(context, imageUri) {
            memoryCachePolicy(DISABLED)
            resultCachePolicy(DISABLED)
            downloadCachePolicy(WRITE_ONLY)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.NETWORK, dataFrom)
        }
    }

    @Test
    fun testBitmapConfig() {
        val context = getContext()
        val sketch = newSketch()

        LoadRequest(context, TestAssets.SAMPLE_JPEG_URI) {
            resultCachePolicy(DISABLED)
            memoryCachePolicy(DISABLED)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!
            .apply {
                Assert.assertEquals(ARGB_8888, bitmap.config)
            }

        LoadRequest(context, TestAssets.SAMPLE_JPEG_URI) {
            resultCachePolicy(DISABLED)
            memoryCachePolicy(DISABLED)
            bitmapConfig(ARGB_8888)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!
            .apply {
                Assert.assertEquals(ARGB_8888, bitmap.config)
            }

        LoadRequest(context, TestAssets.SAMPLE_JPEG_URI) {
            resultCachePolicy(DISABLED)
            memoryCachePolicy(DISABLED)
            @Suppress("DEPRECATION")
            bitmapConfig(ARGB_4444)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!
            .apply {
                if (VERSION.SDK_INT > VERSION_CODES.M) {
                    Assert.assertEquals(ARGB_8888, bitmap.config)
                } else {
                    @Suppress("DEPRECATION")
                    Assert.assertEquals(ARGB_4444, bitmap.config)
                }
            }

        LoadRequest(context, TestAssets.SAMPLE_JPEG_URI) {
            resultCachePolicy(DISABLED)
            memoryCachePolicy(DISABLED)
            bitmapConfig(ALPHA_8)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!
            .apply {
                Assert.assertEquals(ARGB_8888, bitmap.config)
            }

        LoadRequest(context, TestAssets.SAMPLE_JPEG_URI) {
            resultCachePolicy(DISABLED)
            memoryCachePolicy(DISABLED)
            bitmapConfig(RGB_565)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!
            .apply {
                Assert.assertEquals(RGB_565, bitmap.config)
            }

        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            LoadRequest(context, TestAssets.SAMPLE_JPEG_URI) {
                resultCachePolicy(DISABLED)
                memoryCachePolicy(DISABLED)
                bitmapConfig(RGBA_F16)
            }.let { runBlocking { sketch.execute(it) } }
                .asOrNull<LoadResult.Success>()!!
                .apply {
                    Assert.assertEquals(RGBA_F16, bitmap.config)
                }
        }

        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            LoadRequest(context, TestAssets.SAMPLE_JPEG_URI) {
                resultCachePolicy(DISABLED)
                memoryCachePolicy(DISABLED)
                bitmapConfig(HARDWARE)
            }.let { runBlocking { sketch.execute(it) } }
                .asOrNull<LoadResult.Success>()!!
                .apply {
                    Assert.assertEquals(HARDWARE, bitmap.config)
                }
        }

        LoadRequest(context, TestAssets.SAMPLE_JPEG_URI) {
            resultCachePolicy(DISABLED)
            memoryCachePolicy(DISABLED)
            bitmapConfig(BitmapConfig.LowQuality)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!
            .apply {
                Assert.assertEquals(RGB_565, bitmap.config)
            }
        LoadRequest(context, TestAssets.SAMPLE_PNG_URI) {
            resultCachePolicy(DISABLED)
            memoryCachePolicy(DISABLED)
            bitmapConfig(BitmapConfig.LowQuality)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!
            .apply {
                if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
                    Assert.assertEquals(ARGB_8888, bitmap.config)
                } else {
                    @Suppress("DEPRECATION")
                    Assert.assertEquals(ARGB_4444, bitmap.config)
                }
            }

        LoadRequest(context, TestAssets.SAMPLE_JPEG_URI) {
            resultCachePolicy(DISABLED)
            memoryCachePolicy(DISABLED)
            bitmapConfig(BitmapConfig.HighQuality)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!
            .apply {
                if (VERSION.SDK_INT >= VERSION_CODES.O) {
                    Assert.assertEquals(RGBA_F16, bitmap.config)
                } else {
                    @Suppress("DEPRECATION")
                    Assert.assertEquals(ARGB_8888, bitmap.config)
                }
            }
        LoadRequest(context, TestAssets.SAMPLE_PNG_URI) {
            resultCachePolicy(DISABLED)
            memoryCachePolicy(DISABLED)
            bitmapConfig(BitmapConfig.HighQuality)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!
            .apply {
                if (VERSION.SDK_INT >= VERSION_CODES.O) {
                    Assert.assertEquals(RGBA_F16, bitmap.config)
                } else {
                    @Suppress("DEPRECATION")
                    Assert.assertEquals(ARGB_8888, bitmap.config)
                }
            }
    }

    @Test
    fun testColorSpace() {
        if (VERSION.SDK_INT < VERSION_CODES.O) return

        val context = getContext()
        val sketch = newSketch()

        LoadRequest(context, TestAssets.SAMPLE_JPEG_URI) {
            resultCachePolicy(DISABLED)
            memoryCachePolicy(DISABLED)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!
            .apply {
                Assert.assertEquals(
                    ColorSpace.get(ColorSpace.Named.SRGB).name,
                    bitmap.colorSpace!!.name
                )
            }

        LoadRequest(context, TestAssets.SAMPLE_JPEG_URI) {
            resultCachePolicy(DISABLED)
            memoryCachePolicy(DISABLED)
            colorSpace(ColorSpace.get(ColorSpace.Named.ADOBE_RGB))
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!
            .apply {
                Assert.assertEquals(
                    ColorSpace.get(ColorSpace.Named.ADOBE_RGB).name,
                    bitmap.colorSpace!!.name
                )
            }

        LoadRequest(context, TestAssets.SAMPLE_JPEG_URI) {
            resultCachePolicy(DISABLED)
            memoryCachePolicy(DISABLED)
            colorSpace(ColorSpace.get(ColorSpace.Named.DISPLAY_P3))
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!
            .apply {
                Assert.assertEquals(
                    ColorSpace.get(ColorSpace.Named.DISPLAY_P3).name,
                    bitmap.colorSpace!!.name
                )
            }
    }

    @Test
    fun testPreferQualityOverSpeed() {
        val context = getContext()
        val sketch = newSketch()

        LoadRequest(context, TestAssets.SAMPLE_JPEG_URI) {
            resultCachePolicy(DISABLED)
            memoryCachePolicy(DISABLED)
        }.let { runBlocking { sketch.execute(it) } }.apply {
            Assert.assertTrue(this is LoadResult.Success)
        }

        LoadRequest(context, TestAssets.SAMPLE_JPEG_URI) {
            resultCachePolicy(DISABLED)
            memoryCachePolicy(DISABLED)
            preferQualityOverSpeed(true)
        }.let { runBlocking { sketch.execute(it) } }.apply {
            Assert.assertTrue(this is LoadResult.Success)
        }

        LoadRequest(context, TestAssets.SAMPLE_JPEG_URI) {
            resultCachePolicy(DISABLED)
            memoryCachePolicy(DISABLED)
            preferQualityOverSpeed(false)
        }.let { runBlocking { sketch.execute(it) } }.apply {
            Assert.assertTrue(this is LoadResult.Success)
        }
    }

    @Test
    fun testResize() {
        val (context, sketch) = getContextAndNewSketch()
        val imageUri = TestAssets.SAMPLE_JPEG_URI
        val imageSize = Size(1291, 1936)
        val displaySize = context.resources.displayMetrics.let {
            Size(it.widthPixels, it.heightPixels)
        }

        // default
        LoadRequest(context, imageUri) {
            resultCachePolicy(DISABLED)
            memoryCachePolicy(DISABLED)
        }
            .let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                Assert.assertEquals(imageSize, imageInfo.size)
                Assert.assertEquals(imageSize.samplingByTarget(displaySize), bitmap.size)
                Assert.assertEquals(imageInfo.size.ratio, bitmap.size.ratio)
            }

        // size: small, precision=LESS_PIXELS/SAME_ASPECT_RATIO/EXACTLY
        val smallSize1 = Size(600, 500)
        LoadRequest(context, imageUri) {
            resize(smallSize1, LESS_PIXELS)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                Assert.assertEquals(imageSize, imageInfo.size)
                Assert.assertEquals(Size(323, 484), bitmap.size)
                Assert.assertEquals(imageInfo.size.ratio, bitmap.size.ratio)
            }
        LoadRequest(context, imageUri) {
            resize(smallSize1, SAME_ASPECT_RATIO)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                Assert.assertEquals(imageSize, imageInfo.size)
                if (VERSION.SDK_INT >= VERSION_CODES.N) {
                    Assert.assertEquals(Size(323, 269), bitmap.size)
                } else {
                    Assert.assertEquals(Size(322, 268), bitmap.size)
                }
                Assert.assertEquals(smallSize1.ratio, bitmap.size.ratio)
            }
        LoadRequest(context, imageUri) {
            resize(smallSize1, EXACTLY)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                Assert.assertEquals(imageSize, imageInfo.size)
                Assert.assertEquals(smallSize1, bitmap.size)
            }

        val smallSize2 = Size(500, 600)
        LoadRequest(context, imageUri) {
            resize(smallSize2, LESS_PIXELS)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                Assert.assertEquals(imageSize, imageInfo.size)
                Assert.assertEquals(Size(323, 484), bitmap.size)
                Assert.assertEquals(imageInfo.size.ratio, bitmap.size.ratio)
            }
        LoadRequest(context, imageUri) {
            resize(smallSize2, SAME_ASPECT_RATIO)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                Assert.assertEquals(imageSize, imageInfo.size)
                if (VERSION.SDK_INT >= VERSION_CODES.N) {
                    Assert.assertEquals(Size(323, 388), bitmap.size)
                } else {
                    Assert.assertEquals(Size(322, 387), bitmap.size)
                }
                Assert.assertEquals(smallSize2.ratio, bitmap.size.ratio)
            }
        LoadRequest(context, imageUri) {
            resize(smallSize2, EXACTLY)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                Assert.assertEquals(imageSize, imageInfo.size)
                Assert.assertEquals(smallSize2, bitmap.size)
            }

        // size: same, precision=LESS_PIXELS/SAME_ASPECT_RATIO/EXACTLY
        val sameSize = Size(imageSize.width, imageSize.height)
        LoadRequest(context, imageUri) {
            resize(sameSize, LESS_PIXELS)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                Assert.assertEquals(imageSize, imageInfo.size)
                Assert.assertEquals(sameSize, bitmap.size)
            }
        LoadRequest(context, imageUri) {
            resize(sameSize, SAME_ASPECT_RATIO)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                Assert.assertEquals(imageSize, imageInfo.size)
                Assert.assertEquals(sameSize, bitmap.size)
            }
        LoadRequest(context, imageUri) {
            resize(sameSize, EXACTLY)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                Assert.assertEquals(imageSize, imageInfo.size)
                Assert.assertEquals(sameSize, bitmap.size)
            }

        // size: big, precision=LESS_PIXELS/SAME_ASPECT_RATIO/EXACTLY
        val bigSize1 = Size(2500, 2100)
        LoadRequest(context, imageUri) {
            resize(bigSize1, LESS_PIXELS)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                Assert.assertEquals(imageSize, imageInfo.size)
                Assert.assertEquals(imageSize, bitmap.size)
                Assert.assertEquals(imageInfo.size.ratio, bitmap.size.ratio)
            }
        LoadRequest(context, imageUri) {
            resize(bigSize1, SAME_ASPECT_RATIO)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                Assert.assertEquals(imageSize, imageInfo.size)
                Assert.assertEquals(Size(1291, 1084), bitmap.size)
                Assert.assertEquals(bigSize1.ratio, bitmap.size.ratio)
            }
        LoadRequest(context, imageUri) {
            resize(bigSize1, EXACTLY)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                Assert.assertEquals(imageSize, imageInfo.size)
                Assert.assertEquals(bigSize1, bitmap.size)
            }

        val bigSize2 = Size(2100, 2500)
        LoadRequest(context, imageUri) {
            resize(bigSize2, LESS_PIXELS)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                Assert.assertEquals(imageSize, imageInfo.size)
                Assert.assertEquals(imageSize, bitmap.size)
                Assert.assertEquals(imageInfo.size.ratio, bitmap.size.ratio)
            }
        LoadRequest(context, imageUri) {
            resize(bigSize2, SAME_ASPECT_RATIO)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                Assert.assertEquals(imageSize, imageInfo.size)
                Assert.assertEquals(Size(1291, 1537), bitmap.size)
                Assert.assertEquals(bigSize2.ratio, bitmap.size.ratio)
            }
        LoadRequest(context, imageUri) {
            resize(bigSize2, EXACTLY)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                Assert.assertEquals(imageSize, imageInfo.size)
                Assert.assertEquals(bigSize2, bitmap.size)
            }

        val bigSize3 = Size(800, 2500)
        LoadRequest(context, imageUri) {
            resize(bigSize3, LESS_PIXELS)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                Assert.assertEquals(imageSize, imageInfo.size)
                Assert.assertEquals(Size(646, 968), bitmap.size)
                Assert.assertEquals(imageInfo.size.ratio, bitmap.size.ratio)
            }
        LoadRequest(context, imageUri) {
            resize(bigSize3, SAME_ASPECT_RATIO)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                Assert.assertEquals(imageSize, imageInfo.size)
                Assert.assertEquals(Size(620, 1936), bitmap.size)
                Assert.assertEquals(bigSize3.ratio, bitmap.size.ratio)
            }
        LoadRequest(context, imageUri) {
            resize(bigSize3, EXACTLY)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                Assert.assertEquals(imageSize, imageInfo.size)
                Assert.assertEquals(bigSize3, bitmap.size)
            }

        val bigSize4 = Size(2500, 800)
        LoadRequest(context, imageUri) {
            resize(bigSize4, LESS_PIXELS)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                Assert.assertEquals(imageSize, imageInfo.size)
                Assert.assertEquals(Size(646, 968), bitmap.size)
                Assert.assertEquals(imageInfo.size.ratio, bitmap.size.ratio)
            }
        LoadRequest(context, imageUri) {
            resize(bigSize4, SAME_ASPECT_RATIO)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                Assert.assertEquals(imageSize, imageInfo.size)
                Assert.assertEquals(Size(1291, 413), bitmap.size)
                Assert.assertEquals(bigSize4.ratio, bitmap.size.ratio)
            }
        LoadRequest(context, imageUri) {
            resize(bigSize4, EXACTLY)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                Assert.assertEquals(imageSize, imageInfo.size)
                Assert.assertEquals(bigSize4, bitmap.size)
            }

        /* scale */
        val size = Size(600, 500)
        var sarStartCropBitmap: Bitmap?
        var sarCenterCropBitmap: Bitmap?
        var sarEndCropBitmap: Bitmap?
        var sarFillCropBitmap: Bitmap?
        LoadRequest(context, imageUri) {
            resize(size, SAME_ASPECT_RATIO, START_CROP)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                sarStartCropBitmap = bitmap
                Assert.assertEquals(imageSize, imageInfo.size)
                if (VERSION.SDK_INT >= VERSION_CODES.N) {
                    Assert.assertEquals(Size(323, 269), bitmap.size)
                } else {
                    Assert.assertEquals(Size(322, 268), bitmap.size)
                }
                Assert.assertEquals(size.ratio, bitmap.size.ratio)
            }
        LoadRequest(context, imageUri) {
            resize(size, SAME_ASPECT_RATIO, CENTER_CROP)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                sarCenterCropBitmap = bitmap
                Assert.assertEquals(imageSize, imageInfo.size)
                if (VERSION.SDK_INT >= VERSION_CODES.N) {
                    Assert.assertEquals(Size(323, 269), bitmap.size)
                } else {
                    Assert.assertEquals(Size(322, 268), bitmap.size)
                }
                Assert.assertEquals(size.ratio, bitmap.size.ratio)
            }
        LoadRequest(context, imageUri) {
            resize(size, SAME_ASPECT_RATIO, END_CROP)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                sarEndCropBitmap = bitmap
                Assert.assertEquals(imageSize, imageInfo.size)
                if (VERSION.SDK_INT >= VERSION_CODES.N) {
                    Assert.assertEquals(Size(323, 269), bitmap.size)
                } else {
                    Assert.assertEquals(Size(322, 268), bitmap.size)
                }
                Assert.assertEquals(size.ratio, bitmap.size.ratio)
            }
        LoadRequest(context, imageUri) {
            resize(size, SAME_ASPECT_RATIO, FILL)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                sarFillCropBitmap = bitmap
                Assert.assertEquals(imageSize, imageInfo.size)
                if (VERSION.SDK_INT >= VERSION_CODES.N) {
                    Assert.assertEquals(Size(323, 269), bitmap.size)
                } else {
                    Assert.assertEquals(Size(322, 268), bitmap.size)
                }
                Assert.assertEquals(size.ratio, bitmap.size.ratio)
            }
        Assert.assertNotEquals(sarStartCropBitmap!!.corners(), sarCenterCropBitmap!!.corners())
        Assert.assertNotEquals(sarStartCropBitmap!!.corners(), sarEndCropBitmap!!.corners())
        Assert.assertNotEquals(sarStartCropBitmap!!.corners(), sarFillCropBitmap!!.corners())
        Assert.assertNotEquals(sarCenterCropBitmap!!.corners(), sarEndCropBitmap!!.corners())
        Assert.assertNotEquals(sarCenterCropBitmap!!.corners(), sarFillCropBitmap!!.corners())
        Assert.assertNotEquals(sarEndCropBitmap!!.corners(), sarFillCropBitmap!!.corners())

        var exactlyStartCropBitmap: Bitmap?
        var exactlyCenterCropBitmap: Bitmap?
        var exactlyEndCropBitmap: Bitmap?
        var exactlyFillCropBitmap: Bitmap?
        LoadRequest(context, imageUri) {
            resize(size, EXACTLY, START_CROP)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                exactlyStartCropBitmap = bitmap
                Assert.assertEquals(imageSize, imageInfo.size)
                Assert.assertEquals(size, bitmap.size)
            }
        LoadRequest(context, imageUri) {
            resize(size, EXACTLY, CENTER_CROP)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                exactlyCenterCropBitmap = bitmap
                Assert.assertEquals(imageSize, imageInfo.size)
                Assert.assertEquals(size, bitmap.size)
            }
        LoadRequest(context, imageUri) {
            resize(size, EXACTLY, END_CROP)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                exactlyEndCropBitmap = bitmap
                Assert.assertEquals(imageSize, imageInfo.size)
                Assert.assertEquals(size, bitmap.size)
            }
        LoadRequest(context, imageUri) {
            resize(size, EXACTLY, FILL)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!.apply {
                exactlyFillCropBitmap = bitmap
                Assert.assertEquals(imageSize, imageInfo.size)
                Assert.assertEquals(size, bitmap.size)
            }
        Assert.assertNotEquals(
            exactlyStartCropBitmap!!.corners(),
            exactlyCenterCropBitmap!!.corners()
        )
        Assert.assertNotEquals(exactlyStartCropBitmap!!.corners(), exactlyEndCropBitmap!!.corners())
        Assert.assertNotEquals(
            exactlyStartCropBitmap!!.corners(),
            exactlyFillCropBitmap!!.corners()
        )
        Assert.assertNotEquals(
            exactlyCenterCropBitmap!!.corners(),
            exactlyEndCropBitmap!!.corners()
        )
        Assert.assertNotEquals(
            exactlyCenterCropBitmap!!.corners(),
            exactlyFillCropBitmap!!.corners()
        )
        Assert.assertNotEquals(exactlyEndCropBitmap!!.corners(), exactlyFillCropBitmap!!.corners())
    }

    @Test
    fun testTransformations() {
        val context = getContext()
        val sketch = newSketch()
        val imageUri = TestAssets.SAMPLE_JPEG_URI
        val request = LoadRequest(context, imageUri) {
            memoryCachePolicy(DISABLED)
            resultCachePolicy(DISABLED)
        }

        request.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!
            .apply {
                Assert.assertTrue(transformedList?.all {
                    it is ResizeTransformed || it is InSampledTransformed
                } != false)
            }

        request.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!
            .apply {
                Assert.assertNotEquals(listOf(0, 0, 0, 0), bitmap.corners())
                Assert.assertNull(transformedList?.getRoundedCornersTransformed())
            }
        request.newLoadRequest {
            addTransformations(RoundedCornersTransformation(30f))
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!
            .apply {
                Assert.assertEquals(listOf(0, 0, 0, 0), bitmap.corners())
                Assert.assertNotNull(transformedList?.getRoundedCornersTransformed())
            }

        request.newLoadRequest {
            resize(500, 500, LESS_PIXELS)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!
            .apply {
                Assert.assertEquals(Size(323, 484), bitmap.size)
                Assert.assertNull(transformedList?.getRotateTransformed())
            }
        request.newLoadRequest {
            resize(500, 500, LESS_PIXELS)
            addTransformations(RotateTransformation(90))
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!
            .apply {
                Assert.assertEquals(Size(484, 323), bitmap.size)
                Assert.assertNotNull(transformedList?.getRotateTransformed())
            }

        request.newLoadRequest {
            resize(500, 500, LESS_PIXELS)
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!
            .apply {
                Assert.assertEquals(Size(323, 484), bitmap.size)
                Assert.assertNotEquals(listOf(0, 0, 0, 0), bitmap.corners())
                Assert.assertNull(transformedList?.getCircleCropTransformed())
            }
        request.newLoadRequest {
            resize(500, 500, LESS_PIXELS)
            addTransformations(CircleCropTransformation())
        }.let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!
            .apply {
                Assert.assertEquals(Size(323, 323), bitmap.size)
                Assert.assertEquals(listOf(0, 0, 0, 0), bitmap.corners())
                Assert.assertNotNull(transformedList?.getCircleCropTransformed())
            }
    }

    @Test
    fun testDisallowReuseBitmap() {
        val context = getContext()
        val sketch = newSketch()
        val bitmapPool = sketch.bitmapPool
        val imageUri = TestAssets.SAMPLE_JPEG_URI
        val request = LoadRequest(context, imageUri) {
            memoryCachePolicy(DISABLED)
            resultCachePolicy(DISABLED)
            resize(500, 500, LESS_PIXELS)
        }

        bitmapPool.put(Bitmap.createBitmap(323, 484, ARGB_8888))
        Assert.assertNotNull(bitmapPool.get(323, 484, ARGB_8888))
        Assert.assertNull(bitmapPool.get(323, 484, ARGB_8888))

        bitmapPool.put(Bitmap.createBitmap(323, 484, ARGB_8888))
        request.newLoadRequest {
            disallowReuseBitmap(true)
        }.let { runBlocking { sketch.execute(it) } }
        Assert.assertNotNull(bitmapPool.get(323, 484, ARGB_8888))
        Assert.assertNull(bitmapPool.get(323, 484, ARGB_8888))

        bitmapPool.put(Bitmap.createBitmap(323, 484, ARGB_8888))
        request.newLoadRequest {
            disallowReuseBitmap(false)
        }.let { runBlocking { sketch.execute(it) } }
        if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
            Assert.assertNull(bitmapPool.get(323, 484, ARGB_8888))
        } else {
            Assert.assertNotNull(bitmapPool.get(323, 484, ARGB_8888))
        }

        bitmapPool.put(Bitmap.createBitmap(323, 484, ARGB_8888))
        request.newLoadRequest {
            disallowReuseBitmap(null)
        }.let { runBlocking { sketch.execute(it) } }
        if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
            Assert.assertNull(bitmapPool.get(323, 484, ARGB_8888))
        } else {
            Assert.assertNotNull(bitmapPool.get(323, 484, ARGB_8888))
        }

        bitmapPool.put(Bitmap.createBitmap(1291, 1936, ARGB_8888))
        request.newLoadRequest {
            resize(null)
            disallowReuseBitmap(false)
        }.let { runBlocking { sketch.execute(it) } }
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
            Assert.assertNull(bitmapPool.get(1291, 1936, ARGB_8888))
        } else {
            Assert.assertNotNull(bitmapPool.get(1291, 1936, ARGB_8888))
        }

        bitmapPool.put(Bitmap.createBitmap(1291, 1936, ARGB_8888))
        request.newLoadRequest {
            resize(null)
            disallowReuseBitmap(null)
        }.let { runBlocking { sketch.execute(it) } }
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
            Assert.assertNull(bitmapPool.get(1291, 1936, ARGB_8888))
        } else {
            Assert.assertNotNull(bitmapPool.get(1291, 1936, ARGB_8888))
        }
    }

    @Test
    fun testIgnoreExifOrientation() {
        val context = getContext()
        val sketch = newSketch()
        ExifOrientationTestFileHelper(context, "exif_origin_clock_hor.jpeg").files().forEach {
            Assert.assertNotEquals(ExifInterface.ORIENTATION_UNDEFINED, it.exifOrientation)

            LoadRequest(context, it.file.path)
                .let { runBlocking { sketch.execute(it) } }
                .asOrNull<LoadResult.Success>()!!
                .apply {
                    Assert.assertEquals(it.exifOrientation, imageExifOrientation)
                    Assert.assertEquals(Size(1500, 750), imageInfo.size)
                }

            LoadRequest(context, it.file.path) {
                ignoreExifOrientation(true)
            }.let { runBlocking { sketch.execute(it) } }
                .asOrNull<LoadResult.Success>()!!
                .apply {
                    Assert.assertEquals(ExifInterface.ORIENTATION_UNDEFINED, imageExifOrientation)
                    if (it.exifOrientation == ExifInterface.ORIENTATION_ROTATE_90
                        || it.exifOrientation == ExifInterface.ORIENTATION_ROTATE_270
                        || it.exifOrientation == ExifInterface.ORIENTATION_TRANSVERSE
                        || it.exifOrientation == ExifInterface.ORIENTATION_TRANSPOSE
                    ) {
                        Assert.assertEquals(
                            exifOrientationName(it.exifOrientation),
                            Size(750, 1500),
                            imageInfo.size
                        )
                    } else {
                        Assert.assertEquals(
                            exifOrientationName(it.exifOrientation),
                            Size(1500, 750),
                            imageInfo.size
                        )
                    }
                }
        }

        LoadRequest(context, TestAssets.SAMPLE_JPEG_URI)
            .let { runBlocking { sketch.execute(it) } }
            .asOrNull<LoadResult.Success>()!!
            .apply {
                Assert.assertEquals(ExifInterface.ORIENTATION_NORMAL, imageExifOrientation)
                Assert.assertEquals(Size(1291, 1936), imageInfo.size)
            }
    }

    @Test
    fun testResultCachePolicy() {
        val context = getContext()
        val sketch = newSketch()
        val diskCache = sketch.diskCache
        val imageUri = TestAssets.SAMPLE_JPEG_URI
        val request = LoadRequest(context, imageUri) {
            memoryCachePolicy(DISABLED)
            resize(500, 500)
        }
        val resultCacheDataKey = request.newResultCacheDataKey()

        /* ENABLED */
        diskCache.clear()
        Assert.assertFalse(diskCache.exist(resultCacheDataKey))
        request.newLoadRequest {
            resultCachePolicy(ENABLED)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.LOCAL, dataFrom)
        }

        Assert.assertTrue(diskCache.exist(resultCacheDataKey))
        request.newLoadRequest {
            resultCachePolicy(ENABLED)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.RESULT_DISK_CACHE, dataFrom)
        }

        /* DISABLED */
        diskCache.clear()
        Assert.assertFalse(diskCache.exist(resultCacheDataKey))
        request.newLoadRequest {
            resultCachePolicy(DISABLED)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.LOCAL, dataFrom)
        }

        Assert.assertFalse(diskCache.exist(resultCacheDataKey))
        request.newLoadRequest {
            resultCachePolicy(DISABLED)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.LOCAL, dataFrom)
        }

        /* READ_ONLY */
        diskCache.clear()
        Assert.assertFalse(diskCache.exist(resultCacheDataKey))
        request.newLoadRequest {
            resultCachePolicy(READ_ONLY)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.LOCAL, dataFrom)
        }

        Assert.assertFalse(diskCache.exist(resultCacheDataKey))
        request.newLoadRequest {
            resultCachePolicy(READ_ONLY)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.LOCAL, dataFrom)
        }

        Assert.assertFalse(diskCache.exist(resultCacheDataKey))
        request.newLoadRequest {
            resultCachePolicy(ENABLED)
        }.let {
            runBlocking { sketch.execute(it) }
        }
        Assert.assertTrue(diskCache.exist(resultCacheDataKey))
        request.newLoadRequest {
            resultCachePolicy(READ_ONLY)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.RESULT_DISK_CACHE, dataFrom)
        }

        /* WRITE_ONLY */
        diskCache.clear()
        Assert.assertFalse(diskCache.exist(resultCacheDataKey))
        request.newLoadRequest {
            resultCachePolicy(WRITE_ONLY)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.LOCAL, dataFrom)
        }

        Assert.assertTrue(diskCache.exist(resultCacheDataKey))
        request.newLoadRequest {
            resultCachePolicy(WRITE_ONLY)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.LOCAL, dataFrom)
        }
    }

    @Test
    fun testMemoryCachePolicy() {
        val context = getContext()
        val sketch = newSketch()
        val memoryCache = sketch.memoryCache
        val imageUri = TestAssets.SAMPLE_JPEG_URI
        val request = LoadRequest(context, imageUri) {
            resultCachePolicy(DISABLED)
            resize(500, 500)
        }
        val memoryCacheKey = request.newMemoryCacheKey()

        /* ENABLED */
        memoryCache.clear()
        Assert.assertFalse(memoryCache.exist(memoryCacheKey))
        request.newLoadRequest {
            memoryCachePolicy(ENABLED)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.LOCAL, dataFrom)
        }

        Assert.assertFalse(memoryCache.exist(memoryCacheKey))
        request.newLoadRequest {
            memoryCachePolicy(ENABLED)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.LOCAL, dataFrom)
        }

        /* DISABLED */
        memoryCache.clear()
        Assert.assertFalse(memoryCache.exist(memoryCacheKey))
        request.newLoadRequest {
            memoryCachePolicy(DISABLED)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.LOCAL, dataFrom)
        }

        Assert.assertFalse(memoryCache.exist(memoryCacheKey))
        request.newLoadRequest {
            memoryCachePolicy(DISABLED)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.LOCAL, dataFrom)
        }

        /* READ_ONLY */
        memoryCache.clear()
        Assert.assertFalse(memoryCache.exist(memoryCacheKey))
        request.newLoadRequest {
            memoryCachePolicy(READ_ONLY)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.LOCAL, dataFrom)
        }

        Assert.assertFalse(memoryCache.exist(memoryCacheKey))
        request.newLoadRequest {
            memoryCachePolicy(READ_ONLY)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.LOCAL, dataFrom)
        }

        Assert.assertFalse(memoryCache.exist(memoryCacheKey))
        request.newLoadRequest {
            memoryCachePolicy(ENABLED)
        }.let {
            runBlocking { sketch.execute(it) }
        }
        Assert.assertFalse(memoryCache.exist(memoryCacheKey))
        request.newLoadRequest {
            memoryCachePolicy(READ_ONLY)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.LOCAL, dataFrom)
        }

        /* WRITE_ONLY */
        memoryCache.clear()
        Assert.assertFalse(memoryCache.exist(memoryCacheKey))
        request.newLoadRequest {
            memoryCachePolicy(WRITE_ONLY)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.LOCAL, dataFrom)
        }

        Assert.assertFalse(memoryCache.exist(memoryCacheKey))
        request.newLoadRequest {
            memoryCachePolicy(WRITE_ONLY)
        }.let {
            runBlocking { sketch.execute(it) }
        }.asOrNull<LoadResult.Success>()!!.apply {
            Assert.assertEquals(DataFrom.LOCAL, dataFrom)
        }
    }

    @Test
    fun testListener() {
        val context = getContext()
        val sketch = newSketch()
        val imageUri = TestAssets.SAMPLE_JPEG_URI
        val errorImageUri = TestAssets.SAMPLE_JPEG_URI + ".fake"

        LoadListenerSupervisor().let { listenerSupervisor ->
            Assert.assertEquals(listOf<String>(), listenerSupervisor.callbackActionList)

            LoadRequest(context, imageUri) {
                listener(listenerSupervisor)
            }.let { request ->
                runBlocking { sketch.execute(request) }
            }
            Assert.assertEquals(
                listOf("onStart", "onSuccess"),
                listenerSupervisor.callbackActionList
            )
        }

        LoadListenerSupervisor().let { listenerSupervisor ->
            Assert.assertEquals(listOf<String>(), listenerSupervisor.callbackActionList)

            LoadRequest(context, errorImageUri) {
                listener(listenerSupervisor)
            }.let { request ->
                runBlocking { sketch.execute(request) }
            }
            Assert.assertEquals(listOf("onStart", "onError"), listenerSupervisor.callbackActionList)
        }

        var deferred: Deferred<LoadResult>? = null
        val listenerSupervisor = LoadListenerSupervisor {
            deferred?.cancel()
        }
        LoadRequest(context, TestAssets.SAMPLE_JPEG_URI) {
            memoryCachePolicy(DISABLED)
            resultCachePolicy(DISABLED)
            listener(listenerSupervisor)
        }.let { request ->
            runBlocking {
                deferred = async {
                    sketch.execute(request)
                }
                deferred?.join()
            }
        }
        Assert.assertEquals(listOf("onStart", "onCancel"), listenerSupervisor.callbackActionList)
    }

    @Test
    fun testProgressListener() {
        val context = getContext()
        val sketch = newSketch {
            httpStack(TestHttpStack(context, 20))
        }
        val testImage = TestHttpStack.testImages.first()

        LoadProgressListenerSupervisor().let { listenerSupervisor ->
            Assert.assertEquals(listOf<String>(), listenerSupervisor.callbackActionList)

            LoadRequest(context, testImage.uriString) {
                memoryCachePolicy(DISABLED)
                resultCachePolicy(DISABLED)
                downloadCachePolicy(DISABLED)
                progressListener(listenerSupervisor)
            }.let { request ->
                runBlocking { sketch.execute(request) }
            }

            Assert.assertTrue(listenerSupervisor.callbackActionList.size > 1)
            listenerSupervisor.callbackActionList.forEachIndexed { index, _ ->
                if (index > 0) {
                    Assert.assertTrue(listenerSupervisor.callbackActionList[index - 1].toLong() < listenerSupervisor.callbackActionList[index].toLong())
                }
            }
            Assert.assertEquals(
                testImage.contentLength,
                listenerSupervisor.callbackActionList.last().toLong()
            )
        }
    }

    @Test
    fun testTarget() {
        val context = getContext()
        val sketch = newSketch()

        TestLoadTarget().let { testTarget ->
            Assert.assertNull(testTarget.start)
            Assert.assertNull(testTarget.successBitmap)
            Assert.assertNull(testTarget.exception)
        }

        TestLoadTarget().let { testTarget ->
            LoadRequest(context, TestAssets.SAMPLE_JPEG_URI) {
                target(testTarget)
            }.let { request ->
                runBlocking { sketch.execute(request) }
            }
            Assert.assertNotNull(testTarget.start)
            Assert.assertNotNull(testTarget.successBitmap)
            Assert.assertNull(testTarget.exception)
        }

        TestLoadTarget().let { testTarget ->
            LoadRequest(context, TestAssets.SAMPLE_JPEG_URI + ".fake") {
                target(testTarget)
            }.let { request ->
                runBlocking { sketch.execute(request) }
            }
            Assert.assertNotNull(testTarget.start)
            Assert.assertNull(testTarget.successBitmap)
            Assert.assertNotNull(testTarget.exception)
        }

        TestLoadTarget().let { testTarget ->
            var deferred: Deferred<LoadResult>? = null
            val listenerSupervisor = LoadListenerSupervisor {
                deferred?.cancel()
            }
            LoadRequest(context, TestAssets.SAMPLE_JPEG_URI) {
                memoryCachePolicy(DISABLED)
                resultCachePolicy(DISABLED)
                listener(listenerSupervisor)
                target(testTarget)
            }.let { request ->
                runBlocking {
                    deferred = async {
                        sketch.execute(request)
                    }
                    deferred?.join()
                }
            }
            Assert.assertNotNull(testTarget.start)
            Assert.assertNull(testTarget.successBitmap)
            Assert.assertNull(testTarget.exception)
        }

        TestLoadTarget().let { testTarget ->
            var deferred: Deferred<LoadResult>? = null
            val listenerSupervisor = LoadListenerSupervisor {
                deferred?.cancel()
            }
            LoadRequest(context, TestAssets.SAMPLE_JPEG_URI + ".fake") {
                memoryCachePolicy(DISABLED)
                resultCachePolicy(DISABLED)
                listener(listenerSupervisor)
                error(android.R.drawable.btn_radio)
                target(testTarget)
            }.let { request ->
                runBlocking {
                    deferred = async {
                        sketch.execute(request)
                    }
                    deferred?.join()
                }
            }
            Assert.assertNotNull(testTarget.start)
            Assert.assertNull(testTarget.successBitmap)
            Assert.assertNull(testTarget.exception)
        }
    }

    @Test
    fun testLifecycle() {
        val context = getContext()
        val sketch = newSketch()
        val lifecycleOwner = object : LifecycleOwner {
            private var lifecycle: Lifecycle? = null
            override fun getLifecycle(): Lifecycle {
                return lifecycle ?: LifecycleRegistry(this).apply {
                    lifecycle = this
                }
            }
        }
        val myLifecycle = lifecycleOwner.lifecycle as LifecycleRegistry
        runBlocking(Dispatchers.Main) {
            myLifecycle.currentState = CREATED
        }

        LoadRequest(context, TestAssets.SAMPLE_JPEG_URI).let { request ->
            Assert.assertSame(GlobalLifecycle, request.lifecycle)
            runBlocking {
                sketch.execute(request)
            }
        }.apply {
            Assert.assertTrue(this is LoadResult.Success)
        }

        LoadRequest(context, TestAssets.SAMPLE_JPEG_URI) {
            lifecycle(myLifecycle)
        }.let { request ->
            Assert.assertSame(myLifecycle, request.lifecycle)
            runBlocking {
                val deferred = async {
                    sketch.execute(request)
                }
                delay(2000)
                if (!deferred.isCompleted) {
                    withContext(Dispatchers.Main) {
                        myLifecycle.currentState = STARTED
                    }
                }
                delay(2000)
                deferred.await()
            }
        }.apply {
            Assert.assertTrue(this is LoadResult.Success)
        }
    }
}