package com.github.panpf.sketch.viewability

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.request.DisplayResult
import com.github.panpf.sketch.request.Listener
import com.github.panpf.sketch.request.ProgressListener
import com.github.panpf.sketch.viewability.internal.RealViewAbilityManager

/**
 * ImageView base class that supports [ViewAbility]
 */
abstract class AbsAbilityImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : AppCompatImageView(context, attrs, defStyle), ViewAbilityContainer {

    private var viewAbilityManager: ViewAbilityManager? = null

    override val viewAbilityList: List<ViewAbility>
        get() = viewAbilityManager?.viewAbilityList ?: emptyList()

    init {
        viewAbilityManager = RealViewAbilityManager(this, this)
    }

    final override fun addViewAbility(viewAbility: ViewAbility) {
        viewAbilityManager?.addViewAbility(viewAbility)
    }

    override fun removeViewAbility(viewAbility: ViewAbility) {
        viewAbilityManager?.removeViewAbility(viewAbility)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewAbilityManager?.onAttachedToWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        viewAbilityManager?.onVisibilityChanged(changedView, visibility)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        viewAbilityManager?.onLayout(changed, left, top, right, bottom)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewAbilityManager?.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onDraw(canvas: Canvas) {
        viewAbilityManager?.onDrawBefore(canvas)
        super.onDraw(canvas)
        viewAbilityManager?.onDraw(canvas)
    }

    override fun onDrawForeground(canvas: Canvas) {
        viewAbilityManager?.onDrawForegroundBefore(canvas)
        super.onDrawForeground(canvas)
        viewAbilityManager?.onDrawForeground(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return viewAbilityManager?.onTouchEvent(event) == true || super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewAbilityManager?.onDetachedFromWindow()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        val oldDrawable = this.drawable
        super.setImageDrawable(drawable)
        val newDrawable = this.drawable
        if (oldDrawable !== newDrawable) {
            viewAbilityManager?.onDrawableChanged(oldDrawable, newDrawable)
        }
    }

    override fun setImageURI(uri: Uri?) {
        val oldDrawable = this.drawable
        super.setImageURI(uri)
        val newDrawable = this.drawable
        if (oldDrawable !== newDrawable) {
            viewAbilityManager?.onDrawableChanged(oldDrawable, newDrawable)
        }
    }

    final override fun setOnClickListener(l: OnClickListener?) {
        viewAbilityManager?.setOnClickListener(l)
    }

    final override fun setOnLongClickListener(l: OnLongClickListener?) {
        viewAbilityManager?.setOnLongClickListener(l)
    }

    final override fun superSetOnClickListener(listener: OnClickListener?) {
        super.setOnClickListener(listener)
        if (listener == null) {
            isClickable = false
        }
    }

    final override fun superSetOnLongClickListener(listener: OnLongClickListener?) {
        super.setOnLongClickListener(listener)
        if (listener == null) {
            isLongClickable = false
        }
    }

    final override fun superSetScaleType(scaleType: ScaleType) {
        super.setScaleType(scaleType)
    }

    final override fun superGetScaleType(): ScaleType {
        return super.getScaleType()
    }

    final override fun setScaleType(scaleType: ScaleType) {
        if (viewAbilityManager?.setScaleType(scaleType) != true) {
            super.setScaleType(scaleType)
        }
    }

    final override fun getScaleType(): ScaleType {
        return viewAbilityManager?.getScaleType() ?: super.getScaleType()
    }

    final override fun superSetImageMatrix(matrix: Matrix?) {
        super.setImageMatrix(matrix)
    }

    final override fun superGetImageMatrix(): Matrix {
        return super.getImageMatrix()
    }

    final override fun setImageMatrix(matrix: Matrix?) {
        if (viewAbilityManager?.setImageMatrix(matrix) != true) {
            super.setImageMatrix(matrix)
        }
    }

    final override fun getImageMatrix(): Matrix {
        return viewAbilityManager?.getImageMatrix() ?: super.getImageMatrix()
    }

    override fun getListener(): Listener<DisplayRequest, DisplayResult.Success, DisplayResult.Error>? {
        return viewAbilityManager?.getRequestListener()
    }

    override fun getProgressListener(): ProgressListener<DisplayRequest>? {
        return viewAbilityManager?.getRequestProgressListener()
    }
}