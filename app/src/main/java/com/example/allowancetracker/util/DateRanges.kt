package com.example.allowancetracker.util

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
