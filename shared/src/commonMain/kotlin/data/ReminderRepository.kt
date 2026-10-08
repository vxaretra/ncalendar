package data

import data.db.ReminderEntity
import data.db.ReminderQueries
import data.db.SelectDue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import reminder.normalizeReminderOffsets
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * A reminder attached to an event, stored as an offset in minutes before the event start.
 *
 * [firedAt] is internal bookkeeping for the scheduler: it prevents the same reminder from
 * being fired twice and is never surfaced in the UI.
 */
data class Reminder(
    val id: Long,
    val eventId: String,
    val offsetMinutes: Int,
    val firedAt: Instant?,
) {
    val fired: Boolean get() = firedAt != null
}

/** A pending reminder joined with the event it belongs to. */
data class ReminderWithEvent(
    val reminder: Reminder,
    val event: Event,
)

/** The instant an event starts, using the current system time zone. */
fun Event.startInstant(timeZone: TimeZone = TimeZone.currentSystemDefault()): Instant =
    LocalDateTime(date, startTime).toInstant(timeZone)

interface ReminderRepository {
    /** All reminders of an event, furthest-out first. */
    suspend fun remindersForEvent(eventId: String): List<Reminder>

    /**
     * Replaces the event's reminders with [offsetMinutes].
     *
     * Duplicates are silently deduped and the list is capped; reminders already stored for
     * the same offset keep their [Reminder.firedAt]. Offsets whose due time has already
     * passed are stored as fired so creating an event with a reminder that can no longer
     * happen never produces an in-the-past notification.
     */
    suspend fun setRemindersForEvent(event: Event, offsetMinutes: List<Int>)

    /** Pending reminders (firedAt IS NULL) joined with their events. */
    suspend fun dueReminders(): List<ReminderWithEvent>

    suspend fun markFired(reminderId: Long, firedAt: Instant)

    /** Clears fired bookkeeping so the reminders can fire again after a start-time edit. */
    suspend fun clearFiredForEvent(eventId: String)
}

class SqlDelightReminderRepository(
    private val reminderQueries: ReminderQueries,
    private val clock: () -> Instant = { Clock.System.now() },
) : ReminderRepository {

    override suspend fun remindersForEvent(eventId: String): List<Reminder> =
        reminderQueries.selectForEvent(eventId)
            .executeAsList()
            .map { it.toReminder() }
            .sortedByDescending { it.offsetMinutes }

    override suspend fun setRemindersForEvent(event: Event, offsetMinutes: List<Int>) {
        val desired = normalizeReminderOffsets(offsetMinutes)
        val existing = reminderQueries.selectForEvent(event.id).executeAsList()
        val existingOffsets = existing.mapTo(mutableSetOf()) { it.offsetMinutes.toInt() }
        val now = clock()
        val start = event.startInstant()

        reminderQueries.transaction {
            existing.filterNot { it.offsetMinutes.toInt() in desired }
                .forEach { reminderQueries.deleteById(it.id) }
            desired.filterNot { it in existingOffsets }
                .forEach { offset ->
                    val dueAt = start - offset.minutes
                    // A reminder that is already due on create never notifies.
                    val firedAt = if (dueAt <= now) now.toEpochMilliseconds() else null
                    reminderQueries.insert(
                        eventId = event.id,
                        offsetMinutes = offset.toLong(),
                        firedAt = firedAt,
                    )
                }
        }
    }

    override suspend fun dueReminders(): List<ReminderWithEvent> =
        reminderQueries.selectDue()
            .executeAsList()
            .map { it.toReminderWithEvent() }

    override suspend fun markFired(reminderId: Long, firedAt: Instant) {
        reminderQueries.markFired(
            firedAt = firedAt.toEpochMilliseconds(),
            id = reminderId,
        )
    }

    override suspend fun clearFiredForEvent(eventId: String) {
        reminderQueries.clearFiredForEvent(eventId)
    }
}

private fun ReminderEntity.toReminder(): Reminder =
    Reminder(
        id = id,
        eventId = eventId,
        offsetMinutes = offsetMinutes.toInt(),
        firedAt = firedAt?.let { Instant.fromEpochMilliseconds(it) },
    )

private fun SelectDue.toReminderWithEvent(): ReminderWithEvent =
    ReminderWithEvent(
        reminder = Reminder(
            id = reminderId,
            eventId = eventId,
            offsetMinutes = offsetMinutes.toInt(),
            firedAt = firedAt?.let { Instant.fromEpochMilliseconds(it) },
        ),
        event = Event(
            id = eventId,
            title = title,
            date = LocalDate.parse(date),
            startTime = LocalTime.parse(startTime),
            endTime = LocalTime.parse(endTime),
            description = description,
        ),
    )
