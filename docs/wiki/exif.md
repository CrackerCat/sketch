# Exif

Sketch 默认支持根据图片的 Exif 信息恢复图片的方向，你还可以通过 [ImageRequest] 和 [ImageOptions] 提供的 ignoreExifOrientation
属性禁用此功能，如下：

```kotlin
imageView.displayImage("https://www.sample.com/image.jpg") {
    ignoreExifOrientation()
}
```

[ImageRequest]: ../../sketch/src/main/java/com/github/panpf/sketch/request/ImageRequest.kt

[ImageOptions]: ../../sketch/src/main/java/com/github/panpf/sketch/request/ImageOptions.kt