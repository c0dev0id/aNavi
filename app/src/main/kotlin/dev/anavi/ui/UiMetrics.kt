package dev.anavi.ui

import android.content.Context

object UiMetrics {
    const val BUTTON_SIZE = 56f
    const val ICON_PAD = 12f
    const val CORNER_R = 4f
    const val MARGIN = 16f

    const val MENU_ITEM_H = 48f
    const val MENU_PAD_H = 16f
    const val MENU_PAD_V = 8f
    const val MENU_HEADER_H = 44f
    const val MENU_WIDTH = 220f
    const val MENU_TEXT_SIZE = 16f
    const val MENU_HEADER_TEXT_SIZE = 14f
    const val MENU_ANIM_MS = 200L

    const val RING_RADIUS = 28f
    const val RING_STROKE = 4f
    const val RING_FILL_MS = 800L

    const val CROSSHAIR_ARM = 12f
    const val CROSSHAIR_GAP = 4f
    const val CROSSHAIR_STROKE = 2f
    const val CROSSHAIR_FADE_DIST = 80f

    const val SPOKE_SPACING = 8f
    const val SPOKE_STAGGER_MS = 40L
    const val SPOKE_ANIM_MS = 200L

    const val SEARCH_RING_SIZE = 56f

    const val CARD_PAD_H = 10f
    const val CARD_PAD_V = 6f
    const val CARD_TEXT_SIZE = 11f
    const val CARD_CORNER_R = 4f

    fun dp(context: Context, dp: Float): Float =
        dp * context.resources.displayMetrics.density
}
