package ui.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalTime

@Composable
fun EventFormDialog(
    state: EventFormState,
    onStateChange: (EventFormState) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (state.isEditing) "Edit Event" else "New Event")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                OutlinedTextField(
                    value = state.title,
                    onValueChange = { onStateChange(state.copy(title = it, titleError = null)) },
                    label = { Text("Title") },
                    isError = state.titleError != null,
                    supportingText = state.titleError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
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
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Save")
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
    onClick: () -> Unit
) {
    val formattedTime = "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
