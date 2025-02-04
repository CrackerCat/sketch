package com.github.panpf.sketch.transition

import androidx.annotation.MainThread
import com.github.panpf.sketch.request.DisplayResult
import com.github.panpf.sketch.target.Target

/**
 * A class to animate between a [Target]'s current drawable and the result of an image request.
 *
 * NOTE: A [Target] must implement [TransitionTarget] to support applying [Transition]s.
 * If the [Target] does not implement [TransitionTarget], any [Transition]s will be ignored.
 */
fun interface Transition {

    /**
     * Start the transition animation.
     *
     * Implementations are responsible for calling the correct [Target] lifecycle callback.
     * See [CrossfadeTransition] for an example.
     */
    @MainThread
    fun transition()

    fun interface Factory {

        fun create(target: TransitionTarget, result: DisplayResult, fitScale: Boolean): Transition?
    }
}