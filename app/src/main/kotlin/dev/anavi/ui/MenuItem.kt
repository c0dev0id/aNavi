package dev.anavi.ui

import android.graphics.drawable.Drawable

class MenuItem(
    val label: String,
    val icon: Drawable? = null,
    val enabled: Boolean = true,
    val checked: Boolean? = null,
    val action: (() -> Unit)? = null
)
