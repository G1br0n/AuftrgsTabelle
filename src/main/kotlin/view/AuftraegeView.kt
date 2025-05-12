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
                    items(
                        items = selectedAuftrag?.schichten.orEmpty(),
                        key = { it.id }
                    ) { schicht ->
                        val idx = selectedAuftrag?.schichten?.indexOf(schicht)?.plus(1) ?: 0
                        SchichtCard(
                            schicht = schicht,
                            index = idx,
                            selected = (schicht.id == selectedSchichtId),
                            onSelect = { selectedSchichtId = schicht.id }
                        )
                    }
                }

            }



            /* Detailansicht */
            Column(Modifier.weight(5f).padding(start = 18.dp)) {

                Spacer(Modifier.height(GAP_S))

                selectedSchicht?.let { s ->
                    // 1) Bearbeiten-Button oben
                    Row(
                        Modifier
                            .fillMaxWidth()

                    ) {
                        GrayIconButton(
                            icon     = Icons.Default.Edit,
                            label    = "Bearbeiten",
                            tooltip  = "Schicht bearbeiten",
                            selected = false,
                            onClick  = { showSchichtForm = true }
                        )
                    }
                    Spacer(Modifier.width(GAP_M))
                    Text("Details", style = MaterialTheme.typography.h6)

                    // 2) Helpers zum sicheren Formatieren
                    fun <T> List<T>?.toLabelList(label: (T) -> String): String =
                        this?.takeIf { it.isNotEmpty() }
                            ?.joinToString("\n") { "• ${label(it)}" }
                            ?: "–"

                    val personenTxt  = s.mitarbeiter.toLabelList { "${it.vorname} ${it.name}".trim() }
                    val fahrzeugeTxt = s.fahrzeug.toLabelList   { it.bezeichnung.orEmpty() }
                    val materialTxt  = s.material.toLabelList   { it.bezeichnung.orEmpty() }

                    // 3) Die eigentliche Detail-Liste
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(GAP_S)
                    ) {
                        items(
                            listOf(
                                "Start"       to s.startDatum?.format(dateTimeFmt).orEmpty().ifBlank { "–" },
                                "Ende"        to s.endDatum?.format(dateTimeFmt).orEmpty().ifBlank { "–" },
                                "Ort"         to s.ort.orEmpty().ifBlank { "–" },
                                "Strecke"     to s.strecke.orEmpty().ifBlank { "–" },
                                "Km"          to listOf(s.kmVon, s.kmBis).joinToString(" – ") { it.orEmpty() }.ifBlank { "–" },
                                "Maßnahme"    to s.massnahme.orEmpty().ifBlank { "–" },
                                "Mitarbeiter" to personenTxt,
                                "Fahrzeuge"   to fahrzeugeTxt,
                                "Material"    to materialTxt,
                                "Bemerkung"   to s.bemerkung.orEmpty().ifBlank { "–" }
                            )
                        ) { (label, value) ->
                            Text(text = "$label:", style = MaterialTheme.typography.subtitle2)
                            Text(text = value, style = MaterialTheme.typography.body2)
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
                            if (selectedAuftrag == null) {
                                viewModel.addAuftrag(basis)
                            } else {
                                // Alte Schichten beibehalten:
                                val updated = selectedAuftrag.copy(
                                    sapANummer = sap,
                                    startDatum = lieferDatum?.toLocalDateTimeOrNull(),
                                    ort        = ort,
                                    strecke    = strecke,
                                    kmVon      = kmVon,
                                    kmBis      = kmBis,
                                    massnahme  = massnahme,
                                    bemerkung  = bemerkung
                                    // schichten bleibt unverändert
                                )
                                viewModel.updateAuftrag(updated)
                            }
                        }
                        else {
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
        var lieferDateVal by remember { mutableStateOf(TextFieldValue(initial?.startDatum?.format(dateFmt).orEmpty())) }      // neu

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
            OutlinedTextField(lieferDateVal, { lieferDateVal = it }, label = { Text("Liefer-Datum (optional)") }, placeholder = { Text("TT.MM.JJJJ") }, isError = !lieferOk, modifier = Modifier.fillMaxWidth())
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
                Button(
                    onClick = {
                        onSave(
                            initial?.id,
                            sapVal.text.trim(),
                            ortVal.text.trim(),
                            streckeVal.text.trim(),
                            kmVonVal.text.trim(),
                            kmBisVal.text.trim(),
                            massnahmeVal.text.trim(),
                            bemerkungVal.text.trim(),
                            lieferDateVal.text.trim().ifBlank { null },  // jetzt korrekt vorbelegt
                            modus,
                            rsDateVal.text.trim(),
                            rsTimeVal.text.trim(),
                            reDateVal.text.trim(),
                            reTimeVal.text.trim(),
                            anzahlVal.text.toIntOrNull(),
                            dauerVal.text.toLongOrNull()
                        )
                    },
                    enabled = canSave
                ) {
                    Icon(Icons.Default.Add, "")
                    Spacer(Modifier.width(4.dp))
                    Text("Speichern")
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
                        icon = Icons.Default.Person,
                        label = "Mitarbeiter wählen",
                        tooltip = "Dialog öffnen",
                        selected = false,
                        onClick = { showPersonDlg = true }
                    )
                    GrayIconButton(
                        icon = Icons.Default.Build,
                        label = "Material wählen",
                        tooltip = "Dialog öffnen",
                        selected = false,
                        onClick = { showMaterialDlg = true }
                    )
                    GrayIconButton(
                        icon = Icons.Default.Settings,
                        label = "Fahrzeuge wählen",
                        tooltip = "Dialog öffnen",
                        selected = false,
                        onClick = { showFahrzeugDlg = true }
                    )
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




    @Composable
    fun SchichtCard(
        schicht: Schicht,
        index: Int,
        selected: Boolean = false,
        onSelect: () -> Unit = {}
    ) {
        // Farben wie in AuftragCard
        val grayNormal = Color(0xFF555555)
        val graySelected = Color(0xFF777777)
        val grayBorder = Color(0xFF999999)
        val bgColor = if (selected) graySelected else grayNormal
        val txtColor = Color.White

        val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        val start = schicht.startDatum
        val ende = schicht.endDatum
        val pauseMin = schicht.pausenZeit
        val durationText = if (start != null && ende != null) {
            val total = Duration.between(start, ende)
            val netto = total.minusMinutes(pauseMin.toLong())
            String.format("%dh %02dm", netto.toHours(), netto.toMinutesPart())
        } else {
            "–"
        }
        val mitarbeiterCount = schicht.mitarbeiter.size ?: 0

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable(onClick = onSelect),
            backgroundColor = bgColor,
            elevation = 0.dp,
            border = BorderStroke(1.dp, grayBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Laufende Schicht
                Text(
                    text = "Schicht $index",
                    style = MaterialTheme.typography.subtitle1,
                    color = txtColor
                )
                Spacer(Modifier.height(4.dp))
                // Startdatum und Uhrzeit
                Row {
                    Column {
                        Text(
                            text = "Start: ${start?.format(dateFmt).orEmpty()}",
                            style = MaterialTheme.typography.body2,
                            color = txtColor
                        )
                        // Endedatum und Uhrzeit
                        Text(
                            text = "Ende:  ${ende?.format(dateFmt).orEmpty()}",
                            style = MaterialTheme.typography.body2,
                            color = txtColor
                        )
                    }
                    Column {
                        // Pause
                        Text(
                            text = "Pause: ${pauseMin}m",
                            style = MaterialTheme.typography.body2,
                            color = txtColor
                        )
                        // Netto-Stunden
                        Text(
                            text = "Netto: $durationText",
                            style = MaterialTheme.typography.body2,
                            color = txtColor
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Spacer(Modifier.height(4.dp))
                // Anzahl Mitarbeiter
                Text(
                    text = "Mitarbeiter: $mitarbeiterCount",
                    style = MaterialTheme.typography.body2,
                    color = txtColor
                )
            }
        }
    }


    /**
     * Ein neues Multi-Select-Fenster mit fester Höhe und Zähler oben.
     */
    @Composable
    fun <T> MultiSelectWindow(
        title: String,
        items: List<T>,
        label: (T) -> String,
        preSel: Set<T> = emptySet(),
        onResult: (Set<T>?) -> Unit
    ) {
        var selected by remember { mutableStateOf(preSel.toMutableSet()) }

        val windowState = rememberWindowState(
            size = DpSize(width = 600.dp, height = 1100.dp)
        )

        Window(
            onCloseRequest = { onResult(null) },
            title = title,
            state = windowState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // 1) Überschrift mit Zähler
                Text(
                    text = "$title (${selected.size} ausgewählt)",
                    style = MaterialTheme.typography.h6
                )
                Spacer(Modifier.height(8.dp))

                // 2) Der scrollbare Bereich, nimmt genau den Rest des Platzes
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn {
                        items(items) { item ->
                            val isChecked = selected.contains(item)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selected = selected.toMutableSet().apply {
                                            if (isChecked) remove(item) else add(item)
                                        }
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selected = selected.toMutableSet().apply {
                                            if (checked) add(item) else remove(item)
                                        }
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(label(item))
                            }
                        }
                    }
                }

                Divider()
                Spacer(Modifier.height(8.dp))

                // 3) Die Buttons, immer unten sichtbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
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

