package ui

import kotlinx.datetime.LocalDate

fun formatDate(date: LocalDate): String {
    val dow = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
    val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$dow, ${date.dayOfMonth} $month ${date.year}"
}
