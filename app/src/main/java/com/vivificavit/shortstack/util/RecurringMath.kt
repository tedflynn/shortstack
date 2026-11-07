package com.vivificavit.shortstack.util

import com.vivificavit.shortstack.data.model.Frequency
import com.vivificavit.shortstack.data.model.RecurringEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** When repeating entries come due. */
object RecurringMath {

    /** A safety cap on how many entries one schedule adds at once, however long the app sat unopened. */
    const val MAX_CATCH_UP = 120

    /**
     * When entry number [index] (from 0) of a schedule falls. Each one counts
     * from the start, so a monthly entry on the 31st lands on the last day of
     * shorter months and goes back to the 31st after.
     */
    fun occurrence(startTimestamp: Long, frequency: Frequency, index: Int, zone: ZoneId): ZonedDateTime {
        val start = Instant.ofEpochMilli(startTimestamp).atZone(zone)
        return when (frequency) {
            Frequency.WEEKLY -> start.plusWeeks(index.toLong())
            Frequency.EVERY_2_WEEKS -> start.plusWeeks(2L * index)
            Frequency.MONTHLY -> start.plusMonths(index.toLong())
        }
    }

    /**
     * The timestamps of the entries [entry] should add by [now]: every one not
     * added yet whose day has come. Each is dated its own day, at the start's
     * time of day, but never later than [now].
     */
    fun dueTimestamps(entry: RecurringEntry, now: Long, zone: ZoneId): List<Long> {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return generateSequence(entry.occurrences) { it + 1 }
            .map { occurrence(entry.startTimestamp, entry.frequency, it, zone) }
            .takeWhile { !it.toLocalDate().isAfter(today) }
            .take(MAX_CATCH_UP)
            .map { minOf(it.toInstant().toEpochMilli(), now) }
            .toList()
    }

    /** The day the next entry will be added. */
    fun nextDate(entry: RecurringEntry, zone: ZoneId): LocalDate =
        occurrence(entry.startTimestamp, entry.frequency, entry.occurrences, zone).toLocalDate()
}
