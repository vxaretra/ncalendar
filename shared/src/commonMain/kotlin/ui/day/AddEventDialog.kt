package ui.day

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import reminder.MAX_REMINDERS_PER_EVENT
import reminder.ReminderUnit
import reminder.addReminderOffset
import reminder.parseOffsetMinutes
import ui.formatDate
import ui.formatTime
import ui.form.EventFormState
import ui.form.TimePickerDialog

/**
 * Add/edit event dialog (shared by both flows).
 *
 * Polish on top of the basic form:
 *  - the title field is focused as soon as the dialog opens, so typing starts immediately
 *  - Enter saves from the title field (the description can still insert newlines)
 *  - Escape cancels the dialog
 *  - explicit copy for buttons and hints
 *
 * The dialog owns create and edit mode: the title follows [EventFormState.isEditing], and
 * the reminder list sits below the time fields and above the buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    state: EventFormState,
    onStateChange: (EventFormState) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var reminderCount by remember { mutableStateOf("") }
    var reminderUnit by remember { mutableStateOf(ReminderUnit.MINUTES) }
    val titleFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        titleFocusRequester.requestFocus()
    }

    val atReminderCap = state.reminderOffsets.size >= MAX_REMINDERS_PER_EVENT
    val parsedOffset = parseOffsetMinutes(reminderCount, reminderUnit)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.onPreviewKeyEvent { event ->
            // Enter is handled by the title field so the description keeps working.
            if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                onDismiss()
                true
            } else {
                false
            }
        },
        title = {
            Text(if (state.isEditing) "Edit event" else "Add event")
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = { onStateChange(state.copy(title = it, titleError = null)) },
                    label = { Text("Title") },
                    placeholder = { Text("e.g. Team standup") },
                    isError = state.titleError != null,
                    supportingText = {
                        Text(state.titleError ?: "Press Enter to save")
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown &&
                                (event.key == Key.Enter || event.key == Key.NumPadEnter)
                            ) {
                                onConfirm()
                                true
                            } else {
                                false
                            }
                        },
                )

                DateField(
                    date = state.date,
                    onClick = { showDatePicker = true }
                )

                TimeField(
                    label = "Start",
                    time = state.startTime,
                    onClick = { showStartPicker = true }
                )

                TimeField(
                    label = "End",
                    time = state.endTime,
                    onClick = { showEndPicker = true }
                )

                if (state.timeError != null) {
                    Text(
                        text = state.timeError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                ReminderSection(
                    offsets = state.reminderOffsets,
                    reminderCount = reminderCount,
                    reminderUnit = reminderUnit,
                    atCap = atReminderCap,
                    canAdd = !atReminderCap && parsedOffset != null,
                    onCountChange = { newValue ->
                        reminderCount = newValue.filter { it.isDigit() }.take(4)
                    },
                    onUnitChange = { reminderUnit = it },
                    onQuickPick = { count, unit ->
                        reminderCount = count
                        reminderUnit = unit
                    },
                    onAdd = {
                        parsedOffset?.let { offset ->
                            onStateChange(
                                state.copy(
                                    reminderOffsets = addReminderOffset(state.reminderOffsets, offset)
                                )
                            )
                            reminderCount = ""
                        }
                    },
                    onDelete = { offset ->
                        onStateChange(
                            state.copy(reminderOffsets = state.reminderOffsets.filterNot { it == offset })
                        )
                    },
                )

                OutlinedTextField(
                    value = state.description,
                    onValueChange = { onStateChange(state.copy(description = it)) },
                    label = { Text("Description (optional)") },
                    placeholder = { Text("Notes, location, agenda…") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(if (state.isEditing) "Save changes" else "Add event")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.toEpochDays() * MILLIS_PER_DAY
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onStateChange(state.copy(date = LocalDate.fromEpochDays((millis / MILLIS_PER_DAY).toInt())))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showStartPicker) {
        TimePickerDialog(
            title = "Select start time",
            initialTime = state.startTime,
            onConfirm = {
                onStateChange(state.copy(startTime = it, timeError = null))
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false }
        )
    }

    if (showEndPicker) {
        TimePickerDialog(
            title = "Select end time",
            initialTime = state.endTime,
            onConfirm = {
                onStateChange(state.copy(endTime = it, timeError = null))
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false }
        )
    }
}

/**
 * Reminder editor: current rows, quick-pick chips and the number + unit input row.
 *
 * The cap is refused here (the add control disables), duplicates are silently absorbed by
 * [addReminderOffset], and rows are always ordered furthest-out first.
 */
@Composable
private fun ReminderSection(
    offsets: List<Int>,
    reminderCount: String,
    reminderUnit: ReminderUnit,
    atCap: Boolean,
    canAdd: Boolean,
    onCountChange: (String) -> Unit,
    onUnitChange: (ReminderUnit) -> Unit,
    onQuickPick: (String, ReminderUnit) -> Unit,
    onAdd: () -> Unit,
    onDelete: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Reminders", style = MaterialTheme.typography.titleSmall)

        offsets.forEach { offset ->
            ReminderRow(offsetMinutes = offset, onDelete = { onDelete(offset) })
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickPick("10 min", "10", ReminderUnit.MINUTES, onQuickPick)
            QuickPick("30 min", "30", ReminderUnit.MINUTES, onQuickPick)
            QuickPick("1 hour", "1", ReminderUnit.HOURS, onQuickPick)
            QuickPick("1 day", "1", ReminderUnit.DAYS, onQuickPick)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = reminderCount,
                onValueChange = onCountChange,
                label = { Text("Remind me") },
                placeholder = { Text("10") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            Box {
                var expanded by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { expanded = true }) {
                    Text(reminderUnit.label)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    ReminderUnit.entries.forEach { unit ->
                        DropdownMenuItem(
                            text = { Text(unit.label) },
                            onClick = {
                                onUnitChange(unit)
                                expanded = false
                            },
                        )
                    }
                }
            }

            IconButton(onClick = onAdd, enabled = canAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add reminder")
            }
        }

        if (atCap) {
            Text(
                text = "Maximum of $MAX_REMINDERS_PER_EVENT reminders per event.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuickPick(
    label: String,
    count: String,
    unit: ReminderUnit,
    onQuickPick: (String, ReminderUnit) -> Unit,
) {
    AssistChip(
        onClick = { onQuickPick(count, unit) },
        label = { Text(label) },
    )
}

@Composable
private fun DateField(
    date: LocalDate,
    onClick: () -> Unit,
) {
    val formattedDate = formatDate(date)
    OutlinedTextField(
        value = formattedDate,
        onValueChange = {},
        readOnly = true,
        label = { Text("Date") },
        enabled = true,
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        trailingIcon = {
            IconButton(onClick = onClick) {
                Icon(Icons.Default.DateRange, contentDescription = "Select date")
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TimeField(
    label: String,
    time: LocalTime,
    onClick: () -> Unit,
) {
    val formattedTime = formatTime(time)
    OutlinedTextField(
        value = formattedTime,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        enabled = true,
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        trailingIcon = {
            IconButton(onClick = onClick) {
                Icon(Icons.Default.Timer, contentDescription = "Select time")
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

private const val MILLIS_PER_DAY = 86_400_000L
