package com.example.minibooklibrary.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IsbnTest {

    @Test
    fun `accepts a valid ISBN-13`() {
        assertEquals("9780201616224", Isbn.normalize("9780201616224"))
    }

    @Test
    fun `strips hyphens and spaces`() {
        assertEquals("9780201616224", Isbn.normalize("978-0-201-61622-4"))
        assertEquals("9780201616224", Isbn.normalize(" 978 0201 616224 "))
    }

    @Test
    fun `accepts a valid ISBN-10 including an X check digit`() {
        assertEquals("020161622X", Isbn.normalize("0-201-61622-x"))
        assertEquals("0306406152", Isbn.normalize("0306406152"))
    }

    @Test
    fun `rejects a bad check digit`() {
        assertNull(Isbn.normalize("9780201616225"))
        assertNull(Isbn.normalize("0306406153"))
    }

    @Test
    fun `rejects EAN-13 barcodes that are not books`() {
        // Valid EAN-13 checksum, but a grocery prefix rather than 978/979.
        assertNull(Isbn.normalize("4006381333931"))
    }

    @Test
    fun `rejects wrong lengths and junk`() {
        assertNull(Isbn.normalize(null))
        assertNull(Isbn.normalize(""))
        assertNull(Isbn.normalize("123"))
        assertNull(Isbn.normalize("97802016162"))
        assertNull(Isbn.normalize("not-an-isbn"))
        assertFalse(Isbn.isValid("97802016162244"))
        assertTrue(Isbn.isValid("9780306406157"))
    }
}
