package com.example.minibooklibrary.data.remote

import android.util.Log
import com.example.minibooklibrary.domain.BookLookup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Tiny client for the public Google Books volumes endpoint.
 *
 * Why we hand-roll HTTP instead of pulling OkHttp:
 *  - One endpoint, one JSON response — adding a 800KB networking lib is overkill.
 *  - `HttpURLConnection` ships with the platform, so the APK stays the same size.
 *  - Parsing uses [org.json] which we already pull in for backups.
 *
 * The endpoint is unauthenticated for low-volume reads, which is what a personal
 * library does. Add an API key via the `key=` param if you start seeing 429s.
 */
class GoogleBooksService(
    private val httpClient: HttpClient = DefaultHttpClient,
    private val parser: GoogleBooksParser = GoogleBooksParser
) {

    /**
     * Look up a book by ISBN-10 or ISBN-13. Returns null on any network/parse failure —
     * this feature is best-effort and the user can always type fields manually.
     */
    suspend fun lookupByIsbn(isbn: String): BookLookup? = withContext(Dispatchers.IO) {
        val cleaned = isbn.filter { it.isDigit() || it == 'X' || it == 'x' }
        if (cleaned.length !in 10..13) return@withContext null

        val url = "https://www.googleapis.com/books/v1/volumes?q=isbn:" +
            URLEncoder.encode(cleaned, "UTF-8") +
            "&maxResults=1&printType=books"

        // Cap at 8s so a flaky connection doesn't hang the UI thread that's awaiting us.
        val body = withTimeoutOrNull(8_000L) { httpClient.get(url) } ?: return@withContext null
        runCatching { parser.parseFirstVolume(body) }
            .onFailure { Log.w(TAG, "Failed to parse Google Books response", it) }
            .getOrNull()
    }

    companion object {
        private const val TAG = "GoogleBooksService"
    }
}

/** Abstraction so tests can stub HTTP without hitting the network. */
fun interface HttpClient {
    fun get(url: String): String?
}

private object DefaultHttpClient : HttpClient {
    override fun get(url: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5_000
                readTimeout = 5_000
                setRequestProperty("Accept", "application/json")
            }
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (t: Throwable) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}

/**
 * Pure JSON → [BookLookup] parser, separated from networking so it can be exhaustively
 * unit-tested with sample fixtures.
 */
object GoogleBooksParser {

    fun parseFirstVolume(rawJson: String): BookLookup? {
        val root = JSONObject(rawJson)
        val items = root.optJSONArray("items") ?: return null
        if (items.length() == 0) return null

        val volumeInfo = items.getJSONObject(0).optJSONObject("volumeInfo") ?: return null

        val title = volumeInfo.optString("title").takeIf { it.isNotBlank() } ?: return null
        val authors = volumeInfo.optJSONArray("authors")
        val firstAuthor = if (authors != null && authors.length() > 0) authors.getString(0) else null
        val category = volumeInfo.optJSONArray("categories")
            ?.takeIf { it.length() > 0 }
            ?.getString(0)
            ?.substringBefore(" / ") // Google returns nested taxonomy like "Fiction / Romance"

        val publishedDate = volumeInfo.optString("publishedDate")
        val year = parsePublishedYear(publishedDate)

        val pageCount = volumeInfo.optInt("pageCount", 0).takeIf { it > 0 }

        return BookLookup(
            title = title,
            author = firstAuthor.orEmpty(),
            category = category.orEmpty(),
            year = year,
            totalPages = pageCount ?: 0
        )
    }

    /**
     * Google returns dates in any of `YYYY`, `YYYY-MM`, or `YYYY-MM-DD`. We only care
     * about the year — extract first 4 digits and accept it if it's plausible.
     */
    internal fun parsePublishedYear(input: String?): Int {
        if (input.isNullOrBlank()) return 0
        val candidate = input.take(4).toIntOrNull() ?: return 0
        return if (candidate in 1000..2100) candidate else 0
    }
}
