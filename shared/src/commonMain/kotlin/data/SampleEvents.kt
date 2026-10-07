package data

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

fun sampleEvents(): List<Event> = listOf(
    Event("1", "Team Standup", LocalDate(2026, 10, 7),
        LocalTime(9, 0), LocalTime(9, 30), "Daily sync"),
    Event("2", "Design Review", LocalDate(2026, 10, 7),
        LocalTime(14, 0), LocalTime(15, 0)),
    Event("3", "1:1 with Manager", LocalDate(2026, 10, 8),
        LocalTime(11, 0), LocalTime(11, 30))
)

fun createSampleRepository(): EventRepository =
    InMemoryEventRepository().apply {
        sampleEvents().forEach { addEvent(it) }
    }
