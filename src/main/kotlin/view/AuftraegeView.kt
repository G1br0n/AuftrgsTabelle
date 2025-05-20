            // ui/AuftraegeUI.kt
            package view

            import androidx.compose.animation.animateContentSize
            import androidx.compose.foundation.*
            import androidx.compose.foundation.layout.*
            import androidx.compose.foundation.lazy.LazyColumn
            import androidx.compose.foundation.lazy.items
            import androidx.compose.foundation.lazy.itemsIndexed
            import androidx.compose.foundation.shape.CircleShape
            import androidx.compose.foundation.text.KeyboardOptions
            import androidx.compose.material.*
            import androidx.compose.material.icons.Icons
            import androidx.compose.material.icons.filled.*
            import androidx.compose.material3.ExperimentalMaterial3Api
            import androidx.compose.runtime.*
            import androidx.compose.ui.Alignment
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.graphics.Color
            import androidx.compose.ui.text.input.KeyboardType
            import androidx.compose.ui.text.input.PasswordVisualTransformation
            import androidx.compose.ui.text.input.TextFieldValue
            import androidx.compose.ui.text.style.TextOverflow
            import androidx.compose.ui.unit.DpSize
            import androidx.compose.ui.unit.dp
            import androidx.compose.ui.unit.sp
            import androidx.compose.ui.window.*
            import elemente.*
            import kotlinx.coroutines.launch
            import models.*
            import repository.AuftragRepository
            import viewModel.AuftraegeViewModel
            import java.io.File
            import java.time.Duration
            import java.time.LocalDate
            import java.time.LocalDateTime
            import java.time.LocalTime
            import java.time.format.DateTimeFormatter
            import java.time.format.DateTimeParseException
            import java.util.*

            enum class WiederholungsModus(val label: String) {
                KEINE("Kein"),
                TAEGLICH("Täglich"),
                HINTEREINANDER("Rund-Um")
            }

            private val GAP_XS = 2.dp   // minimal
            private val GAP_S  = 4.dp
            private val GAP_M  = 8.dp
            private val GAP_L  = 12.dp  // etwas größer


            @OptIn(ExperimentalMaterial3Api::class)
            @Composable
            fun AuftraegeView(
                viewModel: AuftraegeViewModel = remember { AuftraegeViewModel() }
            ) {
                val windowState = rememberWindowState(
                    size = DpSize(width = 1200.dp, height = 900.dp)
                )

                val auftraege by viewModel.auftraegeFlow.collectAsState(emptyList())

                // Ein Scope fürs Starten von Coroutines in der UI
                val uiScope = rememberCoroutineScope()

                // Auswahl nur über IDs
                var selectedAuftragId by remember { mutableStateOf<String?>(null) }
                var selectedSchichtId by remember { mutableStateOf<String?>(null) }
                var filterText by remember { mutableStateOf("") }

                // Dynamisch aktuelles Objekt ermitteln
                val selectedAuftrag = selectedAuftragId?.let { id ->
                    auftraege.find { it.id == id }
                }
                val selectedSchicht = selectedSchichtId?.let { sid ->
                    selectedAuftrag?.schichten?.find { it.id == sid }
                }

                var showAuftragForm by remember { mutableStateOf(false) }
                var showSchichtForm by remember { mutableStateOf(false) }

                var confirmCloseAuftrag  by remember { mutableStateOf(false) }
                var confirmCloseSchicht by remember { mutableStateOf(false) }

                val dateTimeFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")


                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(GAP_M)
                ) {
                    /* Auftragsliste */
                    Column(Modifier.weight(3f)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GrayIconButton(
                                icon     = Icons.Default.Add,
                                label    = "Auftrag",
                                selected = false,
                                onClick  = {
                                    selectedAuftragId = null
                                    showAuftragForm   = true
                                }
                            )

                            Spacer(Modifier.width(4.dp))

                            OutlinedTextField(
                                value = filterText,
                                onValueChange = { filterText = it },
                                label = { Text("Filter S/A-Nr.") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        /* gefilterte und – falls gefiltert – nach sapANummer sortierte Liste */
                        val displayed = if (filterText.isNotBlank()) {
                            auftraege
                                .filter { it.sapANummer?.contains(filterText, ignoreCase = true) == true }
                                .sortedBy { it.sapANummer }
                        } else {
                            auftraege
                        }
                        Spacer(Modifier.height(GAP_S))
                        Text("📋 Auftragsliste (${displayed.size})", style = MaterialTheme.typography.h6)
                        Spacer(Modifier.height(GAP_XS))

                        // Hier itemsIndexed verwenden und Nummer umkehren
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(GAP_S)) {
                            itemsIndexed(
                                items = displayed,
                                key = { _, auftrag -> auftrag.id }
                            ) { idx, auftrag ->
                                val isSelected = auftrag.id == selectedAuftragId
                                // umgekehrte Nummer: Gesamtzahl minus aktueller Index
                                val number = displayed.size - idx
                                AuftragCard(
                                    auftrag  = auftrag,
                                    index    = displayed.size - idx,
                                    selected = auftrag.id == selectedAuftragId,
                                    onSelect = {
                                        selectedAuftragId = auftrag.id
                                        selectedSchichtId = null
                                    },
                                    onEdit = {
                                        selectedAuftragId = auftrag.id
                                        showAuftragForm   = true
                                    },
                                    onScan = {
                                        uiScope.launch {
                                            viewModel.scanStundenzettel(auftrag)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize()
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(GAP_M))

                    /* Schichtenliste */
                    Column(Modifier.weight(4f)) {
                        selectedAuftrag?.let {

                            Spacer(Modifier.height(10.dp))

                            GrayIconButton(
                                Icons.Default.Add,
                                label = "Schicht",
                                tooltip = "Neue Schicht",
                                selected = false,
                                onClick = {
                                    selectedSchichtId = null
                                    showSchichtForm = true
                                })

                            Spacer(Modifier.height(GAP_S))
                        }
                        Spacer(Modifier.height(14.dp))

                        Text("🛠️ Schichtenliste (${selectedAuftrag?.schichten?.size ?: 0})", style = MaterialTheme.typography.h6)



                        Spacer(Modifier.height(GAP_XS))

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(GAP_S)) {
                            items(
                                items = selectedAuftrag?.schichten.orEmpty(),
                                key = { it.id }
                            ) { schicht ->
                                val idx = selectedAuftrag?.schichten?.indexOf(schicht)?.plus(1)
                                SchichtCard(
                                    schicht  = schicht,
                                    index    = idx!!,
                                    selected = schicht.id == selectedSchichtId,
                                    onSelect = { selectedSchichtId = schicht.id },
                                    onEdit   = {
                                        selectedSchichtId = schicht.id
                                        showSchichtForm   = true
                                    }
                                )
                            }
                        }


                    }



                    /* Detailansicht */
                    /* Detailansicht */
                    Column(Modifier.weight(6f).padding(start = 8.dp)) {
                        Spacer(Modifier.height(70.dp))
                        Text("🛠️ Schichtenliste (${selectedAuftrag?.schichten?.size ?: 0})", style = MaterialTheme.typography.h6)


                        if (selectedSchicht != null && selectedAuftrag != null) {
                            SchichtDetailView(schicht = selectedSchicht, auftrag = selectedAuftrag)
                        } else {
                            Text("Keine Schicht ausgewählt", color = Color.Gray)
                        }
                    }



                }


                // … innerhalb von AuftraegeView …

                // Form-Fenster für Auftrag (modal)
                if (showAuftragForm) {
                    Dialog(
                        onCloseRequest = { confirmCloseAuftrag = true },
                        title          = if (selectedAuftrag == null) "Neuer Auftrag" else "Auftrag bearbeiten",
                        state          = rememberDialogState(width = 500.dp, height = 700.dp)
                    ) {
                        AuftragForm(
                            initial = selectedAuftrag,
                            onSave = { id, sap, ort, strecke, kmVon, kmBis,
                                       massnahme, bemerkung, lieferDatum,
                                       modus,
                                       startDate, startTime,
                                       endDate, endTime,
                                       anzahl, dauer ->

                                // 1) Basis-Auftrag bauen
                                val basis = Auftrag(
                                    id         = id ?: UUID.randomUUID().toString(),
                                    sapANummer = sap,
                                    startDatum = lieferDatum,
                                    endDatum   = null,
                                    ort        = ort,
                                    strecke    = strecke,
                                    kmVon      = kmVon,
                                    kmBis      = kmBis,
                                    massnahme  = massnahme,
                                    bemerkung  = bemerkung,
                                    schichten  = emptyList()
                                )

                                // 2) Datum + Zeit in LocalDateTime umwandeln
                                val startDT = if (modus == WiederholungsModus.KEINE) null
                                else if (startDate != null && startTime != null)
                                    LocalDateTime.of(startDate, startTime)
                                else return@AuftragForm

                                val endDT = when (modus) {
                                    WiederholungsModus.TAEGLICH ->
                                        if (endDate != null && endTime != null)
                                            LocalDateTime.of(endDate, endTime)
                                        else return@AuftragForm
                                    else -> null
                                }

                                // 3) Aktion ausführen
                                if (modus == WiederholungsModus.KEINE) {
                                    if (selectedAuftrag == null) {
                                        viewModel.addAuftrag(basis)
                                    } else {
                                        viewModel.updateAuftrag(
                                            selectedAuftrag.copy(
                                                sapANummer = sap,
                                                startDatum = lieferDatum,
                                                ort        = ort,
                                                strecke    = strecke,
                                                kmVon      = kmVon,
                                                kmBis      = kmBis,
                                                massnahme  = massnahme,
                                                bemerkung  = bemerkung
                                            )
                                        )
                                    }
                                } else {
                                    viewModel.addAuftragAutomatisch(
                                        basis  = basis,
                                        modus  = modus,
                                        start  = startDT!!,
                                        ende   = endDT,
                                        anzahl = anzahl,
                                        dauer  = dauer ?: 8L
                                    )
                                }

                                // 4) UI-Status zurücksetzen
                                showAuftragForm   = false
                                selectedAuftragId = basis.id
                            },
                            onDelete = selectedAuftrag?.let {
                                {
                                    viewModel.deleteAuftrag(it.id)
                                    showAuftragForm   = false
                                    selectedAuftragId = null
                                }
                            },
                            onCancel = { showAuftragForm = false }
                        )
                    }
                }



                // Form-Fenster für Schicht (modal)
                // Zustände für den Schicht-Dialog


                // … Schicht-Dialog starten …
                if (showSchichtForm && selectedAuftrag != null) {
                    Dialog(
                        onCloseRequest = { confirmCloseSchicht = true },
                        title          = if (selectedSchicht == null) "Neue Schicht" else "Schicht bearbeiten",
                        state          = rememberDialogState(width  = 1200.dp, height = 800.dp)
                    ) {
                        SchichtForm(
                            initial  = selectedSchicht,
                            onSave   = { neueSchicht ->
                                if (selectedSchicht == null) {
                                    viewModel.addSchicht(selectedAuftrag.id, neueSchicht)
                                } else {
                                    viewModel.updateSchicht(neueSchicht.id, neueSchicht)
                                }
                                showSchichtForm     = false
                                selectedSchichtId   = neueSchicht.id
                            },
                            onDelete = selectedSchicht?.let {
                                {
                                    viewModel.deleteSchicht(selectedAuftrag.id, it.id)
                                    showSchichtForm     = false
                                    selectedSchichtId   = null
                                }
                            },
                            onCancel = { showSchichtForm = false },
                            vm       = viewModel
                        )

                    }
                }



                if (confirmCloseAuftrag) {

                    Window(
                        onCloseRequest = { confirmCloseAuftrag = false },
                        title          = "Änderungen verwerfen?",
                        state          = rememberWindowState(width = 400.dp, height = 200.dp),
                        alwaysOnTop    = true
                    ) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Alle Änderungen gehen verloren.\nWirklich schließen?",
                                style = MaterialTheme.typography.body1
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = {
                                    // Nein: nur das Bestätigungsfenster schließen
                                    confirmCloseAuftrag = false
                                }) {
                                    Text("Nein")
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        showAuftragForm     = false
                                        confirmCloseAuftrag = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color(0xFFD32F2F), // Rotton
                                        contentColor    = Color.White
                                    )
                                ) {
                                    Text("Ja")
                                }

                            }

                        }
                    }
                }
                // Bestätigungsfenster als eigenes Window
                if (confirmCloseSchicht) {
                    Window(
                        onCloseRequest = { confirmCloseSchicht = false },
                        title          = "Änderungen verwerfen?",
                        state          = rememberWindowState(width  = 400.dp, height = 200.dp),
                        alwaysOnTop    = true
                    ) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Alle Änderungen gehen verloren.\nWirklich schließen?",
                                style = MaterialTheme.typography.body1
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = {
                                    // „Nein“
                                    confirmCloseSchicht = false
                                }) {
                                    Text("Nein")
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        showSchichtForm     = false
                                        confirmCloseSchicht = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color(0xFFD32F2F), // Rotton
                                        contentColor    = Color.White
                                    )
                                ) {
                                    Text("Ja")
                                }

                            }
                        }
                    }
                }


            }



            /**
             * Erzeugt anhand eines Basis‑Schicht‑Objekts automatisch die gewünschte Liste
             * von Schichten, abhängig vom Modus.
             */

            //M1----generiereSchichten
            fun generiereSchichten(
                modus: WiederholungsModus,
                start: LocalDateTime,
                ende: LocalDateTime?,
                anzahl: Int?,
                dauerStunden: Long,
                basis: Schicht
            ): List<Schicht> {
                return when (modus) {
                    WiederholungsModus.TAEGLICH -> {
                        val endDate = ende ?: start
                        val tage = Duration.between(
                            start.toLocalDate().atStartOfDay(),
                            endDate.toLocalDate().plusDays(1).atStartOfDay()
                        ).toDays()
                        (0 until tage).map { i ->
                            val s = start.plusDays(i)
                            basis.copy(
                                id = UUID.randomUUID().toString(),
                                startDatum = s,
                                endDatum = s.plusHours(dauerStunden)
                            )
                        }
                    }

                    WiederholungsModus.HINTEREINANDER -> {
                        val n = anzahl ?: 1
                        (0 until n).map { i ->
                            val s = start.plusHours(i * dauerStunden)
                            basis.copy(
                                id = UUID.randomUUID().toString(),
                                startDatum = s,
                                endDatum = s.plusHours(dauerStunden)
                            )
                        }
                    }

                    else -> emptyList()
                }
            }




            /**
             * Auftrag‑Formular.
             *
             * ▸ Ermöglicht sowohl manuelles Anlegen einzelner Schichten (Modus KEINE)
             *   als auch automatisches Generieren per WiederholungsModus.
             * ▸ Validiert alle Datums‑/Zeit‑Eingaben live, sodass keine fehlerhaften Werte
             *   gespeichert werden können.
             * ▸ Auf „Speichern“ werden **genau** die Parameter geliefert, die die übergeordnete
             *   AuftraegeView bereits erwartet.
             */
            /**
             * Auftrag‑Formular mit optionaler Liefer‑Datum‑Eingabe.
             */
            @ExperimentalMaterial3Api
            @Composable
            fun AuftragForm(
                initial: Auftrag?,
                onSave: (
                    id: String?, sap: String, ort: String, strecke: String,
                    kmVon: String, kmBis: String, massnahme: String, bemerkung: String,
                    lieferDatum: LocalDateTime?, modus: WiederholungsModus,
                    startDate: LocalDate?, startTime: LocalTime?,
                    endDate: LocalDate?, endTime: LocalTime?, anzahl: Int?, dauer: Long?
                ) -> Unit,
                onDelete: (() -> Unit)? = null,
                onCancel: () -> Unit,
                vm: AuftraegeViewModel = remember { AuftraegeViewModel() }
            ) {
                val dateTimeFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                var showDeleteDialog by remember { mutableStateOf(false) }
                var deletePassword by remember { mutableStateOf("") }

                // Internal state
                var sapVal by remember { mutableStateOf(initial?.sapANummer.orEmpty()) }
                var ortVal by remember { mutableStateOf(initial?.ort.orEmpty()) }
                var streckeVal by remember { mutableStateOf(initial?.strecke.orEmpty()) }
                var kmVonVal by remember { mutableStateOf(initial?.kmVon.orEmpty()) }
                var kmBisVal by remember { mutableStateOf(initial?.kmBis.orEmpty()) }
                var massnahmeVal by remember { mutableStateOf(initial?.massnahme.orEmpty()) }
                var bemerkungVal by remember { mutableStateOf(initial?.bemerkung.orEmpty()) }
                var lieferDatum by remember { mutableStateOf(initial?.startDatum) }

                var modus by remember { mutableStateOf(WiederholungsModus.KEINE) }
                var startDate by remember { mutableStateOf(initial?.startDatum?.toLocalDate()) }
                var startTime by remember { mutableStateOf(initial?.startDatum?.toLocalTime()) }
                var endDate by remember { mutableStateOf(initial?.endDatum?.toLocalDate()) }
                var endTime by remember { mutableStateOf(initial?.endDatum?.toLocalTime()) }
                var anzahlVal by remember { mutableStateOf("") }
                var dauerVal by remember { mutableStateOf(initial?.schichten?.firstOrNull()?.let { sch ->
                    sch.startDatum?.hour?.let { h -> sch.endDatum?.hour?.minus(h) }?.toString()
                } ?: "8") }

                val canSave = sapVal.isNotBlank()

                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Row with two columns: details and repetition inputs
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left column: Auftrag details
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (initial == null) "Neuen Auftrag anlegen" else "Auftrag bearbeiten",
                                fontSize = AppStyle.TextSizes.Large,
                                color = AppStyle.Colors.TextPrimary
                            )
                            OutlinedTextField(
                                value = sapVal,
                                onValueChange = { sapVal = it },
                                label = { Text("📋  Stempel-A-SAP-Nummer", fontSize = AppStyle.TextSizes.Small) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = LocalTextStyle.current.copy(fontSize = AppStyle.TextSizes.Normal)
                            )

                            /*OutlinedTextField(
                                value = ortVal, onValueChange = { ortVal = it },
                                label = { Text("Ort") }, modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = streckeVal, onValueChange = { streckeVal = it },
                                label = { Text("Strecke") }, modifier = Modifier.fillMaxWidth()
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = kmVonVal, onValueChange = { kmVonVal = it },
                                    label = { Text("Km von") }, modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = kmBisVal, onValueChange = { kmBisVal = it },
                                    label = { Text("Km bis") }, modifier = Modifier.weight(1f)
                                )
                            }*/
                            OutlinedTextField(
                                value = massnahmeVal, onValueChange = { massnahmeVal = it },
                                label = { Text("🏫  Maßnahme/Ort", fontSize = AppStyle.TextSizes.Small) }, modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = bemerkungVal, onValueChange = { bemerkungVal = it },
                                label = { Text("🚨  Bemerkung") }, modifier = Modifier.fillMaxWidth()
                            )
                            DateTimePickerField(
                                label = "📅  Start-Datum & Zeit (optional)",
                                initialDateTime = lieferDatum,
                                onDateTimeSelected = { lieferDatum = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Right column: repetition settings
                        /*   Column(
                               Modifier.weight(1f),

                           ) {

                               Text("Wiederholungsmodus", style = MaterialTheme.typography.subtitle1)
                               Row(
                                   verticalAlignment = Alignment.CenterVertically,
                                   horizontalArrangement = Arrangement.spacedBy(8.dp),
                                   modifier = Modifier.fillMaxWidth()
                               ) {
                                   WiederholungsModus.values().forEach { m ->
                                       Row(verticalAlignment = Alignment.CenterVertically) {
                                           RadioButton(selected = modus == m, onClick = { modus = m })
                                           Spacer(Modifier.width(4.dp))
                                           Text(m.label.lowercase().replaceFirstChar { it.uppercase() })
                                       }
                                   }
                               }
                               if (modus != WiederholungsModus.KEINE) {
                                   // Zeitraum inputs
                                   Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                       DatePickerField(
                                           label = "Start-Datum",
                                           selectedDate = startDate,
                                           onDateSelected = { startDate = it },
                                           modifier = Modifier.weight(1f)
                                       )
                                       TimePickerField(
                                           label = "Start-Zeit",
                                           selectedTime = startTime,
                                           onTimeSelected = { startTime = it },
                                           modifier = Modifier.weight(1f)
                                       )
                                   }
                                   if (modus == WiederholungsModus.TAEGLICH) {
                                       Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                           DatePickerField(
                                               label = "End-Datum",
                                               selectedDate = endDate,
                                               onDateSelected = { endDate = it },
                                               modifier = Modifier.weight(1f)
                                           )
                                           TimePickerField(
                                               label = "End-Zeit",
                                               selectedTime = endTime,
                                               onTimeSelected = { endTime = it },
                                               modifier = Modifier.weight(1f)
                                           )
                                       }
                                   } else {
                                       OutlinedTextField(
                                           value = anzahlVal, onValueChange = { anzahlVal = it },
                                           label = { Text("Anzahl Schichten") },
                                           singleLine = true,
                                           modifier = Modifier.fillMaxWidth()
                                       )
                                   }
                                   OutlinedTextField(
                                       value = dauerVal, onValueChange = { dauerVal = it },
                                       label = { Text("Dauer je Schicht (h)") },
                                       singleLine = true,
                                       modifier = Modifier.fillMaxWidth()
                                   )
                               }
                           }*/
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1) Lösch-Button ganz links, wenn onDelete existiert
                        onDelete?.let {
                            OutlinedButton(
                                onClick = { showDeleteDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    backgroundColor = Color(0xFFD32F2F),
                                    contentColor    = Color.White
                                )
                            ) {
                                Text("Löschen")
                            }
                        }

                        // 2) Spacer dehnt sich, um die Buttons rechts zu schieben
                        Spacer(Modifier.weight(1f))

                        // 3) Abbrechen
                        OutlinedButton(onClick = onCancel) {
                            Text("Abbrechen")
                        }

                        Spacer(Modifier.width(8.dp))

                        // 4) Speichern
                        Button(
                            enabled = canSave,
                            onClick = {
                                onSave(
                                    initial?.id,
                                    sapVal.trim(), "", "",
                                    "", "", massnahmeVal.trim(), bemerkungVal.trim(),
                                    lieferDatum, WiederholungsModus.KEINE,
                                    null, null, null, null, null, null
                                )
                            }
                        ) {
                            Text("Speichern")
                        }
                    }
                }

                // Single confirmation dialog with password input
                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            showDeleteDialog = false
                            deletePassword = ""
                        },
                        title = { Text("Löschen bestätigen") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Bitte Passwort eingeben, um zu bestätigen:")
                                OutlinedTextField(
                                    value = deletePassword,
                                    onValueChange = { deletePassword = it },
                                    label = { Text("Passwort") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation()
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    onDelete?.invoke()
                                    showDeleteDialog = false
                                    deletePassword = ""
                                },
                                enabled = deletePassword == "test"
                            ) {
                                Text("Löschen")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showDeleteDialog = false
                                deletePassword = ""
                            }) {
                                Text("Abbrechen")
                            }
                        }
                    )
                }
            }

            /* ==================================================================
            *  view/SchichtForm.kt          (ersetzt alte Fassung)
            * ================================================================== */

            @OptIn(ExperimentalMaterial3Api::class)
            @Composable
            fun SchichtForm(
                initial: Schicht?,
                onSave:  (Schicht) -> Unit,
                onDelete: (() -> Unit)? = null,
                onCancel: () -> Unit,
                vm:      AuftraegeViewModel = remember { AuftraegeViewModel() }
            ) {

                var confirmClose by remember { mutableStateOf(false) }

                // Formatter nur noch für Labels:
                val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

                // State for deletion dialog and password
                var showDeleteDialog by remember { mutableStateOf(false) }
                var deletePassword by remember { mutableStateOf("") }

                // --- STATE für Datum & Zeit ---
                var startDate by remember { mutableStateOf(initial?.startDatum?.toLocalDate()) }
                var startTime by remember { mutableStateOf(initial?.startDatum?.toLocalTime()) }
                var endDate   by remember { mutableStateOf(initial?.endDatum?.toLocalDate()) }
                var endTime   by remember { mutableStateOf(initial?.endDatum?.toLocalTime()) }

                // Rest-State bleibt unverändert:
                var ortVal       by remember { mutableStateOf(initial?.ort.orEmpty()) }
                var streckeVal   by remember { mutableStateOf(initial?.strecke.orEmpty()) }
                var kmVonVal     by remember { mutableStateOf(initial?.kmVon.orEmpty()) }
                var kmBisVal     by remember { mutableStateOf(initial?.kmBis.orEmpty()) }
                var massnahmeVal by remember { mutableStateOf(initial?.massnahme.orEmpty()) }
                var bemerkungVal by remember { mutableStateOf(initial?.bemerkung.orEmpty()) }
                var pauseVal     by remember { mutableStateOf(initial?.pausenZeit?.toString() ?: "0") }

                var personsSel   by remember { mutableStateOf(initial?.mitarbeiter?.toSet() ?: emptySet()) }
                var materialSel  by remember { mutableStateOf(initial?.material?.toSet() ?: emptySet()) }
                var fahrzeugeSel by remember { mutableStateOf(initial?.fahrzeug?.toSet() ?: emptySet()) }

                var showPersonDlg   by remember { mutableStateOf(false) }
                var showMaterialDlg by remember { mutableStateOf(false) }
                var showFahrzeugDlg by remember { mutableStateOf(false) }

                // Validierung
                val startDT = if (startDate != null && startTime != null)
                    LocalDateTime.of(startDate, startTime) else null
                val endDT = if (endDate != null && endTime != null)
                    LocalDateTime.of(endDate, endTime) else null

                val startErr = startDate == null || startTime == null
                val endErr   = endDate == null || endTime == null
                val pauseErr = pauseVal.toIntOrNull() == null
                val pauseMin = pauseVal.toIntOrNull() ?: 0




                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        if (initial == null) "Neue Schicht hinzufügen" else "Schicht bearbeiten",
                        fontSize = AppStyle.TextSizes.Large,
                        color = AppStyle.Colors.TextPrimary
                    )
                    Spacer(Modifier.height(12.dp))

                    // Hauptbereich: Datum/Zeit + Felder + Auswahl
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Datum/Zeit & Basisfelder
                        Column(Modifier.weight(3f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Datum/Zeit Picker
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DatePickerField(
                                    label = "Start-Datum",
                                    selectedDate = startDate,
                                    onDateSelected = { startDate = it },
                                    modifier = Modifier.weight(1f)
                                )
                                TimePickerField(
                                    label = "Start-Zeit",
                                    selectedTime = startTime,
                                    onTimeSelected = { startTime = it },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DatePickerField(
                                    label = "End-Datum",
                                    selectedDate = endDate,
                                    onDateSelected = { endDate = it },
                                    modifier = Modifier.weight(1f)
                                )
                                TimePickerField(
                                    label = "End-Zeit",
                                    selectedTime = endTime,
                                    onTimeSelected = { endTime = it },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Ausgewählte Felder füllen jetzt die volle Breite
                            OutlinedTextField(
                                value = pauseVal,
                                onValueChange = { pauseVal = it },
                                label = { Text("Pausenzeit [Min]") },
                                isError = pauseErr,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = ortVal,
                                onValueChange = { ortVal = it },
                                label = {
                                    Text("Ort", fontSize = AppStyle.TextSizes.Small)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = streckeVal,
                                onValueChange = { streckeVal = it },
                                label = { Text("Strecke", fontSize = AppStyle.TextSizes.Small) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = massnahmeVal,
                                onValueChange = { massnahmeVal = it },
                                label = { Text("Maßnahme", fontSize = AppStyle.TextSizes.Small) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = bemerkungVal,
                                onValueChange = { bemerkungVal = it },
                                label = { Text("Bemerkung", fontSize = AppStyle.TextSizes.Small) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (startErr || endErr) {
                                Text(
                                    "Bitte gültiges Datum & Zeit auswählen",
                                    fontSize = AppStyle.TextSizes.Small,
                                    color = AppStyle.Colors.Error
                                )
                            }
                        }

                        // Auswahl-Spalten gleichmäßig aufteilen
                        // --- Personen ---
                        Column(Modifier.weight(2f).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            GrayIconButton(
                                Icons.Default.Person,
                                label    = "Mitarbeiter (${personsSel.size})",      // Count im Header
                                tooltip  = "Mitarbeiter wählen",
                                selected = false,
                                onClick  = { showPersonDlg = true }
                            )
                            Spacer(Modifier.height(12.dp))



                                OutlinedTextField(
                                    value = kmBisVal,
                                    onValueChange = { kmBisVal = it },
                                    label = { Text("Anzahl Voraus", fontSize = AppStyle.TextSizes.Small) },
                                    modifier = Modifier.width(200.dp) .heightIn(50.dp).padding(8.dp)
                                )

                            Spacer(Modifier.height(12.dp))
                            LazyColumn(Modifier.height(450.dp)) {
                                itemsIndexed(personsSel.toList()) { idx, p ->
                                    Text("${idx + 1}. ${p.vorname} ${p.name}")       // Nummerierung
                                }
                            }
                        }

                        // --- Fahrzeuge ---
                        Column(Modifier.weight(2f).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            GrayIconButton(
                                Icons.Default.Settings,
                                label    = "Fahrzeuge (${fahrzeugeSel.size})",     // Count im Header
                                tooltip  = "Fahrzeuge wählen",
                                selected = false,
                                onClick  = { showFahrzeugDlg = true }
                            )
                            LazyColumn(Modifier.height(500.dp)) {
                                itemsIndexed(fahrzeugeSel.toList()) { idx, f ->
                                    Text("${idx + 1}. ${f.bezeichnung.orEmpty()}")
                                }
                            }
                        }

                        // --- Material ---
                        Column(Modifier.weight(2f).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            GrayIconButton(
                                Icons.Default.Build,
                                label    = "Material (${materialSel.size})",       // Count im Header
                                tooltip  = "Material wählen",
                                selected = false,
                                onClick  = { showMaterialDlg = true }
                            )
                            LazyColumn(Modifier.height(500.dp)) {
                                itemsIndexed(materialSel.toList()) { idx, m ->
                                    Text("${idx + 1}. ${m.bezeichnung.orEmpty()}")
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Action buttons with delete and save
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        onDelete?.let {
                            // Fully red delete button
                            OutlinedButton(
                                onClick = { showDeleteDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    backgroundColor = Color(0xFFD32F2F),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Löschen")
                            }
                        }
                        Spacer(Modifier.width(20.dp))
                        OutlinedButton(onClick = onCancel) {
                            Text("Abbrechen")
                        }

                        Spacer(Modifier.weight(1f))
                        Button(
                            enabled = startDT != null && endDT != null && !pauseErr,
                            onClick = {
                                onSave(
                                    Schicht(
                                        id         = initial?.id ?: UUID.randomUUID().toString(),
                                        startDatum = startDT!!,
                                        endDatum   = endDT,
                                        pausenZeit = pauseMin,
                                        ort        = ortVal,
                                        strecke    = streckeVal,
                                        kmVon      = kmVonVal,
                                        kmBis      = kmBisVal,
                                        massnahme  = massnahmeVal,
                                        mitarbeiter= personsSel.toList(),
                                        fahrzeug   = fahrzeugeSel.toList(),
                                        material   = materialSel.toList(),
                                        bemerkung  = bemerkungVal.takeIf { it.isNotBlank() }
                                    )
                                )
                            }
                        ) {
                            Text("Speichern")
                        }
                    }
                }

                // Hier kommt der Bestätigungs-Dialog für das Schließen übers X
                if (confirmClose) {
                    AlertDialog(
                        onDismissRequest = {
                            println("SchichtForm: Schließen abgebrochen")
                            confirmClose = false
                        },
                        title = { Text("Änderungen verwerfen?", fontSize = AppStyle.TextSizes.Normal) },
                        text = {
                            Text(
                                "Alle Änderungen gehen verloren. Wirklich schließen?",
                                fontSize = AppStyle.TextSizes.Small
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                println("SchichtForm: Schließen bestätigt")
                                confirmClose = false
                                onCancel()  // jetzt wirklich schließen
                            }) {
                                Text("Ja")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                println("SchichtForm: Schließen abgebrochen")
                                confirmClose = false
                            }) {
                                Text("Nein")
                            }
                        }
                    )
                }

                // — dein bestehender Lösch-Dialog bleibt unverändert —
                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            showDeleteDialog = false
                            deletePassword = ""
                        },
                        title = { Text("Löschen bestätigen") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Bitte Passwort eingeben, um zu bestätigen:")
                                OutlinedTextField(
                                    value = deletePassword,
                                    onValueChange = { deletePassword = it },
                                    label = { Text("Passwort") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation()
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    onDelete?.invoke()
                                    showDeleteDialog = false
                                    deletePassword = ""
                                },
                                enabled = deletePassword == "test"
                            ) {
                                Text("Löschen")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showDeleteDialog = false
                                deletePassword = ""
                            }) {
                                Text("Abbrechen")
                            }
                        }
                    )
                }

                // Picker-Dialoge
                if (showPersonDlg) MultiSelectWindow(
                    title   = "Mitarbeiter auswählen",
                    items   = vm.personen,
                    label   = { "${it.vorname} ${it.name}" },
                    preSel  = personsSel
                ) { result ->
                    result?.let { personsSel = it }
                    showPersonDlg = false
                }
                if (showMaterialDlg) MultiSelectWindow(
                    title   = "Material auswählen",
                    items   = vm.material,
                    label   = { it.bezeichnung ?: "–" },
                    preSel  = materialSel
                ) { result ->
                    result?.let { materialSel = it }
                    showMaterialDlg = false
                }
                if (showFahrzeugDlg) MultiSelectWindow(
                    title   = "Fahrzeuge auswählen",
                    items   = vm.fahrzeuge,
                    label   = { it.bezeichnung ?: "–" },
                    preSel  = fahrzeugeSel
                ) { result ->
                    result?.let { fahrzeugeSel = it }
                    showFahrzeugDlg = false
                }
            }





            /* ------------- kleine Extension --------------------------------------*/
            private fun String.toTextFieldValue() = TextFieldValue(this)
            private fun TextFieldValue(text: String) = TextFieldValue(text)


            /* ================================================================== */
            /*  String → LocalDateTime Helper                                     */
            /* ================================================================== */
            private val FMT = listOf(
                DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"),
                DateTimeFormatter.ofPattern("dd.MM.yyyy")
            )

            fun String?.toLocalDateTimeOrNull(): LocalDateTime? {
                if (this.isNullOrBlank()) return null
                // 1) vollen Datum-Zeit-Parser
                runCatching {
                    return LocalDateTime.parse(this.trim(), DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                }
                // 2) Datums-Parser + atStartOfDay()
                runCatching {
                    return LocalDate
                        .parse(this.trim(), DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                        .atStartOfDay()
                }
                return null
            }



            /* ==========================================================
            *  view/AuftragCard.kt
            * ========================================================== */
            @Composable
            fun AuftragCard(
                auftrag: Auftrag,
                index: Int,
                selected: Boolean,
                onSelect: () -> Unit,
                onEdit: () -> Unit,
                onScan: () -> Unit,
                modifier: Modifier = Modifier,
            ) {
                val bgColor = if (selected) Color(0xFF777777) else Color(0xFF555555)
                val borderColor = Color(0xFF999999)
                val dateTimeFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                val schichtCount = auftrag.schichten?.size ?: 0
                val startTxt = auftrag.startDatum?.format(dateTimeFmt).orEmpty()

                Card(
                    modifier = modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSelect),
                    backgroundColor = bgColor,
                    elevation = 0.dp,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Linke Spalte
                        Column(
                            modifier = Modifier.weight(10f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row {
                                Text(
                                    text = "${index}. 📋 : ${auftrag.sapANummer.orEmpty()}",
                                    style = MaterialTheme.typography.subtitle1.copy(
                                        fontSize = AppStyle.TextSizes.Normal
                                    ),
                                    color = AppStyle.Colors.Warning,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "🛠️ : $schichtCount",
                                    fontSize = AppStyle.TextSizes.Small,
                                    color = AppStyle.Colors.TextPrimary
                                )
                            }

                            Text(
                                text = "    📅 : $startTxt",
                                fontSize = AppStyle.TextSizes.Small,
                                color = Color.Green
                            )

                            if (!auftrag.massnahme.isNullOrEmpty()) {
                                Text(
                                    text = "    🏫 : ${auftrag.massnahme}",
                                    fontSize = AppStyle.TextSizes.Small,
                                    color = AppStyle.Colors.TextPrimary
                                )
                            }

                            if (!auftrag.bemerkung.isNullOrEmpty()) {
                                Text(
                                    text = "    🚨 : ${auftrag.bemerkung}",
                                    fontSize = AppStyle.TextSizes.Small,
                                    color = AppStyle.Colors.TextPrimary
                                )
                            }
                        }

                        // Rechte Spalte: Buttons
                        Column(Modifier.weight(1f)) {
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF555555), shape = CircleShape)
                            ) {
                                Text(
                                    "⚙️",
                                    fontSize = AppStyle.TextSizes.Normal
                                )
                            }

                            /* IconButton(
                                 onClick = onScan,
                                 modifier = Modifier
                                     .size(32.dp)
                                     .background(Color(0xFF555555), shape = CircleShape)
                             ) {
                                 Icon(
                                     Icons.Default.KeyboardArrowDown,
                                     contentDescription = "Scan",
                                     tint = AppStyle.Colors.TextPrimary
                                 )
                             }*/
                        }
                    }
                }
            }







            @Composable
            fun SchichtCard(
                schicht: Schicht,
                index: Int,
                selected: Boolean = false,
                onSelect: () -> Unit = {},
                onEdit: () -> Unit = {}
            ) {
                val bgColor = if (selected) Color(0xFF777777) else Color(0xFF555555)
                val borderColor = Color(0xFF999999)
                val txtColor = AppStyle.Colors.TextPrimary

                val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                val startTxt = schicht.startDatum?.format(dateFmt).orEmpty()
                val endTxt = schicht.endDatum?.format(dateFmt).orEmpty()
                val pauseMin = schicht.pausenZeit
                val total = schicht.startDatum?.let { s -> schicht.endDatum?.let { e -> Duration.between(s, e) } }
                val netto = total?.minusMinutes(pauseMin.toLong())
                val durationText = if (netto != null) String.format("%dh %02dm", netto.toHours(), netto.toMinutesPart()) else "–"
                val voraus: String? = schicht.kmBis

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSelect),
                    backgroundColor = bgColor,
                    elevation = 0.dp,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(10f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${index}  : Schicht",
                                fontSize = AppStyle.TextSizes.Normal,
                                color = AppStyle.Colors.Warning
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(Modifier.width(8.dp))

                                Column {
                                    Text(
                                        text = "🚀 : $startTxt",
                                        fontSize = AppStyle.TextSizes.Small,
                                        color = txtColor
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        text = "🏁 : $endTxt",
                                        fontSize = AppStyle.TextSizes.Small,
                                        color = txtColor
                                    )
                                }

                                Column {
                                    Text(
                                        text = "   ⏸️ : ${pauseMin}m",
                                        fontSize = AppStyle.TextSizes.Small,
                                        color = txtColor
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        text = "   🕔 : $durationText",
                                        fontSize = AppStyle.TextSizes.Small,
                                        color = txtColor
                                    )
                                }
                            }

                            Row {
                                if (schicht.mitarbeiter.isNotEmpty()) {
                                    Text(
                                        text = "  👥 : ${schicht.mitarbeiter.size}/${schicht.kmBis}",
                                        fontSize = AppStyle.TextSizes.Small,
                                        color = txtColor
                                    )
                                } else {
                                    Text(
                                        text = "  👥 : 0/${schicht.kmBis}",
                                        fontSize = AppStyle.TextSizes.Small,
                                        color = txtColor
                                    )
                                }
                                if (schicht.fahrzeug.isNotEmpty()) {
                                    Text(
                                        text = "          🚗 : ${schicht.fahrzeug.size}",
                                        fontSize = AppStyle.TextSizes.Small,
                                        color = txtColor
                                    )
                                }
                                if (schicht.material.isNotEmpty()) {
                                    Text(
                                        text = "         📦 : ${schicht.material.size}",
                                        fontSize = AppStyle.TextSizes.Small,
                                        color = txtColor
                                    )
                                }
                            }
                        }

                        Column(Modifier.weight(1f)) {
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF555555), shape = CircleShape)
                            ) {
                                Text(
                                    "⚙️",
                                    fontSize = AppStyle.TextSizes.Normal
                                )
                            }
                        }
                    }
                }
            }


            @Composable
            fun <T> MultiSelectWindow(
                title: String,
                items: List<T>,
                label: (T) -> String,
                preSel: Set<T> = emptySet(),
                onResult: (Set<T>?) -> Unit
            ) {
                var selected by remember { mutableStateOf(preSel) }
                // 1) neuer State für den Filter-Text
                var filterText by remember { mutableStateOf("") }
                val windowState = rememberWindowState(size = DpSize(600.dp, 600.dp))

                Window(
                    onCloseRequest = { onResult(null) },
                    title = title,
                    state = windowState,
                    alwaysOnTop = true
                ) {
                    Column(Modifier.fillMaxSize().padding(8.dp)) {
                        Text("$title (${selected.size} ausgewählt)", style = MaterialTheme.typography.h6)
                        Spacer(Modifier.height(8.dp))

                        // 2) Liste nur mit Items, die zum Filter passen
                        LazyColumn(Modifier.weight(1f)) {
                            items(
                                items = items
                                    .filter { label(it).contains(filterText, ignoreCase = true) }
                            ) { item ->
                                val isChecked = selected.contains(item)
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selected = if (isChecked) selected - item
                                            else selected + item
                                        }
                                        .padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selected = if (checked) selected + item
                                            else selected - item
                                        }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(label(item))
                                }
                            }
                        }

                        Divider()
                        Spacer(Modifier.height(8.dp))

                        // 3) Bottom-Row: links das Filter-Feld, rechts die Buttons
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = filterText,
                                onValueChange = { filterText = it },
                                label = { Text("Filter") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = { onResult(null) }) {
                                Text("Abbrechen")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { onResult(selected) }) {
                                Text("Übernehmen")
                            }
                        }
                    }
                }
            }



            @OptIn(ExperimentalMaterial3Api::class)
            @Composable
            fun SchichtDetailView(
                schicht: Schicht,
                auftrag: Auftrag
            ) {
                val dateTimeFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                val textColor   = AppStyle.Colors.TextPrimary
                val warnColor   = AppStyle.Colors.Warning
                val bgColor     = AppStyle.Colors.Primary
                val borderColor = AppStyle.Colors.Secondary
                val mitarbeiterCount = schicht.mitarbeiter.size
                val fahrzeugCount   = schicht.fahrzeug.size
                val materialCount   = schicht.material.size
                val voraus: String? = schicht.kmBis

                // Berechne Netto-Stunden
                val duration = schicht.startDatum?.let { s ->
                    schicht.endDatum?.let { e ->
                        Duration.between(s, e).minusMinutes(schicht.pausenZeit.toLong())
                    }
                }
                val nettoText = duration?.let { dur ->
                    "${dur.toHours()}h ${dur.toMinutesPart()}m"
                } ?: "–"

                Card(
                    modifier        = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    backgroundColor = bgColor,
                    elevation       = 0.dp,
                    border          = BorderStroke(1.dp, borderColor)
                ) {
                    Column(
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Auftrag + SAP gleich wie zuvor...
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text("📋 Auftrag", fontSize = AppStyle.TextSizes.Large, color = warnColor)
                            Text(auftrag.sapANummer.orEmpty(), fontSize = AppStyle.TextSizes.Large, color = textColor)
                        }
                        listOf(
                            "📅 Start: ${auftrag.startDatum?.format(dateTimeFmt).orEmpty()}",
                            "📍 Ort: ${auftrag.ort.orEmpty()}",
                            "🏫 Maßnahme: ${auftrag.massnahme.orEmpty()}",
                            "📝 Bemerkung: ${auftrag.bemerkung.orEmpty()}"
                        ).forEach { line ->
                            Text(line, fontSize = AppStyle.TextSizes.Normal, color = textColor)
                        }

                        Divider(color = borderColor.copy(alpha = 0.5f), thickness = 1.dp)

                        // Schicht-Überschrift
                        Text("📦 Schicht", fontSize = AppStyle.TextSizes.Large, color = warnColor)

                        // ** NEU: Zwei Spalten für Start/End und Pause/Netto **
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Links: Start & Ende
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("🚀 Start:", fontSize = AppStyle.TextSizes.Normal, color = textColor)
                                Text(schicht.startDatum?.format(dateTimeFmt).orEmpty(), fontSize = AppStyle.TextSizes.Small, color = textColor)
                                Spacer(Modifier.height(8.dp))
                                Text("🏁 Ende:", fontSize = AppStyle.TextSizes.Normal, color = textColor)
                                Text(schicht.endDatum?.format(dateTimeFmt).orEmpty(), fontSize = AppStyle.TextSizes.Small, color = textColor)
                            }
                            // Rechts: Pause & Netto
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("⏸️ Pause:", fontSize = AppStyle.TextSizes.Normal, color = textColor)
                                Text("${schicht.pausenZeit} Min", fontSize = AppStyle.TextSizes.Small, color = textColor)
                                Spacer(Modifier.height(8.dp))
                                Text("⏱ Netto:", fontSize = AppStyle.TextSizes.Normal, color = textColor)
                                Text(nettoText, fontSize = AppStyle.TextSizes.Small, color = textColor)
                            }
                        }

                        // ** NEU: Strecke/Kilometer als eigene Zeile **
                        Text(
                            text     = "🛣️ Strecke: von ${schicht.kmVon.orEmpty()} bis ${schicht.kmBis.orEmpty()}",
                            fontSize = AppStyle.TextSizes.Normal,
                            color    = textColor
                        )

                        // ** NEU: Maßnahme + Bemerkung **
                        Text(
                            text     = "🏫 Maßnahme: ${schicht.massnahme.orEmpty()}",
                            fontSize = AppStyle.TextSizes.Normal,
                            color    = textColor
                        )
                        if (!schicht.bemerkung.isNullOrBlank()) {
                            Text(
                                text     = "📝 Bemerkung: ${schicht.bemerkung}",
                                fontSize = AppStyle.TextSizes.Normal,
                                color    = textColor
                            )
                        }

                        Divider(color = borderColor.copy(alpha = 0.5f), thickness = 1.dp)

                        // Unten: drei gleich breite Spalten
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Mitarbeiter
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "👥 Mitarbeiter: $mitarbeiterCount/${voraus}",
                                    fontSize = AppStyle.TextSizes.Large,
                                    color    = warnColor
                                )
                                if (mitarbeiterCount == 0) {
                                    Text("– keine –", fontSize = AppStyle.TextSizes.Small, color = AppStyle.Colors.TextSecondary)
                                } else {
                                    schicht.mitarbeiter.forEach { m ->
                                        Text(
                                            "• ${m.vorname} ${m.name} (${m.firma})",
                                            fontSize = AppStyle.TextSizes.Small,
                                            color    = textColor
                                        )
                                    }
                                }
                            }

                            // Fahrzeuge
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "🚗 Fahrzeuge: $fahrzeugCount",
                                    fontSize = AppStyle.TextSizes.Large,
                                    color    = warnColor
                                )
                                if (fahrzeugCount == 0) {
                                    Text("– keine –", fontSize = AppStyle.TextSizes.Small, color = AppStyle.Colors.TextSecondary)
                                } else {
                                    schicht.fahrzeug.forEach { f ->
                                        Text(
                                            "• ${f.bezeichnung} – ${f.kennzeichen}",
                                            fontSize = AppStyle.TextSizes.Small,
                                            color    = textColor
                                        )
                                    }
                                }
                            }

                            // Material
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "📦 Material: $materialCount",
                                    fontSize = AppStyle.TextSizes.Large,
                                    color    = warnColor
                                )
                                if (materialCount == 0) {
                                    Text("– kein –", fontSize = AppStyle.TextSizes.Small, color = AppStyle.Colors.TextSecondary)
                                } else {
                                    schicht.material.forEach { mat ->
                                        Text(
                                            "• ${mat.bezeichnung.orEmpty()}",
                                            fontSize = AppStyle.TextSizes.Small,
                                            color    = textColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
