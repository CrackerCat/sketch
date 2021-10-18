/*
 * Copyright (C) 2019 panpf <panpfpanpf@outlook.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.panpf.sketch.state;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.format.Formatter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.panpf.sketch.Configuration;
import com.github.panpf.sketch.SLog;
import com.github.panpf.sketch.Sketch;
import com.github.panpf.sketch.SketchView;
import com.github.panpf.sketch.cache.BitmapPool;
import com.github.panpf.sketch.cache.BitmapPoolUtils;
import com.github.panpf.sketch.cache.MemoryCache;
import com.github.panpf.sketch.decode.ImageAttrs;
import com.github.panpf.sketch.decode.ProcessImageException;
import com.github.panpf.sketch.drawable.SketchBitmapDrawable;
import com.github.panpf.sketch.drawable.SketchRefBitmap;
import com.github.panpf.sketch.drawable.SketchShapeBitmapDrawable;
import com.github.panpf.sketch.process.ImageProcessor;
import com.github.panpf.sketch.request.DisplayOptions;
import com.github.panpf.sketch.request.ImageFrom;
import com.github.panpf.sketch.request.Resize;
import com.github.panpf.sketch.request.ShapeSize;
import com.github.panpf.sketch.shaper.ImageShaper;
import com.github.panpf.sketch.uri.DrawableUriModel;
import com.github.panpf.sketch.uri.UriModel;
import com.github.panpf.sketch.util.SketchUtils;

/**
 * 可以使用 Options 中配置的 {@link ImageProcessor} 和 {@link Resize} 修改原图片，同样支持 {@link ShapeSize} 和 {@link ImageShaper}
 */
// TODO: 2017/10/30 重命名为 MakerDrawableStateImage 并像 DrawableStateImage 一样支持 drawable
public class MakerStateImage implements StateImage {
    private int resId;

    public MakerStateImage(int resId) {
        this.resId = resId;
    }

    public int getResId() {
        return resId;
    }

    @Nullable
    @Override
    public Drawable getDrawable(@NonNull Context context, @NonNull SketchView sketchView, @NonNull DisplayOptions displayOptions) {
        Drawable drawable = makeDrawable(Sketch.with(context), displayOptions);

        ShapeSize shapeSize = displayOptions.getShapeSize();
        ImageShaper imageShaper = displayOptions.getShaper();
        if ((shapeSize != null || imageShaper != null) && drawable instanceof BitmapDrawable) {
            drawable = new SketchShapeBitmapDrawable(context, (BitmapDrawable) drawable, shapeSize, imageShaper);
        }

        return drawable;
    }

    @Nullable
    private Drawable makeDrawable(@NonNull Sketch sketch, @NonNull DisplayOptions options) {
        Configuration configuration = sketch.getConfiguration();

        ImageProcessor processor = options.getProcessor();
        Resize resize = options.getResize();
        BitmapPool bitmapPool = configuration.getBitmapPool();

        // 不需要处理的时候直接取出图片返回
        if (processor == null && resize == null) {
            return configuration.getContext().getResources().getDrawable(resId);
        }

        // 从内存缓存中取
        String imageUri = DrawableUriModel.makeUri(resId);
        UriModel uriModel = UriModel.match(sketch, imageUri);
        String memoryCacheKey = null;
        if (uriModel != null) {
            memoryCacheKey = SketchUtils.makeRequestKey(imageUri, uriModel, options.makeStateImageKey());
        }
        MemoryCache memoryCache = configuration.getMemoryCache();
        SketchRefBitmap cachedRefBitmap = null;
        if (memoryCacheKey != null) {
            cachedRefBitmap = memoryCache.get(memoryCacheKey);
        }
        if (cachedRefBitmap != null) {
            if (!cachedRefBitmap.isRecycled()) {
                return new SketchBitmapDrawable(cachedRefBitmap, ImageFrom.MEMORY_CACHE);
            } else {
                memoryCache.remove(memoryCacheKey);
            }
        }

        // 读取图片
        Bitmap bitmap;
        boolean allowRecycle = false;
        boolean tempLowQualityImage = configuration.isLowQualityImageEnabled() || options.isLowQualityImage();
        //noinspection deprecation
        Drawable drawable = configuration.getContext().getResources().getDrawable(resId);
        if (drawable instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        } else {
            bitmap = SketchUtils.drawableToBitmap(drawable, tempLowQualityImage, bitmapPool);
            allowRecycle = true;
        }
        if (bitmap == null || bitmap.isRecycled()) {
            return null;
        }

        // 处理图片
        //noinspection ConstantConditions
        if (processor == null && resize != null) {
            processor = sketch.getConfiguration().getResizeProcessor();
        }
        Bitmap newBitmap;
        try {
            newBitmap = processor.process(sketch, bitmap, resize, tempLowQualityImage);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            Context application = sketch.getConfiguration().getContext();
            SLog.emf("MakerStateImage", "onProcessImageError. imageUri: %s. processor: %s. " +
                            "appMemoryInfo: maxMemory=%s, freeMemory=%s, totalMemory=%s",
                    DrawableUriModel.makeUri(resId), processor.toString(),
                    Formatter.formatFileSize(application, Runtime.getRuntime().maxMemory()),
                    Formatter.formatFileSize(application, Runtime.getRuntime().freeMemory()),
                    Formatter.formatFileSize(application, Runtime.getRuntime().totalMemory()));
            sketch.getConfiguration().getCallback().onError(new ProcessImageException(e, DrawableUriModel.makeUri(resId), processor));
            if (allowRecycle) {
                BitmapPoolUtils.freeBitmapToPool(bitmap, bitmapPool);
            }
            return null;
        }

        // bitmap变化了，说明创建了一张全新的图片，那么就要回收掉旧的图片
        if (newBitmap != bitmap) {
            if (allowRecycle) {
                BitmapPoolUtils.freeBitmapToPool(bitmap, bitmapPool);
            }

            // 新图片不能用说你处理部分出现异常了，直接返回null即可
            if (newBitmap.isRecycled()) {
                return null;
            }

            bitmap = newBitmap;
            allowRecycle = true;
        }

        // 允许回收说明是创建了一张新的图片，不能回收说明还是从res中获取的BitmapDrawable可以直接使用
        if (allowRecycle) {
            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(configuration.getContext().getResources(), resId, boundsOptions);

            String uri = DrawableUriModel.makeUri(resId);
            ImageAttrs imageAttrs = new ImageAttrs(boundsOptions.outMimeType, boundsOptions.outWidth, boundsOptions.outHeight, 0);

            SketchRefBitmap newRefBitmap = new SketchRefBitmap(bitmap, memoryCacheKey, uri, imageAttrs, bitmapPool);
            memoryCache.put(memoryCacheKey, newRefBitmap);
            return new SketchBitmapDrawable(newRefBitmap, ImageFrom.LOCAL);
        } else {
            return drawable;
        }
    }
}