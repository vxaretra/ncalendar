package data.db

import data.sampleEvents
import kotlinx.coroutines.runBlocking

fun seedIfEmpty(repository: SqlDelightEventRepository) {
    runBlocking {
        if (repository.getAllEventsOnce().isEmpty()) {
            sampleEvents().forEach { repository.addEvent(it) }
        }
    }
}
