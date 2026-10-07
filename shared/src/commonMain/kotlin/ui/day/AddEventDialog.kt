package ui.day

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalTime
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
 */
@Composable
fun AddEventDialog(
    state: EventFormState,
    onStateChange: (EventFormState) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    val titleFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        titleFocusRequester.requestFocus()
    }

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
            Text(if (state.isEditing) "Edit event" else "New event")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

@Composable
private fun TimeField(
    label: String,
    time: LocalTime,
    onClick: () -> Unit,
) {
    val formattedTime = formatTime(time)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$label time: $formattedTime" }
            .clickable(
                onClickLabel = "Change $label time",
                onClick = onClick
            )
    ) {
        OutlinedTextField(
            value = formattedTime,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
