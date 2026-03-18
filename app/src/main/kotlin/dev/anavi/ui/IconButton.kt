package dev.anavi.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.view.View

class IconButton(context: Context) : View(context) {

    private var icon: Drawable? = null

    private val density = context.resources.displayMetrics.density
    private val sizePx = (UiMetrics.BUTTON_SIZE * density + 0.5f).toInt()
    private val padPx = (UiMetrics.ICON_PAD * density + 0.5f).toInt()
    private val cornerPx = UiMetrics.CORNER_R * density

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = UiColors.BUTTON_BG
        style = Paint.Style.FILL
    }
    private val pressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = UiColors.PRESS_OVERLAY
        style = Paint.Style.FILL
    }

    init {
        isClickable = true
        isFocusable = true
    }

    fun setIcon(drawable: Drawable) {
        icon = drawable.mutate()
        invalidate()
    }

    fun setIcon(resId: Int) {
        setIcon(context.getDrawable(resId)!!)
    }

    override fun setPressed(pressed: Boolean) {
        val changed = isPressed != pressed
        super.setPressed(pressed)
        if (changed) invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(sizePx, sizePx)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRoundRect(0f, 0f, w, h, cornerPx, cornerPx, bgPaint)
        if (isPressed) {
            canvas.drawRoundRect(0f, 0f, w, h, cornerPx, cornerPx, pressPaint)
        }
        icon?.let { d ->
            d.setBounds(padPx, padPx, width - padPx, height - padPx)
            d.setTint(UiColors.ICON_FG)
            d.draw(canvas)
        }
    }
}
