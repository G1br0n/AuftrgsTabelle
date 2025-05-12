package view



import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import elemente.GrayIconButton
import models.Material
import viewModel.MaterialViewModel

@Composable
fun MaterialView(
    viewModel: MaterialViewModel = remember { MaterialViewModel() }
) {
    val materials by viewModel.materialFlow.collectAsState(initial = emptyList())
    var selected by remember { mutableStateOf<Material?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var bezeichnungText by remember { mutableStateOf("") }
    var bemerkungText by remember { mutableStateOf("") }

    LaunchedEffect(selected) {
        bezeichnungText = selected?.bezeichnung.orEmpty()
        bemerkungText = selected?.bemerkung.orEmpty()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            GrayIconButton(
                icon = Icons.Filled.Build,
                label = "Material hinzufügen",
                tooltip = "Neues Material hinzufügen",
                selected = false,
                onClick = {
                    selected = null
                    showForm = true
                }
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.weight(3f)) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    itemsIndexed(materials) { index, m ->
                        Button(
                            onClick = { selected = m; showForm = false },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF555555)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${index+1}. ${m.bezeichnung.orEmpty()}",
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
            Column(Modifier.weight(4f)) {
                if (showForm) {
                    MaterialForm(
                        initial = selected,
                        onSave = { mat ->
                            if (selected == null) viewModel.addMaterial(mat.bezeichnung.orEmpty(), mat.bemerkung)
                            else viewModel.updateMaterial(mat.id, mat.bezeichnung.orEmpty(), mat.bemerkung)
                            showForm = false
                        },
                        onDelete = {
                            selected?.id?.let { viewModel.deleteMaterial(it) }
                            showForm = false
                        },
                        onCancel = { showForm = false }
                    )
                } else {
                    selected?.let { m ->
                        Text("Bemerkung: ${m.bezeichnung}", style = MaterialTheme.typography.h6)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bemerkungText,
                            onValueChange = { new ->
                                bemerkungText = new
                                viewModel.updateMaterial(
                                    id = m.id,
                                    bezeichnung = m.bezeichnung.orEmpty(),
                                    bemerkung = new.takeIf { it.isNotBlank() }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            label = { Text("Bemerkung bearbeiten") }
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GrayIconButton(
                                icon = Icons.Filled.Edit,
                                label = "Material bearbeiten",
                                tooltip = "Material bearbeiten",
                                selected = false,
                                onClick = { showForm = true }
                            )
                        }
                    } ?: Text("Wählen Sie ein Material aus", style = MaterialTheme.typography.body1)
                }
            }
        }
    }
}

@Composable
fun MaterialForm(
    initial: Material?,
    onSave: (Material) -> Unit,
    onDelete: (() -> Unit)?,
    onCancel: () -> Unit
) {
    var bezeichnung by remember { mutableStateOf(initial?.bezeichnung.orEmpty()) }
    var bemerkung by remember { mutableStateOf(initial?.bemerkung.orEmpty()) }
    var showConfirm by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            if (initial == null) "Neues Material hinzufügen" else "Material bearbeiten",
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
            Button(onClick = { onSave(Material(id = initial?.id ?: java.util.UUID.randomUUID().toString(), bezeichnung = bezeichnung, bemerkung = bemerkung.takeIf { it.isNotBlank() })) }) {
                Text("Speichern")
            }
        }
    }
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Bestätigung") },
            text = { Text("Möchten Sie dieses Material wirklich löschen?") },
            confirmButton = {
                TextButton(onClick = { onDelete?.invoke(); showConfirm = false }) { Text("Ja") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Nein") }
            }
        )
    }
}