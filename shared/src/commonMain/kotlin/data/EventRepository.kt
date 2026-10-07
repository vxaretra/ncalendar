package data

import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

interface EventRepository {
    val events: StateFlow<List<Event>>
    suspend fun addEvent(event: Event)
    suspend fun updateEvent(event: Event)
    suspend fun deleteEvent(eventId: String)

    /**
     * Dates within [month] that have at least one event.
     *
     * Part of the shared calendar data API: the calendar consults event dates to decide
     * which day cells show an event dot. Reads the latest snapshot from [events], so it
     * should be called from a composition/`remember` keyed on the collected event list.
     */
    fun eventDatesInMonth(month: YearMonth): Set<LocalDate> =
        events.value.asSequence()
            .map { it.date }
            .filter { it.year == month.year && it.month == month.month }
            .toSet()

    /**
     * All dates that currently have at least one event.
     *
     * The vertical calendar renders many months at once, so the dots are computed from
     * this full set rather than from a single month.
     */
    fun allEventDates(): Set<LocalDate> =
        events.value.mapTo(mutableSetOf()) { it.date }
}
