# Decode Interceptor

Sketch 的解码过程支持拦截器，你可以通过拦截器来拦截解码过程改变解码前后的输入和输出

Sketch 将解码分为 Drawable 和 Bitmap 两种，因此拦截也同样分为两种 [BitmapDecodeInterceptor]
和 [DrawableDecodeInterceptor]，如下：

```kotlin
class MyBitmapDecodeInterceptor : BitmapDecodeInterceptor {

    @WorkerThread
    override suspend fun intercept(
        chain: BitmapDecodeInterceptor.Chain,
    ): BitmapDecodeResult {
        val newRequest = chain.request.newRequest {
            bitmapConfig(Bitmap.Config.ARGB_4444)
        }
        return chain.proceed(newRequest)
    }
}

class MyDrawableDecodeInterceptor : DrawableDecodeInterceptor {

    @WorkerThread
    override suspend fun intercept(
        chain: DrawableDecodeInterceptor.Chain,
    ): DrawableDecodeResult {
        val newRequest = chain.request.newRequest {
            disallowAnimatedImage()
        }
        return chain.proceed(newRequest)
    }
}
```

> 1. MyBitmapDecodeInterceptor 演示了一个将所有请求的 Bitmap.Config 改为 ARGB_4444 的案例
> 2. MyDrawableDecodeInterceptor 演示了一个禁止所有请求解码动图的案例
> 3. 如果你想修改返回结果，就拦截 proceed 方法返回的结果，返回一个新的 [BitmapDecodeResult] 或 [DrawableDecodeResult] 即可
> 4. 如果想不再执行请求只需不执行 proceed 方法即可

然后在初始化 Sketch 时通过 addBitmapDecodeInterceptor() 和 addDrawableDecodeInterceptor() 方法注册即可：

```kotlin
class MyApplication : Application(), SketchFactory {

    override fun createSketch(): Sketch = Sketch.Builder(this).apply {
        components {
            addBitmapDecodeInterceptor(MyBitmapDecodeInterceptor())
            addDrawableDecodeInterceptor(MyDrawableDecodeInterceptor())
        }
    }.build()
}
```

[BitmapDecodeInterceptor]: ../../sketch/src/main/java/com/github/panpf/sketch/decode/BitmapDecodeInterceptor.kt

[DrawableDecodeInterceptor]: ../../sketch/src/main/java/com/github/panpf/sketch/decode/DrawableDecodeInterceptor.kt

[BitmapDecodeResult]: ../../sketch/src/main/java/com/github/panpf/sketch/decode/BitmapDecodeResult.kt

[DrawableDecodeResult]: ../../sketch/src/main/java/com/github/panpf/sketch/decode/DrawableDecodeResult.kt

[ImageRequest]: ../../sketch/src/main/java/com/github/panpf/sketch/request/ImageRequest.kt