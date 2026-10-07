package ui

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

fun formatDate(date: LocalDate): String {
    val dow = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
    val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$dow, ${date.dayOfMonth} $month ${date.year}"
}

/** Formats a time as `HH:mm` (24h clock), e.g. `09:00`. */
fun formatTime(time: LocalTime): String =
    "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
