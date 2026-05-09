package com.example.minibooklibrary.util

import android.content.Context
import android.text.format.DateUtils
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.minibooklibrary.domain.ReadingStatus
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

fun View.show() { visibility = View.VISIBLE }
fun View.hide() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }
fun View.showIf(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
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

/** UI-friendly label for a reading status. */
fun ReadingStatus.label(): String = displayLabel
