@file:JvmName("ViewSizeResolvers")

package com.github.panpf.sketch.resize

import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnPreDrawListener
import com.github.panpf.sketch.util.Size
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Create a [ViewSizeResolver] using the default [View] measurement implementation.
 *
 * @param view The view to measure.
 * @param subtractPadding If true, the view's padding will be subtracted from its size.
 */
@JvmOverloads
@JvmName("create")
fun <T : View> ViewSizeResolver(view: T, subtractPadding: Boolean = true): ViewSizeResolver<T> =
    RealViewSizeResolver(view, subtractPadding)

internal class RealViewSizeResolver<T : View>(
    override val view: T,
    override val subtractPadding: Boolean
) : ViewSizeResolver<T> {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is RealViewSizeResolver<*> &&
                view == other.view &&
                subtractPadding == other.subtractPadding
    }

    override fun hashCode(): Int {
        var result = view.hashCode()
        result = 31 * result + subtractPadding.hashCode()
        return result
    }
}

/**
 * A [SizeResolver] that measures the size of a [View].
 */
interface ViewSizeResolver<T : View> : SizeResolver {

    /** The [View] to measure. This field should be immutable. */
    val view: T

    /** If true, the [view]'s padding will be subtracted from its size. */
    val subtractPadding: Boolean get() = true

    override suspend fun size(): Size? {
        // Fast path: the view is already measured.
        getSize()?.let { return it }

        // Slow path: wait for the view to be measured.
        return suspendCancellableCoroutine { continuation ->
            val viewTreeObserver = view.viewTreeObserver

            val preDrawListener = object : OnPreDrawListener {
                private var isResumed = false

                override fun onPreDraw(): Boolean {
                    val size = getSize()
                    if (size != null) {
                        viewTreeObserver.removePreDrawListenerSafe(this)

                        if (!isResumed) {
                            isResumed = true
                            continuation.resume(size)
                        }
                    }
                    return true
                }
            }

            viewTreeObserver.addOnPreDrawListener(preDrawListener)

            continuation.invokeOnCancellation {
                viewTreeObserver.removePreDrawListenerSafe(preDrawListener)
            }
        }
    }

    private fun getSize(): Size? {
        val width = getWidth() ?: return null
        val height = getHeight() ?: return null
        return Size(width, height)
    }

    private fun getWidth(): Int? = getDimension(
        paramSize = view.layoutParams?.width ?: -1,
        viewSize = view.width,
        paddingSize = if (subtractPadding) view.paddingLeft + view.paddingRight else 0
    )?.let {
        if (it != -1) {
            it
        } else {
            view.resources.displayMetrics.widthPixels
        }
    }

    private fun getHeight(): Int? = getDimension(
        paramSize = view.layoutParams?.height ?: -1,
        viewSize = view.height,
        paddingSize = if (subtractPadding) view.paddingTop + view.paddingBottom else 0
    )?.let {
        if (it != -1) {
            it
        } else {
            view.resources.displayMetrics.heightPixels
        }
    }

    private fun getDimension(paramSize: Int, viewSize: Int, paddingSize: Int): Int? {
        // If the dimension is set to WRAP_CONTENT, use the original dimension of the image.
        if (paramSize == ViewGroup.LayoutParams.WRAP_CONTENT) {
            return -1
        }

        // Assume the dimension will match the value in the view's layout params.
        val insetParamSize = paramSize - paddingSize
        if (insetParamSize > 0) {
            return insetParamSize
        }

        // Fallback to the view's current dimension.
        val insetViewSize = viewSize - paddingSize
        if (insetViewSize > 0) {
            return insetViewSize
        }

        // Unable to resolve the dimension's value.
        return null
    }

    private fun ViewTreeObserver.removePreDrawListenerSafe(victim: OnPreDrawListener) {
        if (isAlive) {
            removeOnPreDrawListener(victim)
        } else {
            view.viewTreeObserver.removeOnPreDrawListener(victim)
        }
    }
}
