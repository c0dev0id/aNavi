package dev.anavi.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

class Crosshair(context: Context) : View(context) {

    private val density = context.resources.displayMetrics.density
    private val armPx = UiMetrics.CROSSHAIR_ARM * density
    private val gapPx = UiMetrics.CROSSHAIR_GAP * density
    private val strokePx = UiMetrics.CROSSHAIR_STROKE * density

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = UiColors.CROSSHAIR_COLOR
        strokeWidth = strokePx
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val shadowPaint = Paint(paint).apply {
        color = 0x66_000000
        strokeWidth = strokePx + density
    }

    /**
     * Set puck distance from screen center in pixels.
     * Alpha fades: 0 when puck is at center, 1.0 when beyond fadeDistance.
     */
    fun setPuckDistance(distancePx: Float) {
        val fadeDist = UiMetrics.CROSSHAIR_FADE_DIST * density
        val a = (distancePx / fadeDist).coerceIn(0f, 1f)
        if (alpha != a) {
            alpha = a
        }
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f

        // Four arms with gap: right, left, down, up
        drawArm(canvas, cx + gapPx, cy, cx + gapPx + armPx, cy)
        drawArm(canvas, cx - gapPx, cy, cx - gapPx - armPx, cy)
        drawArm(canvas, cx, cy + gapPx, cx, cy + gapPx + armPx)
        drawArm(canvas, cx, cy - gapPx, cx, cy - gapPx - armPx)
    }

    private fun drawArm(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float) {
        canvas.drawLine(x1, y1, x2, y2, shadowPaint)
        canvas.drawLine(x1, y1, x2, y2, paint)
    }
}
