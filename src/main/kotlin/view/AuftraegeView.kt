    // ui/AuftraegeUI.kt
    package view

    import androidx.compose.animation.animateContentSize
    import androidx.compose.foundation.BorderStroke
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.lazy.LazyColumn
    import androidx.compose.foundation.lazy.itemsIndexed
    import androidx.compose.material.*
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.*
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.text.input.TextFieldValue
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.window.Window
    import elemente.GrayFillButton
    import elemente.GrayIconButton
    import models.Auftrag
    import models.Schicht
    import viewModel.AuftraegeViewModel
    import java.time.Duration
    import java.time.LocalDate
    import java.time.LocalDateTime
    import java.time.format.DateTimeFormatter
    import java.time.format.DateTimeParseException
    import java.util.*


    private val GAP_XS = 2.dp   // minimal
    private val GAP_S  = 4.dp
    private val GAP_M  = 8.dp
    private val GAP_L  = 12.dp  // etwas größer

    @Composable
    fun AuftraegeView(
        viewModel: AuftraegeViewModel = remember { AuftraegeViewModel() }
    ) {
        val auftraege by viewModel.auftraegeFlow.collectAsState(emptyList())
        var selectedAuftrag by remember { mutableStateOf<Auftrag?>(null) }
        var selectedSchicht by remember { mutableStateOf<Schicht?>(null) }
        var showAuftragForm by remember { mutableStateOf(false) }
        var showSchichtForm by remember { mutableStateOf(false) }

        val dateTimeFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

        Row(
            Modifier
                .fillMaxSize()
                .padding(GAP_M)                     // vorher 16 dp
        ) {

            /* ---------------- Auftrags‑Liste ---------------- */
            Column(Modifier.weight(3f)) {
                GrayIconButton(Icons.Default.Add,"Auftrag","Neuen Auftrag",false, onClick = {
                    selectedAuftrag = null; showAuftragForm = true
                })
                Spacer(Modifier.height(GAP_S))
                Text("Auftragsliste", style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(GAP_XS))
                /* ---------------------------------- */
                /*  Auftrags‑Liste (geänderte Stelle) */
                /* ---------------------------------- */
                LazyColumn(verticalArrangement = Arrangement.spacedBy(GAP_S)) {
                    itemsIndexed(auftraege) { i, a ->
                        AuftragCard(
                            auftrag   = a,
                            index     = i + 1,
                            selected  = a == selectedAuftrag,
                            onSelect  = { selectedAuftrag = a; selectedSchicht = null },
                            onEdit    = {                   // ⬅️ NEU
                                selectedAuftrag = a
                                showAuftragForm = true      // Formular im Edit‑Modus anzeigen
                            },
                            modifier  = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                        )
                        Spacer(Modifier.height(GAP_S))
                    }
                }

            }

            Spacer(Modifier.width(GAP_M))

            /* ---------------- Schichten‑Liste ---------------- */
            Column(Modifier.weight(2f)) {
                selectedAuftrag?.let {
                    GrayIconButton(Icons.Default.Add, "Schicht", "Neue Schicht", false, onClick = {
                        selectedSchicht = null; showSchichtForm = true
                    })
                    Spacer(Modifier.height(GAP_S))
                }
                Text("Schichtenliste", style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(GAP_XS))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(GAP_S)) {
                    itemsIndexed(selectedAuftrag?.schichten.orEmpty()) { i, s ->
                        GrayFillButton(
                            icon = Icons.Default.DateRange,
                            label = "Schicht ${i + 1}: ${s.startDatum?.format(dateTimeFmt).orEmpty()}",
                            tooltip = "Schicht ${i + 1}",
                            selected = s == selectedSchicht,
                            onClick = { selectedSchicht = s }
                        )
                    }
                }
            }

            Spacer(Modifier.width(GAP_M))

            /* ---------------- Detail‑Ansicht ---------------- */
            Column(Modifier.weight(5f)) {
                Text("Details", style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(GAP_S))
                selectedSchicht?.let { s ->
                    listOf(
                        "Start"     to s.startDatum?.format(dateTimeFmt).orEmpty(),
                        "Ende"      to s.endDatum?.format(dateTimeFmt).orEmpty(),
                        "Ort"       to s.ort.orEmpty(),
                        "Strecke"   to s.strecke.orEmpty(),
                        "Km"        to "${s.kmVon.orEmpty()} – ${s.kmBis.orEmpty()}",
                        "Maßnahme"  to s.massnahme.orEmpty(),
                        "Bemerkung" to s.bemerkung.orEmpty()
                    ).forEach { (k,v) -> Text("$k: $v") }
                    Spacer(Modifier.height(GAP_S))
                    GrayIconButton(Icons.Default.Edit, "Bearbeiten", "Schicht bearbeiten", false, onClick = {
                        showSchichtForm = true
                    })
                } ?: Text("Keine Schicht ausgewählt", color = Color.Gray)
            }
        }

        /* ------------- Fenster: Auftrag & Schicht‑Form ------------- */
        if (showAuftragForm)
            Window(
                onCloseRequest = { showAuftragForm = false },
                title = if (selectedAuftrag == null) "Neuer Auftrag" else "Auftrag bearbeiten"
            ) {
                AuftragForm(
                    initial = selectedAuftrag,
                    onSave = onSave@ { id, sap, ort, strecke, kmVon, kmBis,
                                       massnahme, bemerkung, lieferDatum,
                                       modus, rsDate, rsTime, reDate, reTime,
                                       anzahl, dauer ->

                        val basis = Auftrag(
                            id         = id ?: UUID.randomUUID().toString(),
                            sapANummer = sap,
                            startDatum = null,
                            endDatum   = null,
                            ort        = ort,
                            strecke    = strecke,
                            kmVon      = kmVon,
                            kmBis      = kmBis,
                            massnahme  = massnahme,
                            bemerkung  = bemerkung
                                .let {
                                    if (lieferDatum.isNullOrBlank()) it
                                    else if (it.isBlank()) "Lieferdatum: $lieferDatum"
                                    else "$it\nLieferdatum: $lieferDatum"
                                },
                            schichten  = emptyList()
                        )

                        val startDT = if (modus == WiederholungsModus.KEINE)
                            null            // nicht nötig
                        else
                            "$rsDate $rsTime".toLocalDateTimeOrNull() ?: return@onSave

                        val endDT   = if (reDate.isNotBlank() && reTime.isNotBlank())
                            "$reDate $reTime".toLocalDateTimeOrNull()
                        else null
                        val dauerStd = dauer ?: 8L

                        if (modus == WiederholungsModus.KEINE) {
                            // ───────────── einzelner/freier Auftrag ─────────────
                            if (selectedAuftrag == null) {
                                viewModel.addAuftrag(basis)                       // neu anlegen
                            } else {
                                viewModel.updateAuftrag(basis.copy(id = selectedAuftrag!!.id)) // nur Felder updaten
                            }

                        } else {
                            // ───────────── Auftrag mit automatischen Schichten ─────────────
                            if (modus == WiederholungsModus.KEINE) {
                                // Auftrag ohne automatische Schichten
                                if (selectedAuftrag == null)
                                    viewModel.addAuftrag(basis)
                                else
                                    viewModel.updateAuftrag(basis.copy(id = selectedAuftrag!!.id))
                            } else {
                                // automatische Schichten – startDT darf jetzt null sein
                                viewModel.addAuftragAutomatisch(
                                    basis  = basis,
                                    modus  = modus,
                                    start  = startDT,      // nullable passt zur neuen Signatur
                                    ende   = endDT,
                                    anzahl = anzahl,
                                    dauer  = dauerStd
                                )
                            }
                        }


                            showAuftragForm = false
                    },
                    onDelete = selectedAuftrag?.let { { viewModel.deleteAuftrag(it.id) } },
                    onCancel = { showAuftragForm = false }
                )
            }


        if (showSchichtForm && selectedAuftrag!=null)
            Window(onCloseRequest={showSchichtForm=false},
                title = if (selectedSchicht==null)"Neue Schicht" else "Schicht bearbeiten") {
                SchichtForm(
                    initial = selectedSchicht,
                    formatter = dateTimeFmt,
                    dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy"),
                    onSave = { /* … */ },
                    onCancel = { showSchichtForm=false }
                )
            }
    }


    // Hinweis: AuftragForm und SchichtForm weiterhin unverändert (Anpassung sofern nötig).
    // Die Composables AuftragForm und SchichtForm bleiben unverändert


    enum class WiederholungsModus {
        KEINE,            // einzelne / frei erfasste Schichten
        TAEGLICH,         // täglich wiederholen (ggf. bis Enddatum)
        HINTEREINANDER    // N Schichten lückenlos hintereinander
    }

    // --------------------------------------------
    // File: utils/SchichtGenerator.kt
    // --------------------------------------------


    /**
     * Erzeugt anhand eines Basis‑Schicht‑Objekts automatisch die gewünschte Liste
     * von Schichten, abhängig vom Modus.
     */
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

    // --------------------------------------------
    // File: viewModel/AuftraegeViewModel.kt  (nur neue Funktion)
    // --------------------------------------------



    // ------------- Kleine Hilfs‑Composable ---------------
    // Zeigt die vier Date/Time‑Felder für Täglich‑Modus.
    @Composable
    private fun RepeatingDateTimeFields(
        startDate: MutableState<TextFieldValue>,
        startTime: MutableState<TextFieldValue>,
        endDate:   MutableState<TextFieldValue>,
        endTime:   MutableState<TextFieldValue>
    ) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startDate.value,
                    onValueChange = { startDate.value = it },
                    label = { Text("Startdatum") },
                    placeholder = { Text("TT.MM.JJJJ") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = startTime.value,
                    onValueChange = { startTime.value = it },
                    label = { Text("Startzeit") },
                    placeholder = { Text("HH:mm") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = endDate.value,
                    onValueChange = { endDate.value = it },
                    label = { Text("Enddatum") },
                    placeholder = { Text("TT.MM.JJJJ") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = endTime.value,
                    onValueChange = { endTime.value = it },
                    label = { Text("Endzeit") },
                    placeholder = { Text("HH:mm") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }


    /* ================================================================== */
    /*  Hilfs‑Composable für ein einzeiliges beschriftetes Feld           */
    /* ================================================================== */
    @Composable
    private fun LabeledField(
        label:      String,
        state:      TextFieldValue,
        onChange:   (TextFieldValue) -> Unit,
        isError:    Boolean = false,
        singleLine: Boolean = true
    ) {
        OutlinedTextField(
            value       = state,
            onValueChange = onChange,
            label       = { Text(label) },
            modifier    = Modifier.fillMaxWidth(),
            isError     = isError,
            singleLine  = singleLine
        )
        Spacer(Modifier.height(8.dp))
    }


    /** Gap used inside the compact form */
    private val FORM_GAP = 4.dp

    /** A super‑lightweight field with minimal vertical spacing */
    @Composable
    private fun CompactField(
        label: String,
        text: String,
        onChange: (String) -> Unit,
        singleLine: Boolean = true,
        isError: Boolean = false,
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onChange,
            label = { Text(label) },
            singleLine = singleLine,
            isError = isError,
            modifier = Modifier.fillMaxWidth()
        )
    }

    /** Single row that holds two fields next to each other with tight spacing */
    @Composable
    private fun CompactDualRow(
        field1: @Composable () -> Unit,
        field2: @Composable () -> Unit
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FORM_GAP)
        ) {
            Box(Modifier.weight(1f)) { field1() }
            Box(Modifier.weight(1f)) { field2() }
        }
    }

    /* ================================================================== */
    /*  Auftrag‑Form                                                      */
    /* ================================================================== */



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
    @Composable
    fun AuftragForm(
        initial: Auftrag?,
        onSave: (
            id: String?, sap: String, ort: String, strecke: String,
            kmVon: String, kmBis: String, massnahme: String, bemerkung: String,
            lieferDatum: String?,                   // <‑‑ neu
            modus: WiederholungsModus,
            rsDate: String, rsTime: String,
            reDate: String, reTime: String,
            anzahl: Int?, dauer: Long?
        ) -> Unit,
        onDelete: (() -> Unit)? = null,
        onCancel: () -> Unit
    ) {
        /* ---------- Formatter ---------- */
        val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
        val dtFmt   = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

        /* ---------- State ---------- */
        var sapVal       by remember { mutableStateOf(TextFieldValue(initial?.sapANummer.orEmpty())) }
        var ortVal       by remember { mutableStateOf(TextFieldValue(initial?.ort.orEmpty())) }
        var streckeVal   by remember { mutableStateOf(TextFieldValue(initial?.strecke.orEmpty())) }
        var kmVonVal     by remember { mutableStateOf(TextFieldValue(initial?.kmVon.orEmpty())) }
        var kmBisVal     by remember { mutableStateOf(TextFieldValue(initial?.kmBis.orEmpty())) }
        var massnahmeVal by remember { mutableStateOf(TextFieldValue(initial?.massnahme.orEmpty())) }
        var bemerkungVal by remember { mutableStateOf(TextFieldValue(initial?.bemerkung.orEmpty())) }
        var lieferDateVal by remember { mutableStateOf(TextFieldValue("")) }         // neu

        var modus        by remember { mutableStateOf(WiederholungsModus.KEINE) }

        var rsDateVal by remember { mutableStateOf(TextFieldValue(initial?.startDatum?.format(dateFmt).orEmpty())) }
        var rsTimeVal by remember { mutableStateOf(TextFieldValue(initial?.startDatum?.format(timeFmt).orEmpty())) }
        var reDateVal by remember { mutableStateOf(TextFieldValue(initial?.endDatum?.format(dateFmt).orEmpty())) }
        var reTimeVal by remember { mutableStateOf(TextFieldValue(initial?.endDatum?.format(timeFmt).orEmpty())) }
        var anzahlVal by remember { mutableStateOf(TextFieldValue("")) }
        var dauerVal  by remember { mutableStateOf(TextFieldValue("8")) }

        /* ---------- Validation ---------- */
        fun String.toLocalDateTimeOrNull(): LocalDateTime? {
            val trimmed = trim()
            val formats = listOf(dtFmt, dateFmt)
            for (f in formats) try { return LocalDateTime.parse(trimmed, f) } catch (_: DateTimeParseException) {}
            return null
        }
        fun String.toLocalDateOrNull(): LocalDate? =
            runCatching { LocalDate.parse(trim(), dateFmt) }.getOrNull()

        fun isDateOk(d: TextFieldValue, t: TextFieldValue): Boolean {
            if (d.text.isBlank() && t.text.isBlank()) return modus==WiederholungsModus.KEINE
            return (d.text.isNotBlank() && runCatching {
                val combined = if (t.text.isBlank()) d.text else "${d.text} ${t.text}"
                combined.toLocalDateTimeOrNull() != null
            }.getOrDefault(false))
        }

        val startOk   = isDateOk(rsDateVal, rsTimeVal)
        val endOk     = modus!=WiederholungsModus.TAEGLICH || isDateOk(reDateVal, reTimeVal)
        val anzahlOk  = modus!=WiederholungsModus.HINTEREINANDER || anzahlVal.text.toIntOrNull()?.let { it>0 } == true
        val dauerOk   = dauerVal.text.toLongOrNull()?.let { it>0 } == true
        val lieferOk  = lieferDateVal.text.isBlank() || lieferDateVal.text.toLocalDateOrNull()!=null

        val canSave = startOk && endOk && anzahlOk && dauerOk && lieferOk

        /* ---------- UI ---------- */
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(if (initial==null) "Neuen Auftrag anlegen" else "Auftrag bearbeiten", style=MaterialTheme.typography.h6)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(sapVal, { sapVal=it }, label={Text("SAP‑A‑Nummer")}, modifier=Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(ortVal, { ortVal=it }, label={Text("Ort")}, modifier=Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(streckeVal, { streckeVal=it }, label={Text("Strecke")}, modifier=Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(kmVonVal,{kmVonVal=it},label={Text("Km von")},modifier=Modifier.weight(1f))
                OutlinedTextField(kmBisVal,{kmBisVal=it},label={Text("Km bis")},modifier=Modifier.weight(1f))
            }
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(massnahmeVal,{massnahmeVal=it},label={Text("Maßnahme")},modifier=Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(bemerkungVal,{bemerkungVal=it},label={Text("Bemerkung")},modifier=Modifier.fillMaxWidth(),singleLine=false)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(lieferDateVal,{lieferDateVal=it},label={Text("Liefer‑Datum (optional)")},placeholder={Text("TT.MM.JJJJ")},isError=!lieferOk,modifier=Modifier.fillMaxWidth())

            /* -------- Wiederholungsmodus‑Bereich -------- */
            Spacer(Modifier.height(8.dp))
            Text("Wiederholungsmodus", style=MaterialTheme.typography.subtitle1)
            Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                WiederholungsModus.values().forEach { m ->
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        RadioButton(selected=modus==m,onClick={modus=m})
                        Text(m.name.lowercase().replaceFirstChar{it.uppercase()})
                    }
                }
            }

            if (modus!=WiederholungsModus.KEINE) {
                Spacer(Modifier.height(8.dp))
                Text("Zeitraum", style=MaterialTheme.typography.subtitle2)
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(rsDateVal,{rsDateVal=it},label={Text("Start‑Datum")},placeholder={Text("TT.MM.JJJJ")},isError=!startOk,modifier=Modifier.weight(1f))
                    OutlinedTextField(rsTimeVal,{rsTimeVal=it},label={Text("Start‑Zeit")},placeholder={Text("HH:mm")},isError=!startOk,modifier=Modifier.weight(1f))
                }
                if (modus==WiederholungsModus.TAEGLICH) {
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(reDateVal,{reDateVal=it},label={Text("End‑Datum")},placeholder={Text("TT.MM.JJJJ")},isError=!endOk,modifier=Modifier.weight(1f))
                        OutlinedTextField(reTimeVal,{reTimeVal=it},label={Text("End‑Zeit")},placeholder={Text("HH:mm")},isError=!endOk,modifier=Modifier.weight(1f))
                    }
                }
                if (modus==WiederholungsModus.HINTEREINANDER) {
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(anzahlVal,{anzahlVal=it},label={Text("Anzahl Schichten")},isError=!anzahlOk,modifier=Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(dauerVal,{dauerVal=it},label={Text("Dauer je Schicht (h)")},isError=!dauerOk,modifier=Modifier.fillMaxWidth())
            }

            if (!canSave) Text("Bitte rot markierte Felder korrigieren.",color=Color.Red,style=MaterialTheme.typography.caption)

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onCancel) { Icon(Icons.Default.Close,"");Spacer(Modifier.width(4.dp));Text("Abbrechen") }
                onDelete?.let{ OutlinedButton(it, colors=ButtonDefaults.outlinedButtonColors(contentColor=Color.Red)) {
                    Icon(Icons.Default.Delete,"");Spacer(Modifier.width(4.dp));Text("Löschen") } }
                Spacer(Modifier.weight(1f))
                Button(onClick={
                    onSave(
                        initial?.id,
                        sapVal.text.trim(), ortVal.text.trim(), streckeVal.text.trim(),
                        kmVonVal.text.trim(), kmBisVal.text.trim(), massnahmeVal.text.trim(), bemerkungVal.text.trim(),
                        lieferDateVal.text.trim().ifBlank { null },
                        modus,
                        rsDateVal.text.trim(), rsTimeVal.text.trim(),
                        reDateVal.text.trim(), reTimeVal.text.trim(),
                        anzahlVal.text.toIntOrNull(), dauerVal.text.toLongOrNull()
                    )
                }, enabled=canSave) {
                    Icon(Icons.Default.Add,"");Spacer(Modifier.width(4.dp));Text("Speichern")
                }
            }
        }
    }
    /* ================================================================== */
    /*  Schicht‑Form                                                      */
    /* ================================================================== */
    @Composable
    fun SchichtForm(
        initial: Schicht?,
        formatter: DateTimeFormatter,
        dateFormatter: DateTimeFormatter,
        onSave: (Schicht) -> Unit,
        onCancel: () -> Unit
    ) {
        /* -------- States -------- */
        var startVal by remember { mutableStateOf(TextFieldValue(initial?.startDatum?.format(formatter).orEmpty())) }
        var endVal   by remember { mutableStateOf(TextFieldValue(initial?.endDatum?.format(formatter).orEmpty())) }
        var ortVal   by remember { mutableStateOf(TextFieldValue(initial?.ort.orEmpty())) }
        var streckeVal by remember { mutableStateOf(TextFieldValue(initial?.strecke.orEmpty())) }
        var kmVonVal  by remember { mutableStateOf(TextFieldValue(initial?.kmVon.orEmpty())) }
        var kmBisVal  by remember { mutableStateOf(TextFieldValue(initial?.kmBis.orEmpty())) }
        var massnahmeVal by remember { mutableStateOf(TextFieldValue(initial?.massnahme.orEmpty())) }
        var bemerkungVal by remember { mutableStateOf(TextFieldValue(initial?.bemerkung.orEmpty())) }

        /* -------- Validierung -------- */
        val parsedStart = startVal.text.toLocalDateTimeOrNull()
        val parsedEnd   = endVal.text.toLocalDateTimeOrNull()
        val startErr    = startVal.text.isNotBlank() && parsedStart == null
        val endErr      = endVal.text.isNotBlank()   && parsedEnd   == null

        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(if (initial == null) "Neue Schicht hinzufügen" else "Schicht bearbeiten",
                style = MaterialTheme.typography.h6)
            Spacer(Modifier.height(8.dp))

            LabeledField("Startdatum", startVal, { startVal = it }, startErr)
            LabeledField("Enddatum",   endVal,   { endVal   = it }, endErr)
            LabeledField("Ort",        ortVal,   { ortVal   = it })
            LabeledField("Strecke",    streckeVal, { streckeVal = it })
            LabeledField("Km von",     kmVonVal,  { kmVonVal  = it })
            LabeledField("Km bis",     kmBisVal,  { kmBisVal  = it })
            LabeledField("Maßnahme",   massnahmeVal, { massnahmeVal = it })
            LabeledField("Bemerkung",  bemerkungVal, { bemerkungVal = it }, singleLine = false)

            if (startErr) Text("Ungültiges Datum/Zeitformat", color = Color.Red,
                style = MaterialTheme.typography.caption)
            if (endErr)   Text("Ungültiges Datum/Zeitformat", color = Color.Red,
                style = MaterialTheme.typography.caption)

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                Button(onClick = onCancel) { Text("Abbrechen") }
                Button(
                    enabled = !startErr && !endErr,
                    onClick = {
                        val sch = Schicht(
                            id         = initial?.id ?: UUID.randomUUID().toString(),
                            startDatum = parsedStart,
                            endDatum   = parsedEnd,
                            ort        = ortVal.text,
                            strecke    = streckeVal.text,
                            kmVon      = kmVonVal.text,
                            kmBis      = kmBisVal.text,
                            massnahme  = massnahmeVal.text,
                            mitarbeiter= null,
                            fahrzeug   = null,
                            material   = null,
                            bemerkung  = bemerkungVal.text.takeIf { it.isNotBlank() }
                        )
                        onSave(sch)
                    }
                ) { Text("Speichern") }
            }
        }
    }

    /* ================================================================== */
    /*  String → LocalDateTime Helper                                     */
    /* ================================================================== */
    private val FMT = listOf(
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"),
        DateTimeFormatter.ofPattern("dd.MM.yyyy")
    )

    fun String?.toLocalDateTimeOrNull(): LocalDateTime? {
        if (this.isNullOrBlank()) return null
        for (f in FMT) try { return LocalDateTime.parse(this.trim(), f) }
        catch (_: DateTimeParseException) {}
        return null
    }


    /* ==========================================================
     *  view/AuftragCard.kt   – NEU
     * ========================================================== */

    /* ==========================================================
  *  view/AuftragCard.kt
  * ========================================================== */

    @Composable
    fun AuftragCard(
        auftrag:  Auftrag,
        index:    Int,
        selected: Boolean,
        onSelect: () -> Unit,
        onEdit:   () -> Unit,
        modifier: Modifier = Modifier,
        dateTimeFmt: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy")
    ) {
        /* – Farben unverändert – */
        val grayNormal   = Color(0xFF555555)
        val graySelected = Color(0xFF777777)
        val grayBorder   = Color(0xFF999999)
        val bgColor      = if (selected) graySelected else grayNormal
        val txtColor     = Color.White

        val schichtCount = auftrag.schichten?.size ?: 0
        val startTxt     = auftrag.startDatum?.format(dateTimeFmt).orEmpty()
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onSelect),
            backgroundColor = bgColor,
            elevation       = 0.dp,
            border          = BorderStroke(1.dp, grayBorder)
        ) {
            Row(
                Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                /* -------- linke Spalte -------- */
                Column(
                    modifier = Modifier.weight(10f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    /* Kopfzeile: S/A-Nummer */
                    Text(
                        "${index}. S/A‑Nr.: ${auftrag.sapANummer.orEmpty()}",
                        style = MaterialTheme.typography.subtitle1,
                        color = txtColor
                    )

                    /* 𝗡𝗘𝗨: immer sichtbares Lieferdatum */
                    Text("Lieferdatum: $startTxt", color = txtColor)

                    /* WeitereInfos */

                    Text("Schichten: $schichtCount",color = txtColor)
                    Text("Maßnahme:  ${auftrag.massnahme}", color = txtColor)

                    /* Spacer schiebt Bemerkung nach ganz unten */


                    /* Bemerkung steht jetzt ganz unten */
                    if (!auftrag.bemerkung.isNullOrEmpty()) {
                        Text(
                            "Bemerkung: ${auftrag.bemerkung}",
                            color = txtColor
                        )
                    }

                }

                /* -------- rechte Spalte: Bearbeiten‑Button -------- */
                Column(Modifier.weight(2f)) {
                    GrayIconButton(
                        icon     = Icons.Default.Edit,
                        label    = "",
                        tooltip  = "Auftrag bearbeiten",
                        selected = false,
                        onClick  = onEdit
                    )
                }
            }
        }
    }
