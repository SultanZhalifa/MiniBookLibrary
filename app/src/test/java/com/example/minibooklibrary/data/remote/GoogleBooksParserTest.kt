package com.example.minibooklibrary.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure JSON-parsing tests. No HTTP, no Android dependencies — runs as a fast JVM test.
 *
 * Fixtures are inline rather than under `resources/` because we want the test to be
 * self-contained and easy to scan when reviewing.
 */
class GoogleBooksParserTest {

    @Test
    fun `parses a complete volume entry`() {
        val json = """
            {
              "totalItems": 1,
              "items": [
                {
                  "volumeInfo": {
                    "title": "The Pragmatic Programmer",
                    "authors": ["Andrew Hunt", "David Thomas"],
                    "publishedDate": "1999-10-30",
                    "categories": ["Computers / Programming"],
                    "pageCount": 320
                  }
                }
              ]
            }
        """.trimIndent()

        val result = GoogleBooksParser.parseFirstVolume(json)
        requireNotNull(result)
        assertEquals("The Pragmatic Programmer", result.title)
        assertEquals("Andrew Hunt", result.author)
        assertEquals("Computers", result.category) // taxonomy split on " / "
        assertEquals(1999, result.year)
        assertEquals(320, result.totalPages)
    }

    @Test
    fun `returns null when items array is empty`() {
        val json = """{ "totalItems": 0, "items": [] }"""
        assertNull(GoogleBooksParser.parseFirstVolume(json))
    }

    @Test
    fun `returns null when items array is missing`() {
        val json = """{ "totalItems": 0 }"""
        assertNull(GoogleBooksParser.parseFirstVolume(json))
    }

    @Test
    fun `tolerates missing optional fields`() {
        val json = """
            {
              "items": [
                { "volumeInfo": { "title": "Bare Title" } }
              ]
            }
        """.trimIndent()
        val result = GoogleBooksParser.parseFirstVolume(json)
        requireNotNull(result)
        assertEquals("Bare Title", result.title)
        assertEquals("", result.author)
        assertEquals("", result.category)
        assertEquals(0, result.year)
        assertEquals(0, result.totalPages)
    }

    @Test
    fun `picks first author when multiple are listed`() {
        val json = """
            {
              "items": [
                {
                  "volumeInfo": {
                    "title": "T",
                    "authors": ["First", "Second", "Third"]
                  }
                }
              ]
            }
        """.trimIndent()
        assertEquals("First", GoogleBooksParser.parseFirstVolume(json)?.author)
    }

    @Test
    fun `parses year-only and year-month dates`() {
        assertEquals(2010, GoogleBooksParser.parsePublishedYear("2010"))
        assertEquals(2010, GoogleBooksParser.parsePublishedYear("2010-08"))
        assertEquals(2010, GoogleBooksParser.parsePublishedYear("2010-08-15"))
        assertEquals(0, GoogleBooksParser.parsePublishedYear(""))
        assertEquals(0, GoogleBooksParser.parsePublishedYear(null))
        assertEquals(0, GoogleBooksParser.parsePublishedYear("nope"))
        assertEquals(0, GoogleBooksParser.parsePublishedYear("9999")) // out of plausible range
    }

    @Test
    fun `service returns null when http client returns null`() = kotlinx.coroutines.test.runTest {
        val service = GoogleBooksService(httpClient = { _ -> null })
        assertNull(service.lookupByIsbn("9780201616224"))
    }

    @Test
    fun `service returns null for malformed ISBN`() = kotlinx.coroutines.test.runTest {
        val service = GoogleBooksService(httpClient = { _ -> error("should not be called") })
        assertNull(service.lookupByIsbn("123"))
        assertNull(service.lookupByIsbn(""))
        assertNull(service.lookupByIsbn("not-numeric-at-all"))
    }

    @Test
    fun `service returns parsed lookup for happy path`() = kotlinx.coroutines.test.runTest {
        val canned = """
            { "items": [{ "volumeInfo": { "title": "Hello", "authors": ["A"] } }] }
        """.trimIndent()
        val service = GoogleBooksService(httpClient = { _ -> canned })
        val result = service.lookupByIsbn("9780201616224")
        requireNotNull(result)
        assertEquals("Hello", result.title)
        assertEquals("A", result.author)
    }
}
