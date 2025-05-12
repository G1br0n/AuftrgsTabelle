@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package elemente

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.*
import java.time.format.DateTimeFormatter

@ExperimentalMaterial3Api
@Composable
fun DatePickerField(
    label: String,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    // Verwende UTC, um Zeitzonen-Verschiebungen zu vermeiden
    val initialMillis = selectedDate
        ?.atStartOfDay(ZoneOffset.UTC)
        ?.toInstant()
        ?.toEpochMilli()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )
    var showDialog by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

    OutlinedTextField(
        value = selectedDate?.format(formatter) ?: "",
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.DateRange, contentDescription = "Datum Auswählen")
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    )

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // Rückkonvertierung in UTC
                        val ld = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        onDateSelected(ld)
                    }
                    showDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Abbrechen") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun TimePickerField(
    label: String,
    selectedTime: LocalTime?,
    onTimeSelected: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    colors: TimePickerColors = TimePickerDefaults.colors(),
    layoutType: TimePickerLayoutType = TimePickerLayoutType.Vertical
) {
    val current = selectedTime ?: LocalTime.now()
    val timePickerState = rememberTimePickerState(
        initialHour = current.hour,
        initialMinute = current.minute,
        is24Hour = true
    )
    var showDialog by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    OutlinedTextField(
        value = selectedTime?.format(formatter) ?: "",
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Select time")
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    onTimeSelected(
                        LocalTime.of(
                            timePickerState.hour,
                            timePickerState.minute
                        )
                    )
                    showDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Abbrechen") }
            },
            text = {
                TimePicker(
                    state = timePickerState,
                    modifier = Modifier.fillMaxWidth(),
                    colors = colors,
                    layoutType = layoutType
                )
            }
        )
    }
}

@Composable
fun DateTimePickerField(
    label: String,
    initialDateTime: LocalDateTime?,
    onDateTimeSelected: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier
) {
    var date by remember { mutableStateOf(initialDateTime?.toLocalDate()) }
    var time by remember { mutableStateOf(initialDateTime?.toLocalTime()) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm") }

    OutlinedTextField(
        value = if (date != null && time != null)
            LocalDateTime.of(date, time).format(formatter)
        else "",
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        trailingIcon = {
            Row {
                IconButton(onClick = { date = null }) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                }
                IconButton(onClick = { time = null }) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    )

    LaunchedEffect(date, time) {
        if (date != null && time != null) {
            onDateTimeSelected(LocalDateTime.of(date, time))
        }
    }
}
