package ui.form

import kotlinx.datetime.LocalTime

data class EventFormState(
    val id: String? = null,
    val title: String = "",
    val startTime: LocalTime = LocalTime(9, 0),
    val endTime: LocalTime = LocalTime(10, 0),
    val description: String = "",
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
