package data.db

import data.Event
import data.EventRepository
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class SqlDelightEventRepository(
    private val database: AppDatabase
) : EventRepository {

    private val queries = database.eventQueries

    override fun getAllEvents(): List<Event> =
        queries.selectAll().executeAsList().map { it.toEvent() }

    override fun getEventsForDate(date: LocalDate): List<Event> =
        queries.selectByDate(date.toString()).executeAsList().map { it.toEvent() }

    override fun addEvent(event: Event) {
        queries.insert(
            id = event.id,
            title = event.title,
            date = event.date.toString(),
            start_time = event.startTime.toString(),
            end_time = event.endTime.toString(),
            description = event.description
        )
    }

    override fun updateEvent(event: Event) {
        queries.update(
            title = event.title,
            date = event.date.toString(),
            start_time = event.startTime.toString(),
            end_time = event.endTime.toString(),
            description = event.description,
            id = event.id
        )
    }

    override fun deleteEvent(eventId: String) {
        queries.deleteById(eventId)
    }
}

private fun data.db.EventEntity.toEvent(): Event =
    Event(
        id = id,
        title = title,
        date = LocalDate.parse(date),
        startTime = LocalTime.parse(start_time),
        endTime = LocalTime.parse(end_time),
        description = description
    )
