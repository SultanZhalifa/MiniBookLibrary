package com.example.minibooklibrary.domain

/** Sort orders the user can apply to the book list. */
enum class SortOrder(val storageValue: String, val displayLabel: String) {
    DATE_ADDED_DESC("date_desc", "Newest First"),
    DATE_ADDED_ASC("date_asc", "Oldest First"),
    TITLE_ASC("title_asc", "Title (A-Z)"),
    TITLE_DESC("title_desc", "Title (Z-A)"),
    RATING_DESC("rating_desc", "Rating (High-Low)"),
    RATING_ASC("rating_asc", "Rating (Low-High)");

    companion object {
        fun fromStorageValue(value: String?): SortOrder =
            entries.firstOrNull { it.storageValue == value } ?: DATE_ADDED_DESC
    }
}
