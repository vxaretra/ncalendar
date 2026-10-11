package assistant

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import data.Event
import data.EventRepository
import data.generateId
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * The calendar tools exposed to the AI assistant.
 *
 * Reads go straight to [EventRepository]. Writes ask the user for confirmation first via
 * [CalendarConfirmationHandler.awaitDecision]; when the user denies, the tool reports the
 * refusal to the model instead of mutating the repository.
 */
class CalendarToolSet(
    private val events: EventRepository,
    private val confirmation: CalendarConfirmationHandler,
) : ToolSet {

    @Tool
    @LLMDescription(
        "List the user's calendar events between two dates (inclusive). " +
            "Use this whenever the user asks about their schedule or wants to find an event."
    )
    suspend fun getEvents(
        @LLMDescription("Start date in YYYY-MM-DD format.") startDate: String,
        @LLMDescription("End date in YYYY-MM-DD format.") endDate: String,
    ): String {
        val start = parseDate(startDate) ?: return "Error: invalid start date '$startDate', expected YYYY-MM-DD."
        val end = parseDate(endDate) ?: return "Error: invalid end date '$endDate', expected YYYY-MM-DD."
        val matches = events.events.value
            .filter { it.date in start..end }
            .sortedWith(compareBy({ it.date }, { it.startTime }))
        if (matches.isEmpty()) return "No events between $startDate and $endDate."
        return matches.joinToString("\n") { it.describe() }
    }

    @Tool
    @LLMDescription(
        "Create a new calendar event. The user is asked to confirm before it is saved, " +
            "so call this even when the user sounds definitive."
    )
    suspend fun addEvent(
        @LLMDescription("Event title.") title: String,
        @LLMDescription("Date in YYYY-MM-DD format.") date: String,
        @LLMDescription("Start time in 24-hour HH:MM format.") startTime: String,
        @LLMDescription("End time in 24-hour HH:MM format.") endTime: String,
        @LLMDescription("Optional description or notes.") description: String? = null,
    ): String {
        val newDate = parseDate(date) ?: return "Error: invalid date '$date', expected YYYY-MM-DD."
        val start = parseTime(startTime) ?: return "Error: invalid start time '$startTime', expected HH:MM."
        val end = parseTime(endTime) ?: return "Error: invalid end time '$endTime', expected HH:MM."
        if (end <= start) return "Error: end time must be after start time."

        val approved = confirmation.awaitDecision(
            toolName = "addEvent",
            description = "Add \"$title\" on $newDate from $start to $end?",
        )
        if (!approved) return "The user declined to add this event. Do not retry unless they ask again."

        val event = Event(
            id = generateId(),
            title = title,
            date = newDate,
            startTime = start,
            endTime = end,
            description = description?.takeIf { it.isNotBlank() },
        )
        events.addEvent(event)
        return "Created \"$title\" on $newDate $start-$end (id=${event.id})."
    }

    @Tool
    @LLMDescription(
        "Change fields of an existing event by id. The user is asked to confirm before the " +
            "change is saved. Only pass the fields the user asked to change."
    )
    suspend fun updateEvent(
        @LLMDescription("Id of the event to change, as returned by getEvents.") id: String,
        @LLMDescription("New title, if it changes.") title: String? = null,
        @LLMDescription("New date in YYYY-MM-DD format, if it changes.") date: String? = null,
        @LLMDescription("New start time in HH:MM, if it changes.") startTime: String? = null,
        @LLMDescription("New end time in HH:MM, if it changes.") endTime: String? = null,
        @LLMDescription("New description, if it changes.") description: String? = null,
    ): String {
        val existing = events.events.value.firstOrNull { it.id == id }
            ?: return "Error: no event with id=$id. Use getEvents to find the right id."

        val newDate = if (date != null) {
            parseDate(date) ?: return "Error: invalid date '$date', expected YYYY-MM-DD."
        } else {
            existing.date
        }
        val start = if (startTime != null) {
            parseTime(startTime) ?: return "Error: invalid start time '$startTime', expected HH:MM."
        } else {
            existing.startTime
        }
        val end = if (endTime != null) {
            parseTime(endTime) ?: return "Error: invalid end time '$endTime', expected HH:MM."
        } else {
            existing.endTime
        }
        if (end <= start) return "Error: end time must be after start time."

        val approved = confirmation.awaitDecision(
            toolName = "updateEvent",
            description = "Update \"${existing.title}\" to " +
                "\"${title ?: existing.title}\" on $newDate from $start to $end?",
        )
        if (!approved) return "The user declined this change. Do not retry unless they ask again."

        events.updateEvent(
            existing.copy(
                title = title?.takeIf { it.isNotBlank() } ?: existing.title,
                date = newDate,
                startTime = start,
                endTime = end,
                description = description ?: existing.description,
            )
        )
        return "Updated \"${title ?: existing.title}\" (id=$id)."
    }

    @Tool
    @LLMDescription("Delete an event by id. The user is asked to confirm before it is deleted.")
    suspend fun deleteEvent(
        @LLMDescription("Id of the event to delete, as returned by getEvents.") id: String,
    ): String {
        val existing = events.events.value.firstOrNull { it.id == id }
            ?: return "Error: no event with id=$id. Use getEvents to find the right id."

        val approved = confirmation.awaitDecision(
            toolName = "deleteEvent",
            description = "Delete \"${existing.title}\" on ${existing.date}?",
        )
        if (!approved) return "The user declined to delete this event. Do not retry unless they ask again."

        events.deleteEvent(id)
        return "Deleted \"${existing.title}\"."
    }
}

private fun parseDate(value: String): LocalDate? =
    runCatching { LocalDate.parse(value) }.getOrNull()

private fun parseTime(value: String): LocalTime? =
    runCatching { LocalTime.parse(value) }.getOrNull()

private fun Event.describe(): String =
    "- id=$id | $title | $date $startTime-$endTime" +
        (description?.let { " | $it" } ?: "")
