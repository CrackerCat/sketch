/*
 * Copyright 2011, 2012 Chris Banes.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.panpf.sketch.zoom.internal

import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener
import android.view.VelocityTracker
import android.view.ViewConfiguration
import java.lang.Float.isInfinite
import java.lang.Float.isNaN
import kotlin.math.abs
import kotlin.math.max

class ScaleDragGestureDetector(context: Context, val onGestureListener: OnGestureListener) {

    companion object {
        private const val INVALID_POINTER_ID = -1
    }

    private val touchSlop: Float
    private val minimumVelocity: Float
    private val scaleDetector: ScaleGestureDetector

    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f
    private var velocityTracker: VelocityTracker? = null
    private var activePointerId: Int = INVALID_POINTER_ID
    private var activePointerIndex: Int = 0
    private var _isHorDragging = false
    private var _isVerDragging = false
    private var multiPoint = false

    var onActionListener: OnActionListener? = null
    val isDragging: Boolean
        get() = _isHorDragging || _isVerDragging
    val isScaling: Boolean
        get() = scaleDetector.isInProgress

    init {
        val configuration = ViewConfiguration.get(context)
        minimumVelocity = configuration.scaledMinimumFlingVelocity.toFloat()
        touchSlop = configuration.scaledTouchSlop.toFloat()
        scaleDetector = ScaleGestureDetector(context, object : OnScaleGestureListener {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor =
                    detector.scaleFactor.takeIf { !isNaN(it) && !isInfinite(it) } ?: return false
                if (scaleFactor >= 0) {
                    onGestureListener.onScale(scaleFactor, detector.focusX, detector.focusY)
                }
                return true
            }

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean =
                onGestureListener.onScaleBegin()

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                onGestureListener.onScaleEnd()
            }
        })
    }

    fun onTouchEvent(ev: MotionEvent): Boolean {
        try {
            scaleDetector.onTouchEvent(ev)
            processTouchEvent(ev)
        } catch (e: IllegalArgumentException) {
            // Fix for support lib bug, happening when onDestroy is
        }
        return true
    }

    private fun processTouchEvent(ev: MotionEvent) {
        when (ev.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = ev.getPointerId(0)
                velocityTracker = VelocityTracker.obtain()
                velocityTracker?.addMovement(ev)
                lastTouchX = getActiveX(ev)
                lastTouchY = getActiveY(ev)
                _isHorDragging = false
                _isVerDragging = false
                multiPoint = false
                onActionListener?.onActionDown(ev)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                multiPoint = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!multiPoint) {
                    val x = getActiveX(ev)
                    val y = getActiveY(ev)
                    val dx = x - lastTouchX
                    val dy = y - lastTouchY
                    if (!_isHorDragging) {
                        _isHorDragging = abs(dx) >= touchSlop
                    }
                    if (!_isVerDragging) {
                        _isVerDragging = abs(dy) >= touchSlop
                    }
                    if (_isHorDragging || _isVerDragging) {
                        onGestureListener.onDrag(
                            if (_isHorDragging) dx else 0f,
                            if (_isVerDragging) dy else 0f
                        )
                        if (_isHorDragging) {
                            lastTouchX = x
                        }
                        if (_isVerDragging) {
                            lastTouchY = y
                        }
                        velocityTracker?.addMovement(ev)
                    }
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                activePointerId = INVALID_POINTER_ID

                // Recycle Velocity Tracker
                velocityTracker?.recycle()
                velocityTracker = null

                onActionListener?.onActionCancel(ev)
            }
            MotionEvent.ACTION_UP -> {
                activePointerId = INVALID_POINTER_ID
                if (_isHorDragging || _isVerDragging) {
                    val velocityTracker = velocityTracker
                    if (velocityTracker != null) {
                        lastTouchX = getActiveX(ev)
                        lastTouchY = getActiveY(ev)

                        // Compute velocity within the last 1000ms
                        velocityTracker.addMovement(ev)
                        velocityTracker.computeCurrentVelocity(1000)
                        val vX = velocityTracker.xVelocity
                        val vY = velocityTracker.yVelocity

                        // If the velocity is greater than minVelocity, call
                        // listener
                        if (max(abs(vX), abs(vY)) >= minimumVelocity) {
                            onGestureListener.onFling(lastTouchX, lastTouchY, -vX, -vY)
                        }
                    }
                }

                // Recycle Velocity Tracker
                velocityTracker?.recycle()
                velocityTracker = null

                onActionListener?.onActionUp(ev)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                multiPoint = false
                // Ignore deprecation, ACTION_POINTER_ID_MASK and
                // ACTION_POINTER_ID_SHIFT has same value and are deprecated
                // You can have either deprecation or lint target api warning
                val pointerIndex = getPointerIndex(ev.action)
                val pointerId = ev.getPointerId(pointerIndex)
                if (pointerId == activePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    activePointerId = ev.getPointerId(newPointerIndex)
                    lastTouchX = ev.getX(newPointerIndex)
                    lastTouchY = ev.getY(newPointerIndex)
                }
            }
        }

        activePointerIndex =
            ev.findPointerIndex(if (activePointerId != INVALID_POINTER_ID) activePointerId else 0)
    }

    private fun getActiveX(ev: MotionEvent): Float = try {
        ev.getX(activePointerIndex)
    } catch (e: Exception) {
        ev.x
    }

    private fun getActiveY(ev: MotionEvent): Float = try {
        ev.getY(activePointerIndex)
    } catch (e: Exception) {
        ev.y
    }

    interface OnActionListener {
        fun onActionDown(ev: MotionEvent)
        fun onActionUp(ev: MotionEvent)
        fun onActionCancel(ev: MotionEvent)
    }

    interface OnGestureListener {
        fun onDrag(dx: Float, dy: Float)
        fun onFling(startX: Float, startY: Float, velocityX: Float, velocityY: Float)
        fun onScale(scaleFactor: Float, focusX: Float, focusY: Float)
        fun onScaleBegin(): Boolean
        fun onScaleEnd()
    }
}