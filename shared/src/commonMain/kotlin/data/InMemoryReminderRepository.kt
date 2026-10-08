package data

import reminder.normalizeReminderOffsets
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * In-memory reminder store for previews and tests.
 *
 * Unlike [SqlDelightReminderRepository] it has no event table to join against, so it stores
 * a snapshot of the event alongside each reminder. [setRemindersForEvent] refreshes that
 * snapshot, which keeps due-time computation correct after start-time edits.
 */
class InMemoryReminderRepository(
    private val clock: () -> Instant = { Clock.System.now() },
) : ReminderRepository {

    private data class Entry(val reminder: Reminder, val event: Event)

    private val entries = mutableListOf<Entry>()
    private var nextId = 1L

    override suspend fun remindersForEvent(eventId: String): List<Reminder> =
        entries.filter { it.reminder.eventId == eventId }
            .map { it.reminder }
            .sortedByDescending { it.offsetMinutes }

    override suspend fun setRemindersForEvent(event: Event, offsetMinutes: List<Int>) {
        val desired = normalizeReminderOffsets(offsetMinutes)
        entries.removeAll { it.reminder.eventId == event.id && it.reminder.offsetMinutes !in desired }
        val existingOffsets = entries.filter { it.reminder.eventId == event.id }
            .mapTo(mutableSetOf()) { it.reminder.offsetMinutes }
        val now = clock()
        val start = event.startInstant()

        desired.filterNot { it in existingOffsets }
            .forEach { offset ->
                val dueAt = start - offset.minutes
                val firedAt = if (dueAt <= now) now else null
                entries += Entry(Reminder(nextId++, event.id, offset, firedAt), event)
            }

        // Refresh snapshots so later start-time edits are reflected in due times.
        entries.replaceAll { entry ->
            if (entry.reminder.eventId == event.id) entry.copy(event = event) else entry
        }
    }

    override suspend fun dueReminders(): List<ReminderWithEvent> =
        entries.filter { it.reminder.firedAt == null }
            .map { ReminderWithEvent(it.reminder, it.event) }

    override suspend fun markFired(reminderId: Long, firedAt: Instant) {
        entries.replaceAll { entry ->
            if (entry.reminder.id == reminderId) {
                entry.copy(reminder = entry.reminder.copy(firedAt = firedAt))
            } else {
                entry
            }
        }
    }

    override suspend fun clearFiredForEvent(eventId: String) {
        entries.replaceAll { entry ->
            if (entry.reminder.eventId == eventId) {
                entry.copy(reminder = entry.reminder.copy(firedAt = null))
            } else {
                entry
            }
        }
    }
}
