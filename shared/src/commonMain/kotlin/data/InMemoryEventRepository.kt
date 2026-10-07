package data

import kotlinx.datetime.LocalDate

class InMemoryEventRepository : EventRepository {
    private val events = mutableListOf<Event>()

    override fun getAllEvents(): List<Event> = events.toList()

    override fun getEventsForDate(date: LocalDate): List<Event> =
        events.filter { it.date == date }
            .sortedBy { it.startTime }

    override fun addEvent(event: Event) {
        events.add(event)
    }

    override fun deleteEvent(eventId: String) {
        events.removeAll { it.id == eventId }
    }
}
