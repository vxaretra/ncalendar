package data.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import data.Event
import data.EventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class SqlDelightEventRepository(
    private val queries: EventQueries,
    private val scope: CoroutineScope
) : EventRepository {

    override val events: StateFlow<List<Event>> =
        queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { entities ->
                entities.map { it.toEvent() }
                    .sortedBy { it.startTime }
            }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    // One-shot read for bootstrap tasks such as seeding.
    // The events flow above stays the single source of truth for the UI.
    suspend fun getAllEventsOnce(): List<Event> =
        queries.selectAll().executeAsList().map { it.toEvent() }

    override suspend fun addEvent(event: Event) {
        queries.insert(
            id = event.id,
            title = event.title,
            date = event.date.toString(),
            start_time = event.startTime.toString(),
            end_time = event.endTime.toString(),
            description = event.description
        )
    }

    override suspend fun updateEvent(event: Event) {
        queries.update(
            title = event.title,
            date = event.date.toString(),
            start_time = event.startTime.toString(),
            end_time = event.endTime.toString(),
            description = event.description,
            id = event.id
        )
    }

    override suspend fun deleteEvent(eventId: String) {
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
