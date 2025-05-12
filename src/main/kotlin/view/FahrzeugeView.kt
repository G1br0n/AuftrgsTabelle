package view


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import viewModel.FahrzeugeViewModel
import elemente.GrayIconButton
import models.Fahrzeug
import java.util.*

@Composable
fun FahrzeugeView(
    viewModel: FahrzeugeViewModel = remember { FahrzeugeViewModel() }
) {
    val fahrzeuge by viewModel.fahrzeugeFlow.collectAsState(initial = emptyList())
    var selected by remember { mutableStateOf<Fahrzeug?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var bezeichnungText by remember { mutableStateOf("") }
    var kennzeichenText by remember { mutableStateOf("") }
    var bemerkungText by remember { mutableStateOf("") }

    // Sync form fields when selection changes
    LaunchedEffect(selected) {
        bezeichnungText = selected?.bezeichnung.orEmpty()
        kennzeichenText = selected?.kennzeichen.orEmpty()
        bemerkungText = selected?.bemerkung.orEmpty()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Add button
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            GrayIconButton(
                icon = Icons.Filled.Edit,
                label = "Fahrzeug hinzufügen",
                tooltip = "Neues Fahrzeug hinzufügen",
                selected = false,
                onClick = {
                    selected = null
                    showForm = true
                }
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxSize()) {
            // List
            Column(Modifier.weight(3f)) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    itemsIndexed(fahrzeuge) { index, f ->
                        Button(
                            onClick = {
                                selected = f
                                showForm = false
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF555555)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 0.dp)
                        ) {
                            Text(
                                text = "${index + 1}. ${f.bezeichnung.orEmpty()} (${f.kennzeichen.orEmpty()})",
                                fontSize = 16.sp,
                                color = Color.White,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            // Detail/Form
            Column(Modifier.weight(4f)) {
                if (showForm) {
                    FahrzeugeForm(
                        initial = selected,
                        onSave = { fahrzeug ->
                            if (selected == null) {
                                viewModel.addFahrzeug(
                                    bezeichnung = fahrzeug.bezeichnung.orEmpty(),
                                    kennzeichen = fahrzeug.kennzeichen.orEmpty(),
                                    bemerkung = fahrzeug.bemerkung
                                )
                            } else {
                                viewModel.updateFahrzeug(
                                    id = fahrzeug.id,
                                    bezeichnung = fahrzeug.bezeichnung.orEmpty(),
                                    kennzeichen = fahrzeug.kennzeichen.orEmpty(),
                                    bemerkung = fahrzeug.bemerkung
                                )
                            }
                            showForm = false
                        },
                        onDelete = {
                            selected?.id?.let { viewModel.deleteFahrzeug(it) }
                            showForm = false
                        },
                        onCancel = { showForm = false }
                    )
                } else {
                    selected?.let { f ->
                        Text("Bemerkung: ${f.bezeichnung} ${f.kennzeichen}", style = MaterialTheme.typography.h6)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bemerkungText,
                            onValueChange = { new ->
                                bemerkungText = new
                                viewModel.updateFahrzeug(
                                    id = f.id,
                                    bezeichnung = f.bezeichnung.orEmpty(),
                                    kennzeichen = f.kennzeichen.orEmpty(),
                                    bemerkung = new.takeIf { it.isNotBlank() }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            label = { Text("Bemerkung bearbeiten") }
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GrayIconButton(
                                icon = Icons.Filled.Edit,
                                label = "Fahrzeug bearbeiten",
                                tooltip = "Fahrzeug bearbeiten",
                                selected = false,
                                onClick = { showForm = true }
                            )
                        }
                    } ?: Text("Wählen Sie ein Fahrzeug aus", style = MaterialTheme.typography.body1)
                }
            }
        }
    }
}

@Composable
fun FahrzeugeForm(
    initial: Fahrzeug?,
    onSave: (Fahrzeug) -> Unit,
    onDelete: (() -> Unit)?,
    onCancel: () -> Unit
) {
    var bezeichnung by remember { mutableStateOf(initial?.bezeichnung.orEmpty()) }
    var kennzeichen by remember { mutableStateOf(initial?.kennzeichen.orEmpty()) }
    var bemerkung by remember { mutableStateOf(initial?.bemerkung.orEmpty()) }
    var showConfirm by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            if (initial == null) "Neues Fahrzeug hinzufügen" else "Fahrzeug bearbeiten",
            style = MaterialTheme.typography.h6
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = bezeichnung,
            onValueChange = { bezeichnung = it },
            label = { Text("Bezeichnung") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = kennzeichen,
            onValueChange = { kennzeichen = it },
            label = { Text("Kennzeichen") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = bemerkung,
            onValueChange = { bemerkung = it },
            label = { Text("Bemerkung") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            initial?.let {
                Button(onClick = { showConfirm = true }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Text("Löschen", color = Color.White)
                }
            }
            Button(onClick = onCancel) { Text("Abbrechen") }
            Button(onClick = {
                onSave(
                    Fahrzeug(
                        id = initial?.id ?: UUID.randomUUID().toString(),
                        bezeichnung = bezeichnung,
                        kennzeichen = kennzeichen,
                        bemerkung = bemerkung.takeIf { it.isNotBlank() }
                    )
                )
            }) {
                Text("Speichern")
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Bestätigung") },
            text = { Text("Möchten Sie dieses Fahrzeug wirklich löschen?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete?.invoke()
                    showConfirm = false
                }) { Text("Ja") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Nein") }
            }
        )
    }
}

