package data.db

import data.sampleEvents

fun seedIfEmpty(repository: SqlDelightEventRepository) {
    if (repository.getAllEvents().isEmpty()) {
        sampleEvents().forEach { repository.addEvent(it) }
    }
}
