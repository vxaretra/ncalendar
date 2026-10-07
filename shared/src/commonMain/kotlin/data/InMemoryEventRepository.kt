package data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InMemoryEventRepository : EventRepository {
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    override val events: StateFlow<List<Event>> = _events.asStateFlow()

    override suspend fun addEvent(event: Event) {
        _events.update { current ->
            (current + event).sortedBy { it.startTime }
        }
    }

    override suspend fun updateEvent(event: Event) {
        _events.update { current ->
            current.map { if (it.id == event.id) event else it }
                .sortedBy { it.startTime }
        }
    }

    override suspend fun deleteEvent(eventId: String) {
        _events.update { current ->
            current.filter { it.id != eventId }
        }
    }
}
