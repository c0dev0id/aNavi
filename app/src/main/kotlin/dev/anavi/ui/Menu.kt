package dev.anavi.ui

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout

enum class ExpandH { LEFT, RIGHT }
enum class ExpandV { UP, DOWN }

class Menu(context: Context) : View(context) {

    private var title = ""
    private var items = emptyList<MenuItem>()
    private var headerActions = emptyList<MenuItem>()
    private var expandH = ExpandH.RIGHT
    private var expandV = ExpandV.DOWN
    private var anchorX = 0f
    private var anchorY = 0f
    private var progress = 0f
    private var onDismiss: (() -> Unit)? = null
    private var dismissed = false

    private val density = context.resources.displayMetrics.density
    private val menuWidthPx = UiMetrics.MENU_WIDTH * density
    private val itemHPx = UiMetrics.MENU_ITEM_H * density
    private val headerHPx = UiMetrics.MENU_HEADER_H * density
    private val padHPx = UiMetrics.MENU_PAD_H * density
    private val padVPx = UiMetrics.MENU_PAD_V * density
    private val cornerPx = UiMetrics.CORNER_R * density
    private val textSizePx = UiMetrics.MENU_TEXT_SIZE * density
    private val headerTextSizePx = UiMetrics.MENU_HEADER_TEXT_SIZE * density
    private val iconSizePx = (20f * density).toInt()
    private val headerActionSizePx = (UiMetrics.MENU_HEADER_H * 0.6f * density).toInt()

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = UiColors.MENU_BG
        style = Paint.Style.FILL
    }
    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = UiColors.MENU_HEADER_BG
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = UiColors.MENU_TEXT
        textSize = textSizePx
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
    }
    private val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = UiColors.MENU_TEXT
        textSize = headerTextSizePx
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    private val disabledTextPaint = Paint(textPaint).apply {
        color = UiColors.MENU_TEXT_DISABLED
    }
    private val dividerPaint = Paint().apply {
        color = UiColors.MENU_DIVIDER
        strokeWidth = 1f
    }
    private val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = UiColors.MENU_TEXT
        textSize = textSizePx
        typeface = Typeface.DEFAULT
    }
    private val pressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = UiColors.PRESS_OVERLAY
        style = Paint.Style.FILL
    }

    private var pressedIndex = -1
    private var animator: ValueAnimator? = null

    private val backCallback = android.window.OnBackInvokedCallback { dismiss() }

    fun show(
        parent: FrameLayout,
        anchor: View,
        expandH: ExpandH,
        expandV: ExpandV,
        title: String,
        items: List<MenuItem>,
        headerActions: List<MenuItem> = emptyList(),
        onDismiss: (() -> Unit)? = null
    ) {
        val loc = IntArray(2)
        anchor.getLocationInWindow(loc)
        val ax = when (expandH) {
            ExpandH.RIGHT -> loc[0].toFloat()
            ExpandH.LEFT -> (loc[0] + anchor.width).toFloat()
        }
        val ay = when (expandV) {
            ExpandV.DOWN -> (loc[1] + anchor.height).toFloat()
            ExpandV.UP -> loc[1].toFloat()
        }
        showAt(parent, ax, ay, expandH, expandV, title, items, headerActions, onDismiss)
    }

    fun showAt(
        parent: FrameLayout,
        x: Float,
        y: Float,
        expandH: ExpandH,
        expandV: ExpandV,
        title: String,
        items: List<MenuItem>,
        headerActions: List<MenuItem> = emptyList(),
        onDismiss: (() -> Unit)? = null
    ) {
        this.title = title
        this.items = items
        this.headerActions = headerActions
        this.expandH = expandH
        this.expandV = expandV
        this.onDismiss = onDismiss
        dismissed = false
        anchorX = x
        anchorY = y

        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        parent.addView(this)
        registerBackHandler()
        animateIn()
    }

    fun dismiss() {
        if (dismissed) return
        dismissed = true
        unregisterBackHandler()
        animateOut {
            (parent as? FrameLayout)?.removeView(this)
            onDismiss?.invoke()
        }
    }

    private fun menuRect(): RectF {
        val totalH = headerHPx + items.size * itemHPx + 2 * padVPx
        val left = when (expandH) {
            ExpandH.RIGHT -> anchorX
            ExpandH.LEFT -> anchorX - menuWidthPx
        }
        val top = when (expandV) {
            ExpandV.DOWN -> anchorY
            ExpandV.UP -> anchorY - totalH
        }
        return RectF(left, top, left + menuWidthPx, top + totalH)
    }

    override fun onDraw(canvas: Canvas) {
        if (progress <= 0f) return

        val rect = menuRect()
        val save = canvas.save()

        // Clip to animated height
        val clipH = rect.height() * progress
        when (expandV) {
            ExpandV.DOWN -> canvas.clipRect(rect.left, rect.top, rect.right, rect.top + clipH)
            ExpandV.UP -> canvas.clipRect(rect.left, rect.bottom - clipH, rect.right, rect.bottom)
        }

        canvas.saveLayerAlpha(rect, (255 * progress).toInt())

        // Background
        canvas.drawRoundRect(rect, cornerPx, cornerPx, bgPaint)

        // Header
        val headerTop = when (expandV) {
            ExpandV.DOWN -> rect.top
            ExpandV.UP -> rect.bottom - headerHPx
        }
        val headerRect = RectF(rect.left, headerTop, rect.right, headerTop + headerHPx)
        canvas.drawRoundRect(
            headerRect.left, headerRect.top,
            headerRect.right, headerRect.bottom,
            cornerPx, cornerPx, headerPaint
        )

        // Header title
        val titleY = headerRect.centerY() - (headerTextPaint.descent() + headerTextPaint.ascent()) / 2
        canvas.drawText(title, rect.left + padHPx, titleY, headerTextPaint)

        // Header action icons
        if (headerActions.isNotEmpty()) {
            var actionX = rect.right - padHPx
            for (ha in headerActions.reversed()) {
                actionX -= headerActionSizePx
                ha.icon?.let { d ->
                    val iconTop = (headerRect.centerY() - headerActionSizePx / 2).toInt()
                    d.mutate().apply {
                        setBounds(actionX.toInt(), iconTop,
                            actionX.toInt() + headerActionSizePx, iconTop + headerActionSizePx)
                        setTint(UiColors.ICON_FG)
                        draw(canvas)
                    }
                }
                actionX -= padHPx / 2
            }
        }

        // Items
        val itemsTop = when (expandV) {
            ExpandV.DOWN -> rect.top + headerHPx + padVPx
            ExpandV.UP -> rect.top + padVPx
        }

        for (i in items.indices) {
            val itemTop = itemsTop + i * itemHPx
            val item = items[i]
            val paint = if (item.enabled) textPaint else disabledTextPaint

            // Press highlight
            if (i == pressedIndex && item.enabled) {
                canvas.drawRect(rect.left, itemTop, rect.right, itemTop + itemHPx, pressPaint)
            }

            var textX = rect.left + padHPx

            // Item icon
            item.icon?.let { d ->
                val iconLeft = textX.toInt()
                val iconTop2 = (itemTop + (itemHPx - iconSizePx) / 2).toInt()
                d.mutate().apply {
                    setBounds(iconLeft, iconTop2, iconLeft + iconSizePx, iconTop2 + iconSizePx)
                    setTint(if (item.enabled) UiColors.ICON_FG else UiColors.MENU_TEXT_DISABLED)
                    draw(canvas)
                }
                textX += iconSizePx + padHPx / 2
            }

            // Label
            val textY = itemTop + itemHPx / 2 - (paint.descent() + paint.ascent()) / 2
            canvas.drawText(item.label, textX, textY, paint)

            // Toggle checkmark
            if (item.checked != null) {
                val mark = if (item.checked) "✓" else ""
                val markW = checkPaint.measureText("✓")
                canvas.drawText(mark, rect.right - padHPx - markW, textY, checkPaint)
            }

            // Divider (not after last item)
            if (i < items.size - 1) {
                val divY = itemTop + itemHPx
                canvas.drawLine(rect.left + padHPx, divY, rect.right - padHPx, divY, dividerPaint)
            }
        }

        canvas.restoreToCount(save)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val rect = menuRect()
        val inMenu = rect.contains(event.x, event.y)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!inMenu) {
                    dismiss()
                    return true
                }
                pressedIndex = itemIndexAt(event.y, rect)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                val newIndex = if (inMenu) itemIndexAt(event.y, rect) else -1
                if (newIndex != pressedIndex) {
                    pressedIndex = newIndex
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                val index = itemIndexAt(event.y, rect)
                pressedIndex = -1
                invalidate()
                if (index in items.indices) {
                    val item = items[index]
                    if (item.enabled) {
                        item.action?.invoke()
                        dismiss()
                    }
                } else if (inMenu) {
                    // Tapped header action?
                    handleHeaderActionTap(event.x, event.y, rect)
                } else {
                    dismiss()
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedIndex = -1
                invalidate()
            }
        }
        return true
    }

    private fun itemIndexAt(y: Float, rect: RectF): Int {
        val itemsTop = when (expandV) {
            ExpandV.DOWN -> rect.top + headerHPx + padVPx
            ExpandV.UP -> rect.top + padVPx
        }
        val index = ((y - itemsTop) / itemHPx).toInt()
        return if (index in items.indices) index else -1
    }

    private fun handleHeaderActionTap(x: Float, y: Float, rect: RectF) {
        if (headerActions.isEmpty()) return
        val headerTop = when (expandV) {
            ExpandV.DOWN -> rect.top
            ExpandV.UP -> rect.bottom - headerHPx
        }
        if (y < headerTop || y > headerTop + headerHPx) return

        var actionX = rect.right - padHPx
        for (ha in headerActions.reversed()) {
            actionX -= headerActionSizePx
            if (x >= actionX && x <= actionX + headerActionSizePx) {
                ha.action?.invoke()
                dismiss()
                return
            }
            actionX -= padHPx / 2
        }
    }

    private fun animateIn() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = UiMetrics.MENU_ANIM_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun animateOut(onEnd: () -> Unit) {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(progress, 0f).apply {
            duration = (UiMetrics.MENU_ANIM_MS * progress).toLong()
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onEnd()
                }
            })
            start()
        }
    }

    private fun registerBackHandler() {
        val activity = context as? Activity ?: return
        activity.onBackInvokedDispatcher.registerOnBackInvokedCallback(
            android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            backCallback
        )
    }

    private fun unregisterBackHandler() {
        val activity = context as? Activity ?: return
        try {
            activity.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(backCallback)
        } catch (_: Exception) {
        }
    }
}
