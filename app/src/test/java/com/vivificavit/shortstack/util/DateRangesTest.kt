package com.vivificavit.shortstack.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class DateRangesTest {
    private val pacific = ZoneId.of("America/Los_Angeles")

    /** What Material3's DatePicker reports when the user taps [date]. */
    private fun pickerMillis(date: LocalDate) =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun selectedDayCoversThatWholeLocalDay() {
        val oct30 = LocalDate.of(2026, 10, 30)
        val range = DateRanges.toLocalRange(pickerMillis(oct30), pickerMillis(oct30), pacific)

        val start = ZonedDateTime.of(2026, 10, 30, 0, 0, 0, 0, pacific).toInstant().toEpochMilli()
        val nextDay = ZonedDateTime.of(2026, 10, 31, 0, 0, 0, 0, pacific).toInstant().toEpochMilli()
        assertEquals(start, range.first)
        assertEquals(nextDay - 1, range.last)
    }

    @Test
    fun eveningEntryOnEndDateIsIncluded() {
        val oct30 = LocalDate.of(2026, 10, 30)
        val range = DateRanges.toLocalRange(null, pickerMillis(oct30), pacific)
        val eightPm = ZonedDateTime.of(2026, 10, 30, 20, 0, 0, 0, pacific).toInstant().toEpochMilli()
        assert(eightPm in range)
    }

    @Test
    fun openEndsAndReversedRange() {
        assertEquals(0L..Long.MAX_VALUE, DateRanges.toLocalRange(null, null, pacific))
        val a = pickerMillis(LocalDate.of(2026, 1, 1))
        val b = pickerMillis(LocalDate.of(2026, 2, 1))
        assertEquals(
            DateRanges.toLocalRange(a, b, pacific),
            DateRanges.toLocalRange(b, a, pacific)
        )
    }

    @Test
    fun presetsCoverTheExpectedDays() {
        val today = LocalDate.of(2026, 3, 15)
        assertEquals(
            pickerMillis(LocalDate.of(2026, 3, 1)) to null,
            DateRanges.presetRange(DateRanges.Preset.THIS_MONTH, today)
        )
        assertEquals(
            pickerMillis(LocalDate.of(2026, 1, 1)) to null,
            DateRanges.presetRange(DateRanges.Preset.THIS_YEAR, today)
        )
        assertEquals(null to null, DateRanges.presetRange(DateRanges.Preset.ALL_TIME, today))
    }

    @Test
    fun movingToAnotherDayKeepsTimeOfDay() {
        val evening = ZonedDateTime.of(2026, 10, 30, 19, 45, 0, 0, pacific).toInstant().toEpochMilli()
        val moved = DateRanges.moveToDate(evening, LocalDate.of(2026, 10, 2), pacific)
        assertEquals(
            ZonedDateTime.of(2026, 10, 2, 19, 45, 0, 0, pacific).toInstant().toEpochMilli(),
            moved
        )
        assertEquals(LocalDate.of(2026, 10, 2), DateRanges.timestampToLocalDate(moved, pacific))
    }
}
