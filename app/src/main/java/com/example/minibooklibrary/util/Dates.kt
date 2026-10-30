package com.example.minibooklibrary.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Format epoch milliseconds as a friendly absolute date — used in book detail / PDF. */
fun Long.formatDate(pattern: String = "MMM d, yyyy"): String =
    SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
