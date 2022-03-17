package com.github.panpf.sketch.zoom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.ImageView.ScaleType
import com.github.panpf.sketch.util.Size
import com.github.panpf.sketch.zoom.internal.ScaleDragHelper
import com.github.panpf.sketch.zoom.internal.ScrollBarHelper
import com.github.panpf.sketch.zoom.internal.TapHelper
import kotlin.math.abs

class Zoomer constructor(
    val context: Context,
    val view: View,
    viewSize: Size,
    val imageSize: Size,
    val drawableSize: Size,
    scaleType: ScaleType,
    readModeDecider: ReadModeDecider?,
    zoomScales: ZoomScales = AdaptiveTwoLevelScales(),
) {

    companion object {
        const val MODULE = "Zoomer"
    }

    private val tapHelper = TapHelper(context, this)
    private val scaleDragHelper = ScaleDragHelper(
        context,
        this,
        onUpdateMatrix = {
            scrollBarHelper?.onMatrixChanged()
            onMatrixChangeListenerList?.forEach { listener ->
                listener.onMatrixChanged(this)
            }
        },
        onDragFling = { startX: Float, startY: Float, velocityX: Float, velocityY: Float ->
            onDragFlingListenerList?.forEach {
                it.onFling(startX, startY, velocityX, velocityY)
            }
        },
        onScaleChanged = { scaleFactor: Float, focusX: Float, focusY: Float ->
            onScaleChangeListenerList?.forEach {
                it.onScaleChanged(scaleFactor, focusX, focusY)
            }
        })
    private var scrollBarHelper: ScrollBarHelper? = ScrollBarHelper(context, this)
    private var _rotateDegrees = 0

    private var onMatrixChangeListenerList: MutableSet<OnMatrixChangeListener>? = null
    private var onRotateChangeListenerList: MutableSet<OnRotateChangeListener>? = null
    private var onDragFlingListenerList: MutableSet<OnDragFlingListener>? = null
    private var onScaleChangeListenerList: MutableSet<OnScaleChangeListener>? = null

    /** Allows the parent ViewGroup to intercept events while sliding to an edge */
    var allowParentInterceptOnEdge: Boolean = true
    var zoomInterpolator: Interpolator = AccelerateDecelerateInterpolator()
    var onViewLongPressListener: OnViewLongPressListener? = null
    var onViewTapListener: OnViewTapListener? = null
    var viewSize: Size = viewSize
        internal set(value) {
            if (field != value) {
                field = value
                reset()
            }
        }
    var scaleType: ScaleType = scaleType
        internal set(value) {
            if (field != value) {
                field = value
                reset()
            }
        }
    var readModeDecider: ReadModeDecider? = readModeDecider
        internal set(value) {
            if (field != value) {
                field = value
                reset()
            }
        }
    var zoomScales: ZoomScales = zoomScales
        internal set(value) {
            if (field != value) {
                field = value
                reset()
            }
        }
    var scrollBarEnabled: Boolean
        get() = scrollBarHelper != null
        internal set(value) {
            val enabled = scrollBarHelper != null
            if (enabled != value) {
                scrollBarHelper = if (value) {
                    ScrollBarHelper(context, this).apply { reset() }
                } else {
                    null
                }
            }
        }
    var zoomAnimationDuration: Int = 200
        internal set(value) {
            if (value > 0 && field != value) {
                field = value
            }
        }


    /**************************************** Internal ********************************************/

    init {
        reset()
    }

    private fun reset() {
        zoomScales.reset(
            context,
            viewSize,
            imageSize,
            drawableSize,
            scaleType,
            _rotateDegrees.toFloat(),
            readModeDecider,
        )
        scaleDragHelper.reset()
    }

    internal fun recycle() {
        zoomScales.clean()
        scaleDragHelper.recycle()
        onViewLongPressListener = null
        onViewTapListener = null
        onMatrixChangeListenerList = null
        onScaleChangeListenerList = null
        onRotateChangeListenerList = null
        onDragFlingListenerList = null
    }

    internal fun onDraw(canvas: Canvas) {
        scrollBarHelper?.onDraw(canvas)
    }

    internal fun onTouchEvent(event: MotionEvent): Boolean {
        val scaleAndDragConsumed = scaleDragHelper.onTouchEvent(event)
        val tapConsumed = tapHelper.onTouchEvent(event)
        return scaleAndDragConsumed || tapConsumed
    }


    /*************************************** Interaction ******************************************/

    /**
     * Locate to the location specified on the preview image. You don't have to worry about scaling and rotation
     *
     * @param x Preview the x coordinate on the diagram
     * @param y Preview the y-coordinate on the diagram
     */
    fun location(x: Float, y: Float, animate: Boolean = false) {
        scaleDragHelper.location(x, y, animate)
    }

    /**
     * Scale to the specified scale. You don't have to worry about rotation degrees
     *
     * @param focalX  Scale the x coordinate of the center point on the preview image
     * @param focalY  Scale the y coordinate of the center point on the preview image
     */
    fun zoom(scale: Float, focalX: Float, focalY: Float, animate: Boolean) {
        val finalScale = scale
            .coerceAtLeast(zoomScales.minZoomScale)
            .coerceAtMost(zoomScales.maxZoomScale)
        scaleDragHelper.zoom(finalScale, focalX, focalY, animate)
    }

    /**
     * Scale to the specified scale. You don't have to worry about rotation degrees
     */
    fun zoom(scale: Float, animate: Boolean = false) {
        zoom(scale, (view.right / 2).toFloat(), (view.bottom / 2).toFloat(), animate)
    }

    /**
     * Rotate the image to the specified degrees
     *
     * @param degrees Rotation degrees, can only be 90°, 180°, 270°, 360°
     */
    fun rotateTo(degrees: Int) {
        require(degrees % 90 != 0) { "degrees must be in multiples of 90" }
        if (_rotateDegrees == degrees) return

        var newDegrees = degrees % 360
        if (newDegrees <= 0) {
            newDegrees = 360 - newDegrees
        }
        _rotateDegrees = newDegrees
        reset()
        onRotateChangeListenerList?.forEach {
            it.onRotateChanged(this)
        }
    }

    /**
     * Rotate an degrees based on the current rotation degrees
     *
     * @param addDegrees Rotation degrees, can only be 90°, 180°, 270°, 360°
     */
    fun rotateBy(addDegrees: Int) {
        return rotateTo(_rotateDegrees + addDegrees)
    }

    /**
     * The touch points on the view are converted to the corresponding points on the drawable
     */
    fun viewTouchPointToDrawablePoint(touchX: Int, touchY: Int): Point? {
        val drawRect = RectF()
        getDrawRect(drawRect)
        if (!drawRect.contains(touchX.toFloat(), touchY.toFloat())) {
            return null
        }

        val zoomScale = zoomScale
        val drawableX = ((abs(drawRect.left) + touchX) / zoomScale).toInt()
        val drawableY = ((abs(drawRect.top) + touchY) / zoomScale).toInt()
        return Point(drawableX, drawableY)
    }


    /***************************************** Information ****************************************/

    val rotateDegrees: Int
        get() = _rotateDegrees

    fun canScrollHorizontally(direction: Int): Boolean =
        scaleDragHelper.canScrollHorizontally(direction)

    fun canScrollVertically(direction: Int): Boolean =
        scaleDragHelper.canScrollVertically(direction)

    val horScrollEdge: Int
        get() = scaleDragHelper.horScrollEdge

    val verScrollEdge: Int
        get() = scaleDragHelper.verScrollEdge

    val zoomScale: Float
        get() = scaleDragHelper.zoomScale

    val baseZoomScale: Float
        get() = scaleDragHelper.defaultZoomScale

    val supportZoomScale: Float
        get() = scaleDragHelper.supportZoomScale

    /** Zoom ratio that makes the image fully visible */
    val fullZoomScale: Float
        get() = zoomScales.fullZoomScale

    /** Gets the zoom that fills the image with the ImageView display */
    val fillZoomScale: Float
        get() = zoomScales.fillZoomScale

    /** Gets the scale that allows the image to be displayed at scale to scale */
    val originZoomScale: Float
        get() = zoomScales.originZoomScale

    val minZoomScale: Float
        get() = zoomScales.minZoomScale

    val maxZoomScale: Float
        get() = zoomScales.maxZoomScale

    val doubleClickZoomScales: FloatArray
        get() = zoomScales.zoomScales

    val isZooming: Boolean
        get() = scaleDragHelper.isZooming

    fun getDrawMatrix(matrix: Matrix) = matrix.set(scaleDragHelper.getDrawMatrix())

    fun getDrawRect(rectF: RectF) = scaleDragHelper.getDrawRect(rectF)

    /** Gets the area that the user can see on the preview (not affected by rotation) */
    fun getVisibleRect(rect: Rect) = scaleDragHelper.getVisibleRect(rect)

    fun addOnMatrixChangeListener(listener: OnMatrixChangeListener) {
        this.onMatrixChangeListenerList = (onMatrixChangeListenerList ?: LinkedHashSet()).apply {
            add(listener)
        }
    }

    fun removeOnMatrixChangeListener(listener: OnMatrixChangeListener): Boolean {
        return onMatrixChangeListenerList?.remove(listener) == true
    }

    fun addOnRotateChangeListener(listener: OnRotateChangeListener) {
        this.onRotateChangeListenerList = (onRotateChangeListenerList ?: LinkedHashSet()).apply {
            add(listener)
        }
    }

    fun removeOnRotateChangeListener(listener: OnRotateChangeListener): Boolean {
        return onRotateChangeListenerList?.remove(listener) == true
    }

    fun addOnDragFlingListener(listener: OnDragFlingListener) {
        this.onDragFlingListenerList = (onDragFlingListenerList ?: LinkedHashSet()).apply {
            add(listener)
        }
    }

    fun removeOnDragFlingListener(listener: OnDragFlingListener): Boolean {
        return onDragFlingListenerList?.remove(listener) == true
    }

    fun addOnScaleChangeListener(listener: OnScaleChangeListener) {
        this.onScaleChangeListenerList = (onScaleChangeListenerList ?: LinkedHashSet()).apply {
            add(listener)
        }
    }

    fun removeOnScaleChangeListener(listener: OnScaleChangeListener): Boolean {
        return onScaleChangeListenerList?.remove(listener) == true
    }
}