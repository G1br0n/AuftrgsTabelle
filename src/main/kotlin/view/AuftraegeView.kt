    // ui/AuftraegeUI.kt
    package view

    import androidx.compose.animation.animateContentSize
    import androidx.compose.foundation.BorderStroke
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.lazy.LazyColumn
    import androidx.compose.foundation.lazy.items
    import androidx.compose.foundation.lazy.itemsIndexed
    import androidx.compose.material.*
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.*
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.text.input.TextFieldValue
    import androidx.compose.ui.unit.DpSize
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.window.Window
    import androidx.compose.ui.window.rememberWindowState
    import elemente.GrayFillButton
    import elemente.GrayIconButton
    import models.*
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

        val windowState = rememberWindowState(
            size = DpSize(width = 1200.dp, height = 900.dp)
        )

        val auftraege by viewModel.auftraegeFlow.collectAsState(emptyList())

        // Auswahl nur über IDs
        var selectedAuftragId by remember { mutableStateOf<String?>(null) }
        var selectedSchichtId by remember { mutableStateOf<String?>(null) }

        // Dynamisch aktuelles Objekt ermitteln
        val selectedAuftrag = selectedAuftragId?.let { id ->
            auftraege.find { it.id == id }
        }
        val selectedSchicht = selectedSchichtId?.let { sid ->
            selectedAuftrag?.schichten?.find { it.id == sid }
        }

        var showAuftragForm by remember { mutableStateOf(false) }
        var showSchichtForm by remember { mutableStateOf(false) }

        val dateTimeFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

        Row(
            Modifier
                .fillMaxSize()
                .padding(GAP_M)
        ) {
            /* Auftragsliste */
            Column(Modifier.weight(3f)) {
                GrayIconButton(Icons.Default.Add, "Auftrag", "Neuen Auftrag", false, onClick = {
                    selectedAuftragId = null
                    showAuftragForm = true
                })
                Spacer(Modifier.height(GAP_S))
                Text("Auftragsliste", style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(GAP_XS))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(GAP_S)) {
                    items(
                        items = auftraege,
                        key = { it.id }
                    ) { a ->
                        val isSelected = a.id == selectedAuftragId
                        AuftragCard(
                            auftrag = a,
                            index = auftraege.indexOf(a) + 1,
                            selected = isSelected,
                            onSelect = {
                                selectedAuftragId = a.id
                                selectedSchichtId = null
                            },
                            onEdit = {
                                selectedAuftragId = a.id
                                showAuftragForm = true
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
            Column(Modifier.weight(2f)) {
                selectedAuftrag?.let {
                    GrayIconButton(Icons.Default.Add, "Schicht", "Neue Schicht", false, onClick = {
                        selectedSchichtId = null
                        showSchichtForm = true
                    })
                    Spacer(Modifier.height(GAP_S))
                }
                Text("Schichtenliste", style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(GAP_XS))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(GAP_S)) {
                    selectedAuftrag?.schichten.orEmpty().also { list ->
                        items(
                            items = list,
                            key = { it.id }
                        ) { s ->
                            val isSel = s.id == selectedSchichtId
                            GrayFillButton(
                                icon = Icons.Default.DateRange,
                                label = "Schicht ${list.indexOf(s) + 1}: ${s.startDatum?.format(dateTimeFmt).orEmpty()}",
                                tooltip = "Schicht ${list.indexOf(s) + 1}",
                                selected = isSel,
                                onClick = { selectedSchichtId = s.id }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(GAP_M))

            /* Detailansicht */
            Column(Modifier.weight(5f)) {
                Text("Details", style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(GAP_S))

                selectedSchicht?.let { s ->
                    /** kleine Helper zum hübschen Formatieren */
                    fun List<Any?>?.toLabelList(label: (Any) -> String): String =
                        this?.takeIf { it.isNotEmpty() }
                            ?.joinToString("\n") { "•${label(it as Any)}" }
                    ?: "—"

                    val personenTxt = s.mitarbeiter.toLabelList {
                        it as Person; "${it.vorname} ${it.name}".trim()
                    }
                    val fahrzeugeTxt = s.fahrzeug.toLabelList {
                        it as Fahrzeug; it.bezeichnung ?: "—"
                    }
                    val materialTxt = s.material.toLabelList {
                        it as Material; it.bezeichnung ?: "—"
                    }

                    // Details in einer LazyColumn
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(GAP_S)
                    ) {
                        items(listOf(
                            "Start"       to s.startDatum?.format(dateTimeFmt).orEmpty(),
                            "Ende"        to s.endDatum?.format(dateTimeFmt).orEmpty(),
                            "Ort"         to s.ort.orEmpty(),
                            "Strecke"     to s.strecke.orEmpty(),
                            "Km"          to "${s.kmVon.orEmpty()} – ${s.kmBis.orEmpty()}",
                            "Maßnahme"    to s.massnahme.orEmpty(),
                            "Mitarbeiter" to personenTxt,
                            "Fahrzeuge"   to fahrzeugeTxt,
                            "Material"    to materialTxt,
                            "Bemerkung"   to s.bemerkung.orEmpty()
                        )) { (k, v) ->
                            Text("$k:", style = MaterialTheme.typography.subtitle2)
                            Text(v,    style = MaterialTheme.typography.body2)
                        }
                        // Spacer, damit Edit-Button am Ende bleibt
                        item {
                            Spacer(Modifier.weight(1f))
                        }
                        item {
                            GrayIconButton(
                                Icons.Default.Edit,
                                "Bearbeiten",
                                "Schicht bearbeiten",
                                selected = false,
                                onClick  = { showSchichtForm = true },
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(top = GAP_L)
                            )
                        }
                    }
                } ?: Text("Keine Schicht ausgewählt", color = Color.Gray)
            }
        }


        // Form-Fenster für Auftrag
        if (showAuftragForm) {
            Window(onCloseRequest = { showAuftragForm = false },
                title = if (selectedAuftrag == null) "Neuer Auftrag" else "Auftrag bearbeiten",
                state = windowState) {
                AuftragForm(
                    initial = selectedAuftrag,
                    onSave = onSave@{ id, sap, ort, strecke, kmVon, kmBis,
                                      massnahme, bemerkung, lieferDatum,
                                      modus, rsDate, rsTime, reDate, reTime,
                                      anzahl, dauer ->

                        // wenn irgendwas ungültig ist, früh abbrechen:
                        if (modus != WiederholungsModus.KEINE && rsDate.isBlank()) {
                            // return in die lambda, nicht in die umgebende Funktion
                            return@onSave
                        }

                        val basis = Auftrag(
                            id         = id ?: UUID.randomUUID().toString(),
                            sapANummer = sap,
                            startDatum = lieferDatum?.toLocalDateTimeOrNull(),
                            endDatum   = null,
                            ort        = ort,
                            strecke    = strecke,
                            kmVon      = kmVon,
                            kmBis      = kmBis,
                            massnahme  = massnahme,
                            bemerkung  = bemerkung,
                            schichten  = emptyList()
                        )

                        val startDT = if (modus == WiederholungsModus.KEINE) null
                        else "$rsDate $rsTime".toLocalDateTimeOrNull() ?: return@onSave
                        val endDT   = if (reDate.isNotBlank() && reTime.isNotBlank())
                            "$reDate $reTime".toLocalDateTimeOrNull()
                        else null
                        val dauerStd = dauer ?: 8L

                        if (modus == WiederholungsModus.KEINE) {
                            if (selectedAuftrag == null) viewModel.addAuftrag(basis)
                            else                      viewModel.updateAuftrag(basis.copy(id = selectedAuftrag.id))
                        } else {
                            viewModel.addAuftragAutomatisch(
                                basis  = basis,
                                modus  = modus,
                                start  = startDT,
                                ende   = endDT,
                                anzahl = anzahl,
                                dauer  = dauerStd
                            )
                        }

                        // Dialog schließen und Selektion setzen
                        showAuftragForm     = false
                        selectedAuftragId   = basis.id

                    }, // Ende onSave-Lambda
                    onDelete = selectedAuftrag?.let {
                        {
                            viewModel.deleteAuftrag(it.id)
                            showAuftragForm    = false
                            selectedAuftragId  = null
                        }
                    },
                    onCancel = { showAuftragForm = false }
                )


            }
        }

        // Form-Fenster für Schicht
        if (showSchichtForm && selectedAuftrag != null) {
            Window(onCloseRequest = { showSchichtForm = false },
                title = if (selectedSchicht == null) "Neue Schicht" else "Schicht bearbeiten",
                state = windowState
            ) {
                SchichtForm(
                    initial  = selectedSchicht,
                    onSave   = { neueSchicht ->
                        if (selectedSchicht == null) viewModel.addSchicht(selectedAuftrag.id, neueSchicht)
                        else                          viewModel.updateSchicht(neueSchicht.id, neueSchicht)
                        showSchichtForm   = false
                        selectedSchichtId = neueSchicht.id
                    },
                    onDelete = selectedSchicht?.let { schicht ->
                        {
                            // 1. Schicht aus DB löschen
                            viewModel.deleteSchicht(selectedAuftrag.id, schicht.id)
                            // 2. Formular schließen und Selektion zurücksetzen
                            showSchichtForm   = false
                            selectedSchichtId = null
                        }
                    },
                    onCancel = { showSchichtForm = false },
                    vm       = viewModel
                )
            }
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

        val lieferDT: LocalDateTime? =
            lieferDateVal.text.trim().toLocalDateTimeOrNull()   // nutzt deinen Helper

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
                WiederholungsModus.entries.forEach { m ->
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
    /* ==================================================================
    *  view/SchichtForm.kt          (ersetzt alte Fassung)
    * ================================================================== */

    @Composable
    fun SchichtForm(
        initial: Schicht?,
        dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy"),
        timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm"),
        onSave:  (Schicht) -> Unit,
        onDelete: (() -> Unit)? = null,
        onCancel: () -> Unit,
        vm:      AuftraegeViewModel = remember { AuftraegeViewModel() }
    ) {
        /* ----------- STATE ------------------------------------------------ */
        var startDate by remember { mutableStateOf(TextFieldValue(initial?.startDatum?.format(dateFmt).orEmpty())) }
        var startTime by remember { mutableStateOf(TextFieldValue(initial?.startDatum?.format(timeFmt).orEmpty())) }
        var endDate   by remember { mutableStateOf(TextFieldValue(initial?.endDatum?.format(dateFmt).orEmpty())) }
        var endTime   by remember { mutableStateOf(TextFieldValue(initial?.endDatum?.format(timeFmt).orEmpty())) }

        var ortVal       by remember { mutableStateOf(TextFieldValue(initial?.ort.orEmpty())) }
        var streckeVal   by remember { mutableStateOf(TextFieldValue(initial?.strecke.orEmpty())) }
        var kmVonVal     by remember { mutableStateOf(TextFieldValue(initial?.kmVon.orEmpty())) }
        var kmBisVal     by remember { mutableStateOf(TextFieldValue(initial?.kmBis.orEmpty())) }
        var massnahmeVal by remember { mutableStateOf(TextFieldValue(initial?.massnahme.orEmpty())) }
        var bemerkungVal by remember { mutableStateOf(TextFieldValue(initial?.bemerkung.orEmpty())) }
        var pauseVal     by remember { mutableStateOf(TextFieldValue(initial?.pausenZeit?.toString() ?: "0")) }

        /* aktuell gewählte Relationen */
        var personsSel   by remember { mutableStateOf(initial?.mitarbeiter?.toSet() ?: emptySet()) }
        var materialSel  by remember { mutableStateOf(initial?.material?.toSet() ?: emptySet()) }
        var fahrzeugeSel by remember { mutableStateOf(initial?.fahrzeug?.toSet() ?: emptySet()) }

        /* Picker‑Dialoge sichtbar? */
        var showPersonDlg   by remember { mutableStateOf(false) }
        var showMaterialDlg by remember { mutableStateOf(false) }
        var showFahrzeugDlg by remember { mutableStateOf(false) }

        /* Validierung */
        fun parse(dt: TextFieldValue, tt: TextFieldValue): LocalDateTime? =
            "${dt.text.trim()} ${tt.text.trim()}".toLocalDateTimeOrNull()

        val startDT = parse(startDate, startTime)
        val endDT   = parse(endDate, endTime)
        val startErr = startDate.text.isNotBlank() && startDT == null
        val endErr   = endDate.text.isNotBlank()   && endDT == null
        val pauseErr = pauseVal.text.toIntOrNull() == null
        val pauseMin = pauseVal.text.toIntOrNull() ?: 0

        /* UI */
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(if (initial == null) "Neue Schicht hinzufügen" else "Schicht bearbeiten",
                style = MaterialTheme.typography.h6)
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 1. Spalte: Schicht-Details
                Column(
                    Modifier.weight(3f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // z.B. CompactDualRow für Start/End + Pause
                    CompactDualRow(
                        field1 = { CompactField("Start-Datum", startDate.text, { startDate = it.toTextFieldValue() }, isError = startErr) },
                        field2 = { CompactField("Start-Zeit", startTime.text, { startTime = it.toTextFieldValue() }, isError = startErr) }
                    )
                    CompactDualRow(
                        field1 = { CompactField("End-Datum", endDate.text, { endDate = it.toTextFieldValue() }, isError = endErr) },
                        field2 = { CompactField("End-Zeit", endTime.text, { endTime = it.toTextFieldValue() }, isError = endErr) }
                    )
                    LabeledField("Pausenzeit [Min]", pauseVal, { pauseVal = it }, isError = pauseErr)
                    LabeledField("Ort", ortVal, { ortVal = it })
                    LabeledField("Strecke", streckeVal, { streckeVal = it })
                    CompactDualRow(
                        field1 = { CompactField("Km von", kmVonVal.text, { kmVonVal = it.toTextFieldValue() }) },
                        field2 = { CompactField("Km bis", kmBisVal.text, { kmBisVal = it.toTextFieldValue() }) }
                    )
                    LabeledField("Maßnahme", massnahmeVal, { massnahmeVal = it })
                    LabeledField("Bemerkung", bemerkungVal, { bemerkungVal = it }, singleLine = false)
                    if (startErr || endErr) {
                        Text("Bitte gültiges Datum/Uhrzeit eingeben!", color = Color.Red, style = MaterialTheme.typography.caption)
                    }
                }

                // 2. Spalte: LazyColumns für Auswahl-Listen
                Column(
                    Modifier.weight(2f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Mitarbeiter:", style = MaterialTheme.typography.subtitle2)
                    LazyColumn(Modifier.height(100.dp)) {
                        items(personsSel.toList()) { p -> Text("• ${p.vorname} ${p.name}") }
                    }
                    Text("Material:", style = MaterialTheme.typography.subtitle2)
                    LazyColumn(Modifier.height(100.dp)) {
                        items(materialSel.toList()) { m -> Text("• ${m.bezeichnung}") }
                    }
                    Text("Fahrzeuge:", style = MaterialTheme.typography.subtitle2)
                    LazyColumn(Modifier.height(100.dp)) {
                        items(fahrzeugeSel.toList()) { f -> Text("• ${f.bezeichnung}") }
                    }
                }

                // 3. Spalte: Buttons zum Hinzufügen
                Column(
                    Modifier
                        .weight(1f)
                        .align(Alignment.Top),  // Buttons oben ausrichten
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GrayIconButton(
                        Icons.Default.Person, "Mitarbeiter auswählen", "Dialog öffnen", false
                    , { showPersonDlg = true })
                    GrayIconButton(
                        Icons.Default.Build, "Material auswählen", "Dialog öffnen", false
                    , { showMaterialDlg = true })
                    GrayIconButton(
                        Icons.Default.Settings, "Fahrzeuge auswählen", "Dialog öffnen", false
                    , { showFahrzeugDlg = true })
                }
            }

            Spacer(Modifier.height(16.dp))

            // Aktionen am unteren Rand
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, ""); Spacer(Modifier.width(4.dp)); Text("Abbrechen")
                }
                initial?.let {
                    OutlinedButton(
                        onClick = { onDelete?.invoke() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) {
                        Icon(Icons.Default.Delete, ""); Spacer(Modifier.width(4.dp)); Text("Löschen")
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    enabled = !startErr && !endErr && startDT != null,
                    onClick = {
                        val sd = startDT ?: return@Button  // wenn null, nichts tun
                        onSave(
                            Schicht(
                                id         = initial?.id ?: UUID.randomUUID().toString(),
                                startDatum = sd,
                                endDatum   = endDT,
                                pausenZeit = pauseMin,
                                ort        = ortVal.text,
                                strecke    = streckeVal.text,
                                kmVon      = kmVonVal.text,
                                kmBis      = kmBisVal.text,
                                massnahme  = massnahmeVal.text,
                                mitarbeiter= personsSel.toList(),
                                fahrzeug   = fahrzeugeSel.toList(),
                                material   = materialSel.toList(),
                                bemerkung  = bemerkungVal.text.takeIf { it.isNotBlank() }
                            )
                        )
                    }
                ) {
                    Icon(Icons.Default.Add, ""); Spacer(Modifier.width(4.dp)); Text("Speichern")
                }


            }

            // Picker‑Dialoge
            if (showPersonDlg) MultiSelectDialog(
                title   = "Mitarbeiter auswählen",
                items   = vm.personen,
                label   = { "${it.vorname} ${it.name}".trim() },
                preSel  = personsSel,
                onClose = { it?.let { personsSel = it }; showPersonDlg = false }
            )
            if (showMaterialDlg) MultiSelectDialog(
                title   = "Material auswählen",
                items   = vm.material,
                label   = { it.bezeichnung ?: "—" },
                preSel  = materialSel,
                onClose = { it?.let { materialSel = it }; showMaterialDlg = false }
            )
            if (showFahrzeugDlg) MultiSelectDialog(
                title   = "Fahrzeuge auswählen",
                items   = vm.fahrzeuge,
                label   = { it.bezeichnung ?: "—" },
                preSel  = fahrzeugeSel,
                onClose = { it?.let { fahrzeugeSel = it }; showFahrzeugDlg = false }
            )
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

    /** Generischer Mehrfach‑Auswahl‑Dialog */
    @Composable
    fun <T> MultiSelectDialog(
        title:   String,
        items:   List<T>,
        label:   (T) -> String,
        preSel:  Set<T> = emptySet(),
        onClose: (Set<T>?) -> Unit         // null == Abbrechen
    ) {
        var selected by remember { mutableStateOf(preSel.toMutableSet()) }

        AlertDialog(
            onDismissRequest = { onClose(null) },
            title  = { Text(title) },
            text   = {
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    items(items) { item ->
                        val checked = selected.contains(item)

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // 🟢 NEUE Collection erzeugen und in den State schreiben
                                    selected = selected.toMutableSet().apply {
                                        if (!add(item)) remove(item)
                                    }
                                }
                                .padding(4.dp)
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    selected = selected.toMutableSet().apply {
                                        if (isChecked) add(item) else remove(item)
                                    }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label(item))
                        }
                    }
                }

            },
            confirmButton = {
                TextButton(onClick = { onClose(selected) }) { Text("Übernehmen") }
            },
            dismissButton = {
                TextButton(onClick = { onClose(null) })     { Text("Abbrechen") }
            }
        )
    }