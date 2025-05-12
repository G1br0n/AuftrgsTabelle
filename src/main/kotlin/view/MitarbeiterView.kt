package view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import elemente.GrayIconButton
import models.Person
import viewModel.MitarbeiterViewModel

//M1 -------------------------  MitarbeiterView -------------------------
@Composable
fun MitarbeiterView(
    viewModel: MitarbeiterViewModel = remember { MitarbeiterViewModel() }
) {
    val personen by viewModel.personenFlow.collectAsState(initial = emptyList())
    var selected by remember { mutableStateOf<Person?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var bemerkungText by remember { mutableStateOf("") }

    LaunchedEffect(selected) {
        bemerkungText = selected?.bemerkung.orEmpty()
    }









    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Add button above both columns
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            GrayIconButton(
                icon = Icons.Filled.Person,
                label = "Mitarbeiter hinzufügen",
                tooltip = "Neuen Mitarbeiter hinzufügen",
                selected = false,
                onClick = {
                    selected = null
                    showForm = true
                }
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxSize()) {
            // Left: employee list
            Column(Modifier.weight(3f)) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(personen) { index, p ->
                        Button(
                            onClick = {
                                selected = p
                                showForm = false
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF555555)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 0.dp)
                        ) {
                            Text(
                                text = "${index + 1}. ${p.vorname.orEmpty()} ${p.name.orEmpty()} (${p.firma.orEmpty()})",
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

            // Right: form or details
            Column(Modifier.weight(4f)) {
                if (showForm) {
                    MitarbeiterForm(
                        initial = selected,
                        onSave = { person ->
                            if (selected == null) {
                                viewModel.addPerson(
                                    vorname   = person.vorname.orEmpty(),
                                    name      = person.name.orEmpty(),
                                    firma     = person.firma,
                                    bemerkung = person.bemerkung
                                )
                            } else {
                                viewModel.updatePerson(
                                    id        = person.id,
                                    vorname   = person.vorname.orEmpty(),
                                    name      = person.name.orEmpty(),
                                    firma     = person.firma,
                                    bemerkung = person.bemerkung
                                )
                            }
                            showForm = false
                        },
                        onDelete = {
                            selected?.id?.let { viewModel.deletePerson(it) }
                            showForm = false
                        },
                        onCancel = { showForm = false }
                    )
                } else {
                    selected?.let { p ->
                        Text("Bemerkung: ${p.vorname} ${p.name}", style = MaterialTheme.typography.h6)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bemerkungText,
                            onValueChange = { newVal ->
                                bemerkungText = newVal
                                viewModel.updatePerson(
                                    id        = p.id,
                                    vorname   = p.vorname.orEmpty(),
                                    name      = p.name.orEmpty(),
                                    firma     = p.firma,
                                    bemerkung = newVal.takeIf { it.isNotBlank() }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(0.dp),
                            label = { Text("Bemerkung bearbeiten") },
                            singleLine = false,
                            maxLines = Int.MAX_VALUE
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GrayIconButton(
                                icon = Icons.Filled.Edit,
                                label = "Mitarbeiter bearbeiten",
                                tooltip = "Mitarbeiter bearbeiten",
                                selected = false,
                                onClick = {
                                    showForm = true
                                }
                            )
                        }
                    } ?: Text("Wählen Sie einen Mitarbeiter aus", style = MaterialTheme.typography.body1)
                }
            }
        }
    }
}


//M1 -------------------------  MitarbeiterForm() -------------------------
@Composable
fun MitarbeiterForm(
    initial: Person?,
    onSave: (Person) -> Unit,
    onDelete: (() -> Unit)?,
    onCancel: () -> Unit
) {
    var vorname by remember { mutableStateOf(initial?.vorname ?: "") }
    var name by remember { mutableStateOf(initial?.name    ?: "") }
    var firma by remember { mutableStateOf(initial?.firma   ?: "") }
    var bemerkung by remember { mutableStateOf(initial?.bemerkung ?: "") }
    var showConfirm by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            if (initial == null) "Neuen Mitarbeiter hinzufügen"
            else "Mitarbeiter bearbeiten",
            style = MaterialTheme.typography.h6
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = vorname,
            onValueChange = { vorname = it },
            label = { Text("Vorname") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nachname") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = firma,
            onValueChange = { firma = it },
            label = { Text("Firma") },
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
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            initial?.let {
                Button(
                    onClick = { showConfirm = true },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                ) {
                    Text("Löschen", color = Color.White)
                }
            }
            Button(onClick = onCancel) {
                Text("Abbrechen")
            }
            Button(onClick = {
                onSave(
                    Person(
                        initial?.id ?: java.util.UUID.randomUUID().toString(),
                        vorname,
                        name,
                        firma.takeIf { it.isNotBlank() },
                        bemerkung.takeIf { it.isNotBlank() }
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
            text = { Text("Möchten Sie diesen Mitarbeiter wirklich löschen?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete?.invoke()
                    showConfirm = false
                }) {
                    Text("Ja")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Nein")
                }
            }
        )
    }
}