package data

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class Event(
    val id: String,
    val title: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val description: String? = null
)
