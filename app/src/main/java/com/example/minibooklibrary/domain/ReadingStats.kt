package com.example.minibooklibrary.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Total pages logged on one day. Room fills this straight from a GROUP BY query. */
data class DailyPages(val epochDay: Long, val pages: Int)

/**
 * Numbers for the dashboard's reading card.
 *
 * @property currentStreak consecutive days with reading, ending today or yesterday.
 * @property longestStreak best run ever.
 * @property lastSevenDays pages per day, oldest first, ending today. Always 7 entries.
 */
data class ReadingStats(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastSevenDays: List<Int> = List(7) { 0 }
) {
    val pagesThisWeek: Int get() = lastSevenDays.sum()

    companion object {

        /**
         * A streak survives until the end of the day after the last reading, so opening
         * the app in the morning before you've read doesn't show it as broken.
         */
        fun from(days: List<DailyPages>, today: LocalDate): ReadingStats {
            val readDays = days.filter { it.pages > 0 }.map { it.epochDay }.toSortedSet()
            val todayDay = today.toEpochDay()

            var longest = 0
            var run = 0
            var previous: Long? = null
            for (day in readDays) {
                run = if (previous != null && day == previous + 1) run + 1 else 1
                longest = maxOf(longest, run)
                previous = day
            }

            val anchor = when {
                todayDay in readDays -> todayDay
                todayDay - 1 in readDays -> todayDay - 1
                else -> null
            }
            var current = 0
            if (anchor != null) {
                var day = anchor
                while (day in readDays) {
                    current++
                    day--
                }
            }

            val pagesByDay = days.groupBy { it.epochDay }.mapValues { (_, v) -> v.sumOf { it.pages } }
            val week = (6 downTo 0).map { offset -> pagesByDay[todayDay - offset] ?: 0 }

            return ReadingStats(currentStreak = current, longestStreak = longest, lastSevenDays = week)
        }

        /** How many of [finishedAt] timestamps fall in [year] in [zone]. */
        fun finishedInYear(finishedAt: List<Long?>, year: Int, zone: ZoneId): Int =
            finishedAt.count { millis ->
                millis != null && Instant.ofEpochMilli(millis).atZone(zone).year == year
            }
    }
}
