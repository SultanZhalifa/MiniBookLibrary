package com.example.minibooklibrary.ui.common

import androidx.annotation.StringRes
import com.example.minibooklibrary.R
import com.example.minibooklibrary.domain.ReadingStatus
import com.example.minibooklibrary.domain.SortOrder
import com.example.minibooklibrary.domain.StatusFilter

/*
 * User-facing labels for the domain enums. They live here rather than on the enums so the
 * domain package stays free of Android resources and the labels can be translated.
 */

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
