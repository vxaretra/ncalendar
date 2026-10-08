package ui.form

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class EventFormState(
    val id: String? = null,
    val title: String = "",
    val date: LocalDate = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()).date,
    val startTime: LocalTime = LocalTime(9, 0),
    val endTime: LocalTime = LocalTime(10, 0),
    val description: String = "",
    /** Minutes before the event start, furthest-out first. Empty = no reminders. */
    val reminderOffsets: List<Int> = emptyList(),
    val titleError: String? = null,
    val timeError: String? = null
) {
    val isEditing: Boolean get() = id != null

    fun validate(): EventFormState {
        val titleErr = if (title.isBlank()) "Title is required" else null
        val timeErr = if (endTime <= startTime) "End time must be after start time" else null
        return copy(titleError = titleErr, timeError = timeErr)
    }

    val isValid: Boolean get() = titleError == null && timeError == null
}
