package com.example.minibooklibrary.domain

/**
 * Filter options applied on top of search/sort to scope the book list by reading status.
 *
 * [ALL] is the no-op identity filter — it returns every book regardless of status.
 */
enum class StatusFilter(val storageValue: String, val displayLabel: String) {
    ALL("all", "All Books"),
    WANT_TO_READ("want_to_read", "Want to Read"),
    CURRENTLY_READING("currently_reading", "Currently Reading"),
    FINISHED("finished", "Finished");

    companion object {
        fun fromStorageValue(value: String?): StatusFilter =
            entries.firstOrNull { it.storageValue == value } ?: ALL
    }
}
