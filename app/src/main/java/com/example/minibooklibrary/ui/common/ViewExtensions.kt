package com.example.minibooklibrary.ui.common

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment

/** Show a short toast — the most common shape across the app. */
fun Context.toast(message: CharSequence) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Fragment.toast(message: CharSequence) {
    requireContext().toast(message)
}

fun Fragment.toast(@StringRes message: Int) {
    requireContext().toast(getString(message))
}

fun View.show() { visibility = View.VISIBLE }
fun View.showIf(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}

/**
 * Pad this view by the system bar insets on the chosen edges, on top of the padding it
 * already has in XML. Since targetSdk 35 every window is drawn edge-to-edge, so anything
 * pinned to the top or bottom of the screen has to opt in to staying clear of the bars.
 *
 * With [ime] the bottom edge also follows the keyboard. `adjustResize` no longer does
 * that for edge-to-edge windows.
 */
fun View.applySystemBarInsets(top: Boolean = false, bottom: Boolean = false, ime: Boolean = false) {
    val initialTop = paddingTop
    val initialBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        val barBottom = if (bottom) bars.bottom else 0
        val imeBottom = if (ime) insets.getInsets(WindowInsetsCompat.Type.ime()).bottom else 0
        view.updatePadding(
            top = initialTop + if (top) bars.top else 0,
            bottom = initialBottom + maxOf(barBottom, imeBottom)
        )
        insets
    }
}

/**
 * Like [applySystemBarInsets] but moves the view with its margins instead of growing it.
 * For floating things (FABs, the detail screen's action pill) and fixed-height toolbars.
 */
fun View.applySystemBarMargins(top: Boolean = false, bottom: Boolean = false) {
    val params = layoutParams as ViewGroup.MarginLayoutParams
    val initialTop = params.topMargin
    val initialBottom = params.bottomMargin
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = initialTop + if (top) bars.top else 0
            bottomMargin = initialBottom + if (bottom) bars.bottom else 0
        }
        insets
    }
}
