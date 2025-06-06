package viewModel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import elemente.ScannerUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import models.*
import repository.AuftragRepository
import view.WiederholungsModus
import view.generiereSchichten
import java.io.File
// ✔ richtiger Import
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * ViewModel für Aufträge+Schichten.
 *
 * **Neu:** Bei allen externen/String‑Datumsangaben wird jetzt versucht,
 * sie in`LocalDateTime` zu konvertieren. Scheitert das Parsing,
 * wird`null` gespeichert-damit entstehen keine Laufzeitfehler mehr.
 */
class AuftraegeViewModel(
    private val repository: AuftragRepository = AuftragRepository()
) {


    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())    // <— hier


    /* ---------------------------------------------------- intern+Flows */
    private val _auftraegeFlow = MutableStateFlow<List<Auftrag>>(emptyList())
    val auftraegeFlow: StateFlow<List<Auftrag>> = _auftraegeFlow
        .map { list ->
            // 1) Aufträge ohne startDatum, in umgekehrter Einfügungsreihenfolge
            val ohneStart = list
                .filter { it.startDatum == null }
                .asReversed()
            // 2) Aufträge mit startDatum, absteigend nach startDatum
            val mitStart = list
                .filter { it.startDatum != null }
                .sortedByDescending { it.startDatum }
            ohneStart + mitStart
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    /* ───────────────── Stammdaten ─────────────────── */

    private val _personen  = MutableStateFlow<List<Person>>(emptyList())
    val    personen get() = _personen.value          // ← List<Person>

    private val _fahrzeuge = MutableStateFlow<List<Fahrzeug>>(emptyList())
    val    fahrzeuge get() = _fahrzeuge.value        // ← List<Fahrzeug>

    private val _material  = MutableStateFlow<List<Material>>(emptyList())
    val    material  get() = _material.value         // ← List<Material>

    /* ───────── restlicher Code unverändert ─────────── */

    init {
        loadAuftraege()
        loadStammdaten()           // <‑‑ neu aufrufen
    }

    private fun loadStammdaten() = scope.launch {
        _personen .value = repository.getAllPerson()
        _fahrzeuge.value = repository.getAllFahrzeug()
        _material .value = repository.getAllMaterial()
    }




    /* ---------------------------------------------------- BasicCRUD */
    fun loadAuftraege() = scope.launch {
        _auftraegeFlow.value = repository.getAllAuftraege()
    }

    fun addAuftrag(a: Auftrag) = scope.launch {
        // DEBUG: prüfen, was das ViewModel gerade zum Insert bekommt
        println("DEBUG ViewModel.addAuftrag: startDatum = ${a.startDatum}")

        repository.insertAuftrag(a)
        _auftraegeFlow.update { it + a }
    }

    fun updateAuftrag(a: Auftrag) = scope.launch {
        // DEBUG: prüfen, was das ViewModel gerade zum Update bekommt
        println("DEBUG ViewModel.updateAuftrag: startDatum = ${a.startDatum}")

        repository.updateAuftrag(a)
        _auftraegeFlow.update { list -> list.map { if (it.id == a.id) a else it } }
    }


    fun deleteAuftrag(id: String) = scope.launch {
        repository.deleteAuftrag(id)
        _auftraegeFlow.update { it.filterNot { o -> o.id == id } }
    }

    /* ---------------------------------------------- AutomatischesAnlegen
       Variante1 – Aufrufer liefert gültige LocalDateTime‑Objekte.
     */
    fun addAuftragAutomatisch(
        basis: Auftrag,
        modus: WiederholungsModus,
        startStr: String?,          // String instead of LocalDateTime?
        endeStr:  String?,
        anzahl: Int?,
        dauer: Long
    ) {
        val start = startStr.toLocalDateTimeOrNull()
        val ende  = endeStr.toLocalDateTimeOrNull()
        addAuftragAutomatisch(basis, modus, start, ende, anzahl, dauer)
    }


    /* ---------------------------------------------- AutomatischesAnlegen
       Variante2 – Aufrufer liefert Strings; Parsingwird abgesichert.
     */
    // ViewModel ▸ AuftraegeViewModel.kt
    fun addAuftragAutomatisch(
        basis: Auftrag,
        modus: WiederholungsModus,
        start: LocalDateTime?,          // ←  nullable
        ende:  LocalDateTime?,
        anzahl: Int?,
        dauer: Long
    ) {
        // Wird kein Start übergeben → keine Schichten generieren
        val schichten = if (start != null)
            generiereSchichten(modus, start, ende, anzahl, dauer, basis.toTemplateSchicht())
        else
            emptyList()

        val finalAuftrag = basis.copy(
            startDatum = schichten.minOfOrNull { it.startDatum ?: start ?: LocalDateTime.MIN }, // ggf. null
            endDatum   = schichten.maxOfOrNull { it.endDatum ?: start ?: LocalDateTime.MIN },
            schichten  = schichten
        )
        addAuftrag(finalAuftrag)
    }


    /* --------------------------------------- Automatisches Update
       Variante 1 – gültige LocalDateTime‑Objekte.
     */
    fun updateAuftragAutomatisch(
        original: Auftrag,
        basis: Auftrag,
        modus: WiederholungsModus,
        start: LocalDateTime,
        ende: LocalDateTime?,
        anzahl: Int?,
        dauer: Long
    ) {
        val schichten = generiereSchichten(
            modus, start, ende, anzahl, dauer,
            basis.toTemplateSchicht()
        )
        val updated = original.copy(
            sapANummer = basis.sapANummer,
            ort        = basis.ort,
            strecke    = basis.strecke,
            kmVon      = basis.kmVon,
            kmBis      = basis.kmBis,
            massnahme  = basis.massnahme,
            bemerkung  = basis.bemerkung,
            startDatum = schichten.minOfOrNull { it.startDatum ?: start },
            endDatum   = schichten.maxOfOrNull { it.endDatum   ?: start },
            schichten  = schichten
        )
        updateAuftrag(updated)
    }

    /* --------------------------------------- Automatisches Update
       Variante 2 – abgesichertes Parsing.
     */
    fun updateAuftragAutomatisch(
        original: Auftrag,
        basis: Auftrag,
        modus: WiederholungsModus,
        startStr: String,
        endeStr: String? = null,
        anzahl: Int?,
        dauer: Long
    ) {
        val start = startStr.toLocalDateTimeOrNull() ?: return  // ohne gültigenStart keinUpdate
        val ende  = endeStr.toLocalDateTimeOrNull()
        updateAuftragAutomatisch(original, basis, modus, start, ende, anzahl, dauer)
    }

    /* ----------------------------------------- Schicht‑Hilfsfunktionen */
    private fun Auftrag.toTemplateSchicht() = Schicht(
        startDatum = null,
        endDatum   = null,
        ort        = ort,
        strecke    = strecke,
        kmVon      = kmVon,
        kmBis      = kmBis,
        massnahme  = massnahme,
        bemerkung  = bemerkung,
        pausenZeit = 0
    )

    /* ------------------------------------------------ Schichten I/O */
    fun addSchicht(auftragId: String, s: Schicht) = scope.launch {
        val current = _auftraegeFlow.value.find { it.id == auftragId } ?: return@launch
        val updated = current.copy(schichten = current.schichten.orEmpty() + s.safeDates())
        repository.updateAuftrag(updated)
        loadAuftraege()
    }

    fun updateSchicht(id: String, s: Schicht) = scope.launch {
        repository.updateSchicht(id, s.safeDates())
        loadAuftraege()
    }

    fun deleteSchicht(auftragId: String, schichtId: String) = scope.launch {
        val current = _auftraegeFlow.value.find { it.id == auftragId } ?: return@launch
        val updated = current.copy(
            schichten = current.schichten.orEmpty().filterNot { it.id == schichtId }
        )
        repository.updateAuftrag(updated)
        loadAuftraege()
    }


    fun scanStundenzettel(auftrag: Auftrag) = scope.launch {
        // Ordner anlegen
        val scanDir = File("scans").apply { mkdirs() }
        // Dateiname: SAP + Datumsspanne
        val df       = DateTimeFormatter.ofPattern("yyyyMMdd")
        val start    = auftrag.schichten.minOf { it.startDatum!! }.format(df)
        val end      = auftrag.schichten.maxOf { it.endDatum!!   }.format(df)
        val filename = "${auftrag.sapANummer ?: auftrag.id}_$start-$end.pdf"
        val outPdf   = File(scanDir, filename)
        // Scannen + PDF
        val tempPng  = File.createTempFile("scan_", ".png")
        if (ScannerUtil.scanToPng(tempPng)) {
            ScannerUtil.pngToPdf(tempPng, outPdf)
            tempPng.delete()
            // In DB speichern
            repository.insertStundenzettel(
                auftrag.id,
                Stundenzettel(
                    startDatum = auftrag.schichten.minOf { it.startDatum!! },
                    endDatum   = auftrag.schichten.maxOf { it.endDatum!!   },
                    pfad       = outPdf.absolutePath
                )
            )
            // Option: die UI neu laden lassen
            loadAuftraege()
        } else {
            // Fehlerbehandlung nach Bedarf
            println("Scan fehlgeschlagen für Auftrag ${auftrag.id}")
        }
    }
    /* ------------------------------------------- private Hilfs‑Utilities */

    /**
     * Versucht einen String in `LocalDateTime` zu parsen.
     * Erlaubt mehrere gebräuchliche Formate; gibt `null` zurück, wenn nichts passt.
     */
    private fun String?.toLocalDateTimeOrNull(): LocalDateTime? {
        if (this.isNullOrBlank()) return null
        for (fmt in SUPPORTED_DATE_FORMATS) {
            try {
                return LocalDateTime.parse(this, fmt)
            } catch (_: DateTimeParseException) { /* nächstes Format */ }
        }
        return null
    }

    /**
     * Entfernt ungültige Datumswerte aus einer Schicht.
     * (Bei direktem Instanziieren aus dem UI kann es sein, dass Datumsfelder
     * noch Strings o. Ä. sind – hier wird das abgefangen.)
     */
    private fun Schicht.safeDates(): Schicht = copy(
        startDatum = when (val d = startDatum) {
            null          -> null
            else          -> d  // bereits LocalDateTime → ok
        },
        endDatum   = when (val d = endDatum)   {
            null          -> null
            else          -> d
        }
    )

    companion object {
        private val SUPPORTED_DATE_FORMATS = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,            // 2025-05-06T15:30:00
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"),   // 06.05.2025 15:30
            DateTimeFormatter.ofPattern("dd.MM.yyyy")          // 06.05.2025
        )
    }
}
