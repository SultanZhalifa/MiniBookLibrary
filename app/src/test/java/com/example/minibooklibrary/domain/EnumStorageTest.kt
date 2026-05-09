package com.example.minibooklibrary.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pin the persisted [storageValue]s for each enum. Future contributors who reorder or
 * rename a constant must update these tests, which acts as a tripwire against breaking
 * existing user data.
 */
class EnumStorageTest {

    @Test
    fun `ReadingStatus storage values are stable`() {
        assertEquals("want_to_read", ReadingStatus.WANT_TO_READ.storageValue)
        assertEquals("currently_reading", ReadingStatus.CURRENTLY_READING.storageValue)
        assertEquals("finished", ReadingStatus.FINISHED.storageValue)
    }

    @Test
    fun `ReadingStatus from storage value falls back to want-to-read`() {
        assertEquals(ReadingStatus.WANT_TO_READ, ReadingStatus.fromStorageValue(null))
        assertEquals(ReadingStatus.WANT_TO_READ, ReadingStatus.fromStorageValue("garbage"))
        assertEquals(ReadingStatus.FINISHED, ReadingStatus.fromStorageValue("finished"))
    }

    @Test
    fun `SortOrder storage values are stable`() {
        assertEquals("date_desc", SortOrder.DATE_ADDED_DESC.storageValue)
        assertEquals("title_asc", SortOrder.TITLE_ASC.storageValue)
        assertEquals("rating_desc", SortOrder.RATING_DESC.storageValue)
    }

    @Test
    fun `StatusFilter ALL is the identity filter default`() {
        assertEquals(StatusFilter.ALL, StatusFilter.fromStorageValue(null))
        assertEquals(StatusFilter.WANT_TO_READ, StatusFilter.fromStorageValue("want_to_read"))
    }
}
