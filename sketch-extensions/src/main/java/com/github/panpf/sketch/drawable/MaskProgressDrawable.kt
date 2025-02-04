package com.github.panpf.sketch.drawable

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import androidx.annotation.ColorInt
import com.github.panpf.sketch.util.format

/**
 * Mask Progress Drawable
 */
class MaskProgressDrawable(
    @ColorInt private val maskColor: Int = DEFAULT_MASK_COLOR
) : ProgressDrawable() {

    companion object {
        const val DEFAULT_MASK_COLOR = 0x22000000
    }

    private val paint = Paint().apply {
        color = maskColor
        isAntiAlias = true
    }

    private var _progress: Float = 0f
        set(value) {
            field = value
            invalidateSelf()
            if (value >= 1f) {
                onProgressEnd?.invoke()
            }
        }

    private var progressAnimator: ValueAnimator? = null

    override var progress: Float
        get() = _progress
        set(value) {
            val newValue = value.format(1).coerceAtLeast(0f).coerceAtMost(1f)
            if (newValue != _progress) {
                if (_progress == 0f && newValue == 1f) {
                    // Here is the loading of the local image, no loading progress, quickly complete
                    _progress = newValue
                } else if (newValue > _progress) {
                    animationUpdateProgress(newValue)
                } else {
                    // If newValue is less than _progress, you can reset it quickly
                    _progress = newValue
                }
            }
        }
    override var onProgressEnd: (() -> Unit)? = null

    override fun draw(canvas: Canvas) {
        val currentProgress = _progress.takeIf { it > 0f } ?: return
        val bounds = bounds.takeIf { !it.isEmpty } ?: return
        val saveCount = canvas.save()

        canvas.drawRect(
            bounds.left.toFloat(),
            bounds.top + ((currentProgress * bounds.height())),
            bounds.right.toFloat(),
            bounds.bottom.toFloat(),
            paint
        )

        canvas.restoreToCount(saveCount)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private fun animationUpdateProgress(newProgress: Float) {
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofFloat(_progress, newProgress).apply {
            addUpdateListener {
                if (isActive()) {
                    _progress = animatedValue as Float
                } else {
                    it?.cancel()
                }
            }
            duration = 300
        }
        progressAnimator?.start()
    }

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        val changed = super.setVisible(visible, restart)
        if (changed && !visible) {
            progressAnimator?.cancel()
        }
        return changed
    }

    override fun getIntrinsicWidth(): Int = -1

    override fun getIntrinsicHeight(): Int = -1
}