package com.example.minibooklibrary.util

/**
 * ISBN normalization and checksum validation.
 *
 * Shared by the barcode scanner, the Add/Edit form, and the Google Books client so all
 * three agree on what counts as an ISBN. The scanner in particular needs this: every
 * EAN-13 on a cereal box is 13 digits, but only the 978/979 "Bookland" prefixes with a
 * valid check digit are books.
 */
object Isbn {

    /**
     * Strip hyphens/spaces and return the ISBN if it's a valid ISBN-10 or ISBN-13,
     * otherwise null. A trailing lowercase `x` in an ISBN-10 is upper-cased.
     */
    fun normalize(input: String?): String? {
        if (input.isNullOrBlank()) return null
        val cleaned = input.filter { !it.isWhitespace() && it != '-' }.uppercase()
        return when {
            cleaned.length == 13 && isValidIsbn13(cleaned) -> cleaned
            cleaned.length == 10 && isValidIsbn10(cleaned) -> cleaned
            else -> null
        }
    }

    fun isValid(input: String?): Boolean = normalize(input) != null

    private fun isValidIsbn13(s: String): Boolean {
        if (!s.all { it.isDigit() }) return false
        if (!s.startsWith("978") && !s.startsWith("979")) return false
        val sum = s.take(12).mapIndexed { i, c -> (c - '0') * if (i % 2 == 0) 1 else 3 }.sum()
        return (10 - sum % 10) % 10 == s[12] - '0'
    }

    private fun isValidIsbn10(s: String): Boolean {
        if (!s.take(9).all { it.isDigit() }) return false
        val last = s[9]
        if (!last.isDigit() && last != 'X') return false
        val sum = s.mapIndexed { i, c ->
            val value = if (c == 'X') 10 else c - '0'
            value * (10 - i)
        }.sum()
        return sum % 11 == 0
    }
}
