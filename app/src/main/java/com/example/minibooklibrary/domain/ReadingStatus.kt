package com.example.minibooklibrary.domain

/**
 * Reading status of a book in the user's library.
 *
 * The [storageValue] is what's persisted in Room — kept as a stable string so that
 * a future enum reordering doesn't break existing databases or JSON backups.
 */
enum class ReadingStatus(val storageValue: String, val displayLabel: String) {
    WANT_TO_READ("want_to_read", "Want to Read"),
    CURRENTLY_READING("currently_reading", "Currently Reading"),
    FINISHED("finished", "Finished");

    companion object {
        /** Resolve a status from its persisted [storageValue], defaulting to [WANT_TO_READ]. */
        fun fromStorageValue(value: String?): ReadingStatus =
            entries.firstOrNull { it.storageValue == value } ?: WANT_TO_READ
    }
}
