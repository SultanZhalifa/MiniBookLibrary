package com.example.minibooklibrary.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class ReadingStatsTest {

    private val today = LocalDate.of(2026, 10, 28)
    private fun daysAgo(n: Long, pages: Int = 10) = DailyPages(today.minusDays(n).toEpochDay(), pages)

    @Test
    fun `no reading means no streak and an empty week`() {
        val stats = ReadingStats.from(emptyList(), today)
        assertEquals(0, stats.currentStreak)
        assertEquals(0, stats.longestStreak)
        assertEquals(List(7) { 0 }, stats.lastSevenDays)
    }

    @Test
    fun `consecutive days ending today form the current streak`() {
        val stats = ReadingStats.from(listOf(daysAgo(2), daysAgo(1), daysAgo(0)), today)
        assertEquals(3, stats.currentStreak)
    }

    @Test
    fun `streak is still alive when the last reading was yesterday`() {
        val stats = ReadingStats.from(listOf(daysAgo(2), daysAgo(1)), today)
        assertEquals(2, stats.currentStreak)
    }

    @Test
    fun `a missed day breaks the streak`() {
        val stats = ReadingStats.from(listOf(daysAgo(3), daysAgo(2)), today)
        assertEquals(0, stats.currentStreak)
        assertEquals(2, stats.longestStreak)
    }

    @Test
    fun `longest streak looks at the whole history`() {
        val history = (20L downTo 15L).map { daysAgo(it) } + listOf(daysAgo(1), daysAgo(0))
        val stats = ReadingStats.from(history, today)
        assertEquals(2, stats.currentStreak)
        assertEquals(6, stats.longestStreak)
    }

    @Test
    fun `days with zero pages do not count`() {
        val stats = ReadingStats.from(listOf(daysAgo(1), daysAgo(0, pages = 0)), today)
        assertEquals(1, stats.currentStreak)
    }

    @Test
    fun `last seven days are oldest first and end today`() {
        val stats = ReadingStats.from(
            listOf(daysAgo(6, pages = 5), daysAgo(0, pages = 30), daysAgo(9, pages = 99)),
            today
        )
        assertEquals(listOf(5, 0, 0, 0, 0, 0, 30), stats.lastSevenDays)
        assertEquals(35, stats.pagesThisWeek)
    }

    @Test
    fun `finishedInYear counts only that calendar year`() {
        val utc = ZoneOffset.UTC
        val in2026 = LocalDate.of(2026, 3, 1).atStartOfDay(utc).toInstant().toEpochMilli()
        val in2025 = LocalDate.of(2025, 12, 31).atStartOfDay(utc).toInstant().toEpochMilli()
        assertEquals(1, ReadingStats.finishedInYear(listOf(in2026, in2025, null), 2026, utc))
    }
}
