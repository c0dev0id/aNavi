package dev.anavi.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout

class SearchRing(context: Context) : View(context) {

    private var icon: Drawable? = null
    private var leftSpoke: Spoke? = null
    private var rightSpoke: Spoke? = null
    var extended = false
        private set

    private val density = context.resources.displayMetrics.density
    private val sizePx = (UiMetrics.SEARCH_RING_SIZE * density + 0.5f).toInt()
    private val padPx = (UiMetrics.ICON_PAD * density + 0.5f).toInt()

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

    fun setSpokes(left: Spoke, right: Spoke) {
        leftSpoke = left
        rightSpoke = right
    }

    fun toggle() {
        if (extended) retract() else extend()
    }

    fun extend() {
        if (extended) return
        extended = true
        val parent = parent as? FrameLayout ?: return
        val cx = (left + width / 2).toInt()
        val cy = (top + height / 2).toInt()

        leftSpoke?.attachTo(parent, cx, cy)
        rightSpoke?.attachTo(parent, cx, cy)
        leftSpoke?.extend()
        rightSpoke?.extend()
    }

    fun retract() {
        if (!extended) return
        extended = false
        val parent = parent as? FrameLayout ?: return
        leftSpoke?.retract { leftSpoke?.detach(parent) }
        rightSpoke?.retract { rightSpoke?.detach(parent) }
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
        val cx = width / 2f
        val cy = height / 2f
        val radius = cx

        canvas.drawCircle(cx, cy, radius, bgPaint)
        if (isPressed) {
            canvas.drawCircle(cx, cy, radius, pressPaint)
        }
        icon?.let { d ->
            d.setBounds(padPx, padPx, width - padPx, height - padPx)
            d.setTint(UiColors.ICON_FG)
            d.draw(canvas)
        }
    }
}
