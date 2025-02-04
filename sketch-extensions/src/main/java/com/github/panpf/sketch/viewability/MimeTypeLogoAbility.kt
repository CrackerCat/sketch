package com.github.panpf.sketch.viewability

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import com.github.panpf.sketch.util.findLastSketchDrawable

/**
 * Display the MimeType logo in the lower right corner of the View
 */
fun ViewAbilityContainer.showMimeTypeLogoWithDrawable(
    mimeTypeIconMap: Map<String, Drawable>,
    margin: Int = 0
) {
    removeMimeTypeLogo()
    addViewAbility(
        MimeTypeLogoAbility(
            mimeTypeIconMap.mapValues { MimeTypeLogo(it.value) },
            margin
        )
    )
}

/**
 * Display the MimeType logo in the lower right corner of the View
 */
fun ViewAbilityContainer.showMimeTypeLogoWithRes(
    mimeTypeIconMap: Map<String, Int>,
    margin: Int = 0
) {
    removeMimeTypeLogo()
    addViewAbility(
        MimeTypeLogoAbility(
            mimeTypeIconMap.mapValues { MimeTypeLogo(it.value) },
            margin
        )
    )
}

/**
 * Remove MimeType logo
 */
fun ViewAbilityContainer.removeMimeTypeLogo() {
    viewAbilityList
        .find { it is MimeTypeLogoAbility }
        ?.let { removeViewAbility(it) }
}

/**
 * Returns true if MimeType logo feature is enabled
 */
val ViewAbilityContainer.isShowMimeTypeLogo: Boolean
    get() = viewAbilityList.find { it is MimeTypeLogoAbility } != null


class MimeTypeLogoAbility(
    private val mimeTypeIconMap: Map<String, MimeTypeLogo>,
    private val margin: Int = 0
) : ViewAbility, AttachObserver, DrawObserver, LayoutObserver, DrawableObserver {

    override var host: Host? = null
    private var logoDrawable: Drawable? = null

    override fun onAttachedToWindow() {
        reset()
        host?.view?.invalidate()
    }

    override fun onDetachedFromWindow() {

    }

    override fun onDrawableChanged(oldDrawable: Drawable?, newDrawable: Drawable?) {
        reset()
        host?.view?.invalidate()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        reset()
        host?.view?.invalidate()
    }

    override fun onDrawBefore(canvas: Canvas) {

    }

    override fun onDraw(canvas: Canvas) {
        logoDrawable?.draw(canvas)
    }

    private fun reset() {
        logoDrawable = null
        val host = host ?: return
        val view = host.view
        val lastDrawable = host.container.getDrawable()?.findLastSketchDrawable() ?: return
        val mimeType = lastDrawable.imageInfo.mimeType
        val mimeTypeLogo = mimeTypeIconMap[mimeType] ?: return
        if (mimeTypeLogo.hiddenWhenAnimatable && lastDrawable is Animatable) return
        val logoDrawable = mimeTypeLogo.getDrawable(host.context)
        logoDrawable.setBounds(
            view.right - view.paddingRight - margin - logoDrawable.intrinsicWidth,
            view.bottom - view.paddingBottom - margin - logoDrawable.intrinsicHeight,
            view.right - view.paddingRight - margin,
            view.bottom - view.paddingBottom - margin
        )
        this.logoDrawable = logoDrawable
    }
}

class MimeTypeLogo {

    private val data: Any
    private var _drawable: Drawable? = null

    val hiddenWhenAnimatable: Boolean

    constructor(drawable: Drawable, hiddenWhenAnimatable: Boolean = false) {
        this.data = drawable
        this.hiddenWhenAnimatable = hiddenWhenAnimatable
    }

    constructor(drawableResId: Int, hiddenWhenAnimatable: Boolean = false) {
        this.data = drawableResId
        this.hiddenWhenAnimatable = hiddenWhenAnimatable
    }

    fun getDrawable(context: Context): Drawable {
        return _drawable ?: if (data is Drawable) {
            _drawable = data
            data
        } else {
            val drawableResId = data as Int
            val newDrawable = ResourcesCompat.getDrawable(context.resources, drawableResId, null)!!
            _drawable = newDrawable
            newDrawable
        }
    }
}