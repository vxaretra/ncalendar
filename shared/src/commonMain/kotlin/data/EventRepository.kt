package data

import kotlinx.datetime.LocalDate

interface EventRepository {
    fun getAllEvents(): List<Event>
    fun getEventsForDate(date: LocalDate): List<Event>
    fun addEvent(event: Event)
    fun updateEvent(event: Event)
    fun deleteEvent(eventId: String)
}
