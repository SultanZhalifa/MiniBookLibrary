package com.example.minibooklibrary.ui.common

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
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
 * [enableEdgeToEdge], plus a guard for the activity Android recreates when the user
 * switches to dark mode. Call it after `setContentView`.
 *
 * In that recreated activity the framework and AppCompat wrappers above our layout
 * (`fitsSystemWindows="true"` in their own XML) kept padding themselves by the system bar
 * heights, so the whole app sat inside the bars until the next launch. A fresh activity in
 * the same theme was fine. The emulator screenshot tour caught it.
 *
 * Every screen here applies insets itself, so no wrapper should ever pad: each one is told
 * to leave the insets alone and pass them on to our views.
 */
fun ComponentActivity.setUpEdgeToEdge() {
    enableEdgeToEdge()
    val content = findViewById<View>(android.R.id.content)
    generateSequence(content.parent as? View) { it.parent as? View }
        .filter { it.fitsSystemWindows }
        .forEach { wrapper ->
            ViewCompat.setOnApplyWindowInsetsListener(wrapper) { view, insets ->
                view.setPadding(0, 0, 0, 0)
                insets
            }
        }
    ViewCompat.requestApplyInsets(window.decorView)
}

/**
 * Pad this view by the system bar insets on the chosen edges, on top of the padding it
 * already has in XML. Since targetSdk 35 every window is drawn edge-to-edge, so anything
 * pinned to the top or bottom of the screen has to opt in to staying clear of the bars.
 *
 * With [ime] the bottom edge also follows the keyboard. `adjustResize` no longer does
 * that for edge-to-edge windows.
 *
 * [source] is the view whose insets are read. Pass the fragment root when this view sits
 * inside a CollapsingToolbarLayout: that layout consumes insets before its children see them.
 */
fun View.applySystemBarInsets(
    top: Boolean = false,
    bottom: Boolean = false,
    ime: Boolean = false,
    source: View = this
) {
    val initialTop = paddingTop
    val initialBottom = paddingBottom
    source.doOnInsets { bars, imeBottom ->
        updatePadding(
            top = initialTop + if (top) bars.top else 0,
            bottom = initialBottom + maxOf(if (bottom) bars.bottom else 0, if (ime) imeBottom else 0)
        )
    }
}

/**
 * Like [applySystemBarInsets] but moves the view with its margins instead of growing it.
 * For floating things (FABs, the detail screen's action pill), fixed-height toolbars, and
 * scroll views that should shrink above the keyboard ([ime]) so the focused field scrolls
 * into view.
 */
fun View.applySystemBarMargins(
    top: Boolean = false,
    bottom: Boolean = false,
    ime: Boolean = false,
    source: View = this
) {
    val params = layoutParams as ViewGroup.MarginLayoutParams
    val initialTop = params.topMargin
    val initialBottom = params.bottomMargin
    source.doOnInsets { bars, imeBottom ->
        updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = initialTop + if (top) bars.top else 0
            bottomMargin = initialBottom +
                maxOf(if (bottom) bars.bottom else 0, if (ime) imeBottom else 0)
        }
    }
}

/** One listener per view: each screen uses a given [View] as `source` at most once. */
private fun View.doOnInsets(block: (bars: Insets, imeBottom: Int) -> Unit) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
        block(
            insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()),
            insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        )
        insets
    }
    // A fragment's view is attached after the window's first inset pass; ask for another.
    doOnAttach { ViewCompat.requestApplyInsets(it) }
}
