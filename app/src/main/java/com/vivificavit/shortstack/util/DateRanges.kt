package com.vivificavit.shortstack.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Material3's DatePicker reports the chosen day as midnight UTC. Reading
 * that value in the device's time zone shifts it to the previous day
 * anywhere west of UTC, so always convert it via its UTC calendar date.
 */
object DateRanges {
    fun pickerMillisToLocalDate(pickerUtcMillis: Long): LocalDate =
        Instant.ofEpochMilli(pickerUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()

    /** The value the DatePicker uses for [date]. */
    fun localDateToPickerMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun timestampToLocalDate(timestamp: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()

    /** Moves [timestamp] to [date], keeping its local time of day so same-day entries stay in order. */
    fun moveToDate(timestamp: Long, date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long {
        val timeOfDay = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalTime()
        return date.atTime(timeOfDay).atZone(zone).toInstant().toEpochMilli()
    }

    /** Quick choices for the chart date range. */
    enum class Preset { THIS_MONTH, LAST_MONTH, THIS_YEAR, ALL_TIME }

    /** The picker start/end values for [preset]; a null end means "up to today". */
    fun presetRange(preset: Preset, today: LocalDate): Pair<Long?, Long?> = when (preset) {
        Preset.THIS_MONTH -> localDateToPickerMillis(today.withDayOfMonth(1)) to null
        Preset.LAST_MONTH -> {
            val firstOfLastMonth = today.withDayOfMonth(1).minusMonths(1)
            localDateToPickerMillis(firstOfLastMonth) to
                localDateToPickerMillis(firstOfLastMonth.plusMonths(1).minusDays(1))
        }
        Preset.THIS_YEAR -> localDateToPickerMillis(today.withDayOfYear(1)) to null
        Preset.ALL_TIME -> null to null
    }

    /**
     * Turns the picker's start/end selections into an inclusive range of
     * epoch millis covering whole local days. Missing ends are open-ended,
     * and a reversed range is swapped.
     */
    fun toLocalRange(
        startPickerMillis: Long?,
        endPickerMillis: Long?,
        zone: ZoneId = ZoneId.systemDefault()
    ): LongRange {
        var startDate = startPickerMillis?.let(::pickerMillisToLocalDate)
        var endDate = endPickerMillis?.let(::pickerMillisToLocalDate)
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            startDate = endDate.also { endDate = startDate }
        }
        val start = startDate?.atStartOfDay(zone)?.toInstant()?.toEpochMilli() ?: 0L
        val end = endDate?.plusDays(1)?.atStartOfDay(zone)?.toInstant()?.toEpochMilli()?.minus(1)
            ?: Long.MAX_VALUE
        return start..end
    }
}
