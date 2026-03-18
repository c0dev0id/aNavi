package dev.anavi.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator

class Ring(context: Context) : View(context) {

    var onComplete: ((x: Float, y: Float) -> Unit)? = null

    private val density = context.resources.displayMetrics.density
    private val radiusPx = UiMetrics.RING_RADIUS * density
    private val strokePx = UiMetrics.RING_STROKE * density
    private val slopPx = 12f * density
    private val holdDelayMs = 300L

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = UiColors.RING_BG
        style = Paint.Style.STROKE
        strokeWidth = strokePx
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = UiColors.RING_FILL
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        strokeCap = Paint.Cap.ROUND
    }

    private var progress = 0f
    private var ringX = 0f
    private var ringY = 0f
    private var downX = 0f
    private var downY = 0f
    private var active = false
    private var animator: ValueAnimator? = null
    private val handler = Handler(Looper.getMainLooper())

    private val holdRunnable = Runnable {
        active = true
        startFill()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                handler.postDelayed(holdRunnable, holdDelayMs)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                val dy = event.y - downY
                if (dx * dx + dy * dy > slopPx * slopPx) {
                    cancel()
                    return false
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (active && progress >= 1f) {
                    onComplete?.invoke(ringX, ringY)
                }
                cancel()
            }
        }
        return active
    }

    private fun startFill() {
        ringX = downX
        ringY = downY
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = UiMetrics.RING_FILL_MS
            interpolator = LinearInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun cancel() {
        handler.removeCallbacks(holdRunnable)
        animator?.cancel()
        active = false
        progress = 0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (!active || progress <= 0f) return

        val oval = RectF(
            ringX - radiusPx, ringY - radiusPx,
            ringX + radiusPx, ringY + radiusPx
        )

        // Background circle
        canvas.drawCircle(ringX, ringY, radiusPx, bgPaint)

        // Fill arc (clockwise from top)
        canvas.drawArc(oval, -90f, 360f * progress, false, fillPaint)
    }
}
