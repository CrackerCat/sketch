package com.github.panpf.sketch.request.internal

import android.view.View
import androidx.annotation.MainThread
import com.github.panpf.sketch.R
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

class ViewTargetRequestManager(private val view: View) : View.OnAttachStateChangeListener {

    // The disposable for the current request attached to this view.
    private var currentDisposable: ViewTargetDisposable? = null

    // A pending operation that is posting to the main thread to clear the current request.
    private var pendingClear: Job? = null

    // Only accessed from the main thread.
    private var currentRequestDelegate: ViewTargetRequestDelegate? = null
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
    internal fun getDisposable(job: Deferred<DisplayResult>): ViewTargetDisposable {
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

    /** Cancel any in progress work and detach [currentRequestDelegate] from this view. */
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

    /** Attach [requestDelegate] to this view and cancel the old request. */
    @MainThread
    internal fun setRequest(requestDelegate: ViewTargetRequestDelegate?) {
        currentRequestDelegate?.dispose()
        currentRequestDelegate = requestDelegate
    }

    @MainThread
    override fun onViewAttachedToWindow(v: View) {
        restart()
    }

    @MainThread
    override fun onViewDetachedFromWindow(v: View) {
        currentRequestDelegate?.dispose()
        currentRequestDelegate?.onViewDetachedFromWindow()
    }

    fun restart() {
        val requestDelegate = currentRequestDelegate ?: return

        // As this is called from the main thread, isRestart will
        // be cleared synchronously as part of request.restart().
        isRestart = true
        requestDelegate.restart()
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