package com.example.minibooklibrary.domain

/** Sort orders the user can apply to the book list. */
enum class SortOrder(val storageValue: String) {
    DATE_ADDED_DESC("date_desc"),
    DATE_ADDED_ASC("date_asc"),
    TITLE_ASC("title_asc"),
    TITLE_DESC("title_desc"),
    RATING_DESC("rating_desc"),
    RATING_ASC("rating_asc");

    companion object {
        fun fromStorageValue(value: String?): SortOrder =
            entries.firstOrNull { it.storageValue == value } ?: DATE_ADDED_DESC
    }
}
