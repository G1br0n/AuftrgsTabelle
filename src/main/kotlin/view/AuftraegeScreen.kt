// ui/AuftraegeUI.kt
package view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import elemente.GrayIconButton
import elemente.GrayFillButton
import models.Auftrag
import models.Schicht
import viewModel.AuftraegeViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

@Composable
fun AuftraegeView(
    viewModel: AuftraegeViewModel = remember { AuftraegeViewModel() }
) {
    val auftraege by viewModel.auftraegeFlow.collectAsState(initial = emptyList())
    var selectedAuftrag by remember { mutableStateOf<Auftrag?>(null) }
    var selectedSchicht by remember { mutableStateOf<Schicht?>(null) }
    var showAuftragForm by remember { mutableStateOf(false) }
    var showSchichtForm by remember { mutableStateOf(false) }

    // Formatter
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    Row(Modifier.fillMaxSize().padding(16.dp)) {
        // 1. Auftragsliste-Column
        Column(Modifier.weight(1f).fillMaxHeight()) {
            GrayIconButton(
                icon = Icons.Filled.Add,
                label = "Auftrag hinzufügen",
                tooltip = "Neuen Auftrag hinzufügen",
                selected = false,
                onClick = {
                    selectedAuftrag = null
                    showAuftragForm = true
                }
            )
            Spacer(Modifier.height(8.dp))
            Text("Auftragsliste", style = MaterialTheme.typography.h6)
            Spacer(Modifier.height(4.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(auftraege) { idx, auftrag ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GrayFillButton(
                            icon = Icons.Filled.Notifications,
                            label = "${idx + 1}. ${auftrag.sapANummer.orEmpty()} – ${auftrag.ort.orEmpty()}",
                            tooltip = auftrag.sapANummer.orEmpty(),
                            selected = auftrag == selectedAuftrag,
                            onClick = {
                                selectedAuftrag = auftrag
                                selectedSchicht = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                        GrayIconButton(
                            icon = Icons.Filled.Edit,
                            label = "",
                            tooltip = "Auftrag bearbeiten",
                            selected = false,
                            onClick = {
                                selectedAuftrag = auftrag
                                showAuftragForm = true
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(16.dp))

        // 2. Schichtenliste-Column
        Column(Modifier.weight(1f).fillMaxHeight()) {

            selectedAuftrag?.let {
                GrayIconButton(
                    icon = Icons.Filled.Add,
                    label = "Schicht hinzufügen",
                    tooltip = "Neue Schicht hinzufügen",
                    selected = false,
                    onClick = {
                        selectedSchicht = null
                        showSchichtForm = true
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("Schichtenliste", style = MaterialTheme.typography.h6)
            Spacer(Modifier.height(4.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(selectedAuftrag?.schichten.orEmpty()) { idx, schicht ->
                    GrayFillButton(
                        icon = Icons.Filled.DateRange,
                        label = "Schicht ${idx + 1}: ${schicht.startDatum?.format(formatter).orEmpty()}",
                        tooltip = "Schicht ${idx + 1}",
                        selected = schicht == selectedSchicht,
                        onClick = { selectedSchicht = schicht }
                    )
                }
            }
        }


        /*Row {
            Column {
                Column {
                    //Text Auftrag Details
                    //SAP/A-Nummer
                    //Anzahl schichten(entschprechend dem sol die lieste mit schichten gefült werden)()
                    Row {
                        //Liferdatum von bis(Hier am besten dat picker)
                    }
                    //Ohrt
                    Row {
                        //Strecke //Km-Von //Km-Bis
                    }
                    //Massnahme
                    //Bemerkunge

                }
                Column {
                    //Text Ausfülehen NUR wen sich die schichten täglich wiederholen
                    Row {
                        //Startdatum //Start uhrzeit (ambesten data und time picker )
                    }
                    Row {
                        //Enddatum //End uhrzeit (ambesten data und time picker )
                    }
                }
                //Row aus butons
            }



        }*/

        Spacer(Modifier.width(16.dp))

        // 3. Detailview-Column
        Column(Modifier.weight(3f).fillMaxHeight()) {
            Text("Details", style = MaterialTheme.typography.h6)
            Spacer(Modifier.height(8.dp))
            selectedSchicht?.let { sch ->
                Text("Start: ${sch.startDatum?.format(formatter).orEmpty()}")
                Text("Ende: ${sch.endDatum?.format(formatter).orEmpty()}")
                Text("Ort: ${sch.ort.orEmpty()}")
                Text("Strecke: ${sch.strecke.orEmpty()}")
                Text("Km: ${sch.kmVon.orEmpty()} - ${sch.kmBis.orEmpty()}")
                Text("Maßnahme: ${sch.massnahme.orEmpty()}")
                Text("Bemerkung: ${sch.bemerkung.orEmpty()}")
                Spacer(Modifier.height(8.dp))
                GrayIconButton(
                    icon = Icons.Filled.Edit,
                    label = "Schicht bearbeiten",
                    tooltip = "Schicht bearbeiten",
                    selected = false,
                    onClick = { showSchichtForm = true }
                )
            } ?: run {
                Text("Keine Schicht ausgewählt", color = androidx.compose.ui.graphics.Color.Gray)
            }
        }
    }

    // Auftrag-Formular
    if (showAuftragForm) {
        Window(
            onCloseRequest = { showAuftragForm = false },
            title = if (selectedAuftrag == null) "Neuen Auftrag" else "Auftrag bearbeiten"
        ) {
            AuftragForm(
                initial = selectedAuftrag,
                onSave = { id, sap, ort, strecke, kmVon, kmBis, massnahme, bemerkung ->
                    val auf = Auftrag(
                        id = id ?: UUID.randomUUID().toString(),
                        sapANummer = sap,
                        startDatum = null,
                        endDatum = null,
                        ort = ort,
                        strecke = strecke,
                        kmVon = kmVon,
                        kmBis = kmBis,
                        massnahme = massnahme,
                        bemerkung = bemerkung,
                        schichten = selectedAuftrag?.schichten.orEmpty()
                    )
                    if (selectedAuftrag == null) viewModel.addAuftrag(auf)
                    else viewModel.updateAuftrag(auf)
                    showAuftragForm = false
                },
                onDelete = selectedAuftrag?.let { { viewModel.deleteAuftrag(it.id); showAuftragForm = false } },
                onCancel = { showAuftragForm = false }
            )
        }
    }

    // Schicht-Formular
    if (showSchichtForm && selectedAuftrag != null) {
        Window(
            onCloseRequest = { showSchichtForm = false },
            title = if (selectedSchicht == null) "Neue Schicht" else "Schicht bearbeiten"
        ) {
            SchichtForm(
                initial = selectedSchicht,
                formatter = formatter,
                dateFormatter = dateFormatter,
                onSave = { sch ->
                    if (selectedSchicht == null) viewModel.addSchicht(selectedAuftrag!!.id, sch)
                    else viewModel.updateSchicht(sch.id, sch)
                    showSchichtForm = false
                },
                onCancel = { showSchichtForm = false }
            )
        }
    }
}


// Hinweis: AuftragForm und SchichtForm weiterhin unverändert (Anpassung sofern nötig).
// Die Composables AuftragForm und SchichtForm bleiben unverändert


@Composable
fun AuftragForm(
    initial: Auftrag?,
    onSave: (
        id: String?,
        sapANr: String,
        ort: String,
        strecke: String,
        kmVon: String,
        kmBis: String,
        massnahme: String,
        bemerkung: String?
    ) -> Unit,
    onDelete: (() -> Unit)? = null,
    onCancel: () -> Unit
) {
    // --- State für alle Felder ---
    var sapANr by remember { mutableStateOf(initial?.sapANummer.orEmpty()) }
    var schichtenAnzahl by remember { mutableStateOf(initial?.schichten?.size?.toString() ?: "0") }
    var lieferDatumVon by remember { mutableStateOf(TextFieldValue("")) }
    var lieferDatumBis by remember { mutableStateOf(TextFieldValue("")) }
    var ort by remember { mutableStateOf(initial?.ort.orEmpty()) }
    var strecke by remember { mutableStateOf(initial?.strecke.orEmpty()) }
    var kmVon by remember { mutableStateOf(initial?.kmVon.orEmpty()) }
    var kmBis by remember { mutableStateOf(initial?.kmBis.orEmpty()) }
    var massnahme by remember { mutableStateOf(initial?.massnahme.orEmpty()) }
    var bemerkung by remember { mutableStateOf(initial?.bemerkung.orEmpty()) }

    var repeatStartDate by remember { mutableStateOf(TextFieldValue("")) }
    var repeatStartTime by remember { mutableStateOf(TextFieldValue("")) }
    var repeatEndDate by remember { mutableStateOf(TextFieldValue("")) }
    var repeatEndTime by remember { mutableStateOf(TextFieldValue("")) }

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(Modifier.fillMaxWidth()) {
            // linke Column mit den Auftragsdetails
            Column(Modifier.weight(1f)) {
                Text("Auftragsdetails", style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = sapANr,
                    onValueChange = { sapANr = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("SAP/A-Nummer") }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = schichtenAnzahl,
                    onValueChange = { schichtenAnzahl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Anzahl Schichten") }
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = lieferDatumVon,
                        onValueChange = { lieferDatumVon = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Lieferdatum von") },
                        placeholder = { Text("TT.MM.JJJJ") }
                    )
                    OutlinedTextField(
                        value = lieferDatumBis,
                        onValueChange = { lieferDatumBis = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Lieferdatum bis") },
                        placeholder = { Text("TT.MM.JJJJ") }
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = ort,
                    onValueChange = { ort = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ort") }
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = strecke,
                        onValueChange = { strecke = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Strecke") }
                    )
                    OutlinedTextField(
                        value = kmVon,
                        onValueChange = { kmVon = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Km von") }
                    )
                    OutlinedTextField(
                        value = kmBis,
                        onValueChange = { kmBis = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Km bis") }
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = massnahme,
                    onValueChange = { massnahme = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Maßnahme") }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = bemerkung,
                    onValueChange = { bemerkung = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Bemerkung") }
                )
            }

            Spacer(Modifier.width(16.dp))

            // rechte Column nur für Wiederholungs-Zeitraum (daily repeat)
            Column(Modifier.weight(1f)) {
                Text(
                    "Ausfüllen NUR wenn sich die Schichten täglich wiederholen",
                    style = MaterialTheme.typography.subtitle1
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = repeatStartDate,
                        onValueChange = { repeatStartDate = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Startdatum") },
                        placeholder = { Text("TT.MM.JJJJ") }
                    )
                    OutlinedTextField(
                        value = repeatStartTime,
                        onValueChange = { repeatStartTime = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Start Uhrzeit") },
                        placeholder = { Text("HH:mm") }
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = repeatEndDate,
                        onValueChange = { repeatEndDate = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Enddatum") },
                        placeholder = { Text("TT.MM.JJJJ") }
                    )
                    OutlinedTextField(
                        value = repeatEndTime,
                        onValueChange = { repeatEndTime = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("End Uhrzeit") },
                        placeholder = { Text("HH:mm") }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Button-Zeile
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            onDelete?.let {
                GrayIconButton(
                    icon = Icons.Filled.Delete,
                    label = "Löschen",
                    tooltip = "Auftrag löschen",
                    selected = false,
                    onClick = it
                )
            }
            GrayIconButton(
                icon = Icons.Filled.Add,
                label = if (initial == null) "Hinzufügen" else "Speichern",
                tooltip = if (initial == null) "Neuen Auftrag hinzufügen" else "Auftrag speichern",
                selected = false,
                onClick = {
                    onSave(
                        initial?.id,
                        sapANr,
                        ort,
                        strecke,
                        kmVon,
                        kmBis,
                        massnahme,
                        bemerkung.takeIf { it.isNotBlank() }
                    )
                }
            )
            GrayIconButton(
                icon = Icons.Filled.Close,
                label = "Abbrechen",
                tooltip = "Bearbeitung abbrechen",
                selected = false,
                onClick = onCancel
            )
        }
    }
}




@Composable
fun SchichtForm(
    initial: Schicht?,
    formatter: DateTimeFormatter,
    dateFormatter: DateTimeFormatter,
    onSave: (Schicht) -> Unit,
    onCancel: () -> Unit
) {
    var startVal by remember { mutableStateOf(TextFieldValue(initial?.startDatum?.format(formatter).orEmpty().trim())) }
    var endVal by remember { mutableStateOf(TextFieldValue(initial?.endDatum?.format(formatter).orEmpty().trim())) }
    var ortVal by remember { mutableStateOf(TextFieldValue(initial?.ort.orEmpty())) }
    var streckeVal by remember { mutableStateOf(TextFieldValue(initial?.strecke.orEmpty())) }
    var kmVonVal by remember { mutableStateOf(TextFieldValue(initial?.kmVon.orEmpty())) }
    var kmBisVal by remember { mutableStateOf(TextFieldValue(initial?.kmBis.orEmpty())) }
    var massnahmeVal by remember { mutableStateOf(TextFieldValue(initial?.massnahme.orEmpty())) }
    var bemerkungVal by remember { mutableStateOf(TextFieldValue(initial?.bemerkung.orEmpty())) }

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            if (initial == null) "Neue Schicht hinzufügen" else "Schicht bearbeiten",
            style = MaterialTheme.typography.h6
        )
        Spacer(Modifier.height(8.dp))
        listOf(
            "Startdatum" to startVal,
            "Enddatum" to endVal,
            "Ort" to ortVal,
            "Strecke" to streckeVal,
            "Km von" to kmVonVal,
            "Km bis" to kmBisVal,
            "Maßnahme" to massnahmeVal,
            "Bemerkung" to bemerkungVal
        ).forEach { (labelText, valueState) ->
            OutlinedTextField(
                value = valueState,
                onValueChange = { v ->
                    when(labelText) {
                        "Startdatum" -> startVal = v
                        "Enddatum" -> endVal = v
                        "Ort" -> ortVal = v
                        "Strecke" -> streckeVal = v
                        "Km von" -> kmVonVal = v
                        "Km bis" -> kmBisVal = v
                        "Maßnahme" -> massnahmeVal = v
                        "Bemerkung" -> bemerkungVal = v
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(labelText) },
                singleLine = labelText != "Bemerkung"
            )
            Spacer(Modifier.height(8.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onCancel) { Text("Abbrechen") }
            Button(onClick = {
                // Parseiere Eingaben: entweder "dd.MM.yyyy HH:mm" oder nur "dd.MM.yyyy"
                val parsedStart = startVal.text.takeIf { it.isNotBlank() }?.let { text ->
                    try {
                        LocalDateTime.parse(text, formatter)
                    } catch (_: DateTimeParseException) {
                        LocalDate.parse(text, dateFormatter).atStartOfDay()
                    }
                }
                val parsedEnd = endVal.text.takeIf { it.isNotBlank() }?.let { text ->
                    try {
                        LocalDateTime.parse(text, formatter)
                    } catch (_: DateTimeParseException) {
                        LocalDate.parse(text, dateFormatter).atStartOfDay()
                    }
                }
                val sch = Schicht(
                    id = initial?.id ?: UUID.randomUUID().toString(),
                    startDatum = parsedStart,
                    endDatum = parsedEnd,
                    ort = ortVal.text,
                    strecke = streckeVal.text,
                    kmVon = kmVonVal.text,
                    kmBis = kmBisVal.text,
                    massnahme = massnahmeVal.text,
                    mitarbeiter = null,
                    fahrzeug = null,
                    material = null,
                    bemerkung = bemerkungVal.text.takeIf { it.isNotBlank() }
                )
                onSave(sch)
            }) { Text("Speichern") }
        }
    }
}
