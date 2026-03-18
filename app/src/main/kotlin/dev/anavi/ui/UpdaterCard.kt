package dev.anavi.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View

class UpdaterCard(context: Context) : View(context) {

    var text: String = ""
        set(value) { field = value; invalidate() }

    private val density = context.resources.displayMetrics.density
    private val padHPx = UiMetrics.CARD_PAD_H * density
    private val padVPx = UiMetrics.CARD_PAD_V * density
    private val cornerPx = UiMetrics.CARD_CORNER_R * density

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = UiColors.CARD_BG
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = UiColors.CARD_TEXT
        textSize = UiMetrics.CARD_TEXT_SIZE * density
        typeface = Typeface.MONOSPACE
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = (textPaint.measureText(text) + 2 * padHPx + 0.5f).toInt()
        val h = (textPaint.textSize + 2 * padVPx + 0.5f).toInt()
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRoundRect(0f, 0f, w, h, cornerPx, cornerPx, bgPaint)
        val textY = h / 2 - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(text, padHPx, textY, textPaint)
    }
}
