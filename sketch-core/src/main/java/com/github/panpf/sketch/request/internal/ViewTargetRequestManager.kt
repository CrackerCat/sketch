package com.github.panpf.sketch.request.internal

import android.view.View
import androidx.annotation.MainThread
import com.github.panpf.sketch.core.R
import com.github.panpf.sketch.request.DisplayResult
import com.github.panpf.sketch.request.ViewTargetDisposable
import com.github.panpf.sketch.util.getCompletedOrNull
import com.github.panpf.sketch.util.isMainThread
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class ViewTargetRequestManager(private val view: View) : View.OnAttachStateChangeListener {

    // todo ViewTarget bind RequestManager，方尺重复加载，图片错乱、自动取消、自动重新请求，监听 lifecycler
    // The disposable for the current request attached to this view.
    private var currentDisposable: ViewTargetDisposable? = null

    // A pending operation that is posting to the main thread to clear the current request.
    private var pendingClear: Job? = null

    // Only accessed from the main thread.
    private var currentRequest: ViewTargetRequestDelegate? = null
    private var isRestart = false

    /** Return 'true' if [disposable] is not attached to this view. */
    @Synchronized
    fun isDisposed(disposable: ViewTargetDisposable): Boolean {
        return disposable !== currentDisposable
    }

    /**
     * Create and return a new disposable unless this is a restarted request.
     */
    @Synchronized
    fun getDisposable(job: Deferred<DisplayResult>): ViewTargetDisposable {
        // If this is a restarted request, update the current disposable and return it.
        val disposable = currentDisposable
        if (disposable != null && isMainThread() && isRestart) {
            isRestart = false
            disposable.job = job
            return disposable
        }

        // Cancel any pending clears since they were for the previous request.
        pendingClear?.cancel()
        pendingClear = null

        // Create a new disposable as this is a new request.
        return ViewTargetDisposable(view, job).also {
            currentDisposable = it
        }
    }

    /** Cancel any in progress work and detach [currentRequest] from this view. */
    @Synchronized
    @OptIn(DelicateCoroutinesApi::class)
    fun dispose() {
        pendingClear?.cancel()
        pendingClear = GlobalScope.launch(Dispatchers.Main.immediate) {
            setRequest(null)
        }
        currentDisposable = null
    }

    /** Return the completed value of the latest job if it has completed. Else, return 'null'. */
    @Synchronized
    fun getResult(): DisplayResult? {
        return currentDisposable?.job?.getCompletedOrNull()
    }

    /** Attach [request] to this view and cancel the old request. */
    @MainThread
    fun setRequest(request: ViewTargetRequestDelegate?) {
        currentRequest?.dispose()
        currentRequest = request
    }

    @MainThread
    override fun onViewAttachedToWindow(v: View) {
        val request = currentRequest ?: return

        // As this is called from the main thread, isRestart will
        // be cleared synchronously as part of request.restart().
        isRestart = true
        request.restart()
    }

    @MainThread
    override fun onViewDetachedFromWindow(v: View) {
        currentRequest?.dispose()
    }
}

internal val View.requestManager: ViewTargetRequestManager
    get() {
        val manager = getTag(R.id.sketch_request_manager) as ViewTargetRequestManager?
        if (manager != null) {
            return manager
        }
        return synchronized(this) {
            // Check again in case coil_request_manager was just set.
            (getTag(R.id.sketch_request_manager) as ViewTargetRequestManager?)
                ?: ViewTargetRequestManager(this).apply {
                    addOnAttachStateChangeListener(this)
                    setTag(R.id.sketch_request_manager, this)
                }
        }
    }