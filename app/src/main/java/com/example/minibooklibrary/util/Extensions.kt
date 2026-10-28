package com.example.minibooklibrary.util

import android.content.Context
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.example.minibooklibrary.R
import com.example.minibooklibrary.domain.ReadingStatus
import com.example.minibooklibrary.domain.SortOrder
import com.example.minibooklibrary.domain.StatusFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
fun View.hide() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }
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

/** Format epoch milliseconds as a friendly absolute date — used in book detail / PDF. */
fun Long.formatDate(pattern: String = "MMM d, yyyy"): String =
    SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))

/** Format epoch milliseconds as a relative string ("3 hours ago") for activity rails. */
fun Long.formatRelative(): String =
    DateUtils.getRelativeTimeSpanString(
        this,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()

// User-facing labels for the domain enums. They live here rather than on the enums so
// the domain package stays free of Android resources and the labels can be translated.

@get:StringRes
val ReadingStatus.labelRes: Int
    get() = when (this) {
        ReadingStatus.WANT_TO_READ -> R.string.status_want_to_read
        ReadingStatus.CURRENTLY_READING -> R.string.status_currently_reading
        ReadingStatus.FINISHED -> R.string.status_finished
    }

@get:StringRes
val SortOrder.labelRes: Int
    get() = when (this) {
        SortOrder.DATE_ADDED_DESC -> R.string.sort_newest
        SortOrder.DATE_ADDED_ASC -> R.string.sort_oldest
        SortOrder.TITLE_ASC -> R.string.sort_title_asc
        SortOrder.TITLE_DESC -> R.string.sort_title_desc
        SortOrder.RATING_DESC -> R.string.sort_rating_desc
        SortOrder.RATING_ASC -> R.string.sort_rating_asc
    }

@get:StringRes
val StatusFilter.labelRes: Int
    get() = when (this) {
        StatusFilter.ALL -> R.string.filter_all
        StatusFilter.WANT_TO_READ -> R.string.status_want_to_read
        StatusFilter.CURRENTLY_READING -> R.string.status_currently_reading
        StatusFilter.FINISHED -> R.string.status_finished
    }
