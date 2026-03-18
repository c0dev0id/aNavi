package dev.anavi.ui

import android.animation.ValueAnimator
import android.content.Context
import android.view.Gravity
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout

class Spoke(
    context: Context,
    private val direction: Int,
    private val buttons: List<IconButton>
) {
    private val density = context.resources.displayMetrics.density
    private val buttonSizePx = (UiMetrics.BUTTON_SIZE * density + 0.5f).toInt()
    private val spacingPx = (UiMetrics.SPOKE_SPACING * density + 0.5f).toInt()
    private val stepPx = buttonSizePx + spacingPx

    private var extended = false
    private val animators = mutableListOf<ValueAnimator>()

    fun attachTo(parent: FrameLayout, centerX: Int, centerY: Int) {
        for (btn in buttons) {
            btn.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.NO_GRAVITY
            )
            btn.translationX = (centerX - buttonSizePx / 2).toFloat()
            btn.translationY = (centerY - buttonSizePx / 2).toFloat()
            btn.alpha = 0f
            parent.addView(btn)
        }
    }

    fun extend() {
        if (extended) return
        extended = true
        cancelAnimators()

        val sign = if (direction == Gravity.START || direction == Gravity.LEFT) -1 else 1
        val originX = buttons.firstOrNull()?.translationX ?: return

        for (i in buttons.indices) {
            val btn = buttons[i]
            val targetX = originX + sign * (i + 1) * stepPx
            val delay = i * UiMetrics.SPOKE_STAGGER_MS

            val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = UiMetrics.SPOKE_ANIM_MS
                startDelay = delay
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    val p = it.animatedValue as Float
                    btn.translationX = originX + (targetX - originX) * p
                    btn.alpha = p
                }
                start()
            }
            animators.add(anim)
        }
    }

    fun retract(onDone: (() -> Unit)? = null) {
        if (!extended) {
            onDone?.invoke()
            return
        }
        extended = false
        cancelAnimators()

        val originX = buttons.firstOrNull()?.let {
            val sign = if (direction == Gravity.START || direction == Gravity.LEFT) -1 else 1
            it.translationX - sign * stepPx
        } ?: run { onDone?.invoke(); return }

        var lastAnim: ValueAnimator? = null
        for (i in buttons.indices.reversed()) {
            val btn = buttons[i]
            val currentX = btn.translationX
            val delay = (buttons.size - 1 - i) * UiMetrics.SPOKE_STAGGER_MS

            val anim = ValueAnimator.ofFloat(1f, 0f).apply {
                duration = UiMetrics.SPOKE_ANIM_MS
                startDelay = delay
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    val p = it.animatedValue as Float
                    btn.translationX = originX + (currentX - originX) * p
                    btn.alpha = p
                }
                start()
            }
            animators.add(anim)
            lastAnim = anim
        }
        lastAnim?.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                onDone?.invoke()
            }
        })
    }

    fun detach(parent: FrameLayout) {
        cancelAnimators()
        for (btn in buttons) {
            parent.removeView(btn)
        }
    }

    private fun cancelAnimators() {
        animators.forEach { it.cancel() }
        animators.clear()
    }
}
