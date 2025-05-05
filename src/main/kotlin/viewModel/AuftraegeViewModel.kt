package viewModel

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import models.*
import repository.AuftragRepository
import view.WiederholungsModus
import view.generiereSchichten
// ✔ richtiger Import
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * ViewModel für Aufträge + Schichten.
 *
 * **Neu:** Bei allen externen / String‑Datumsangaben wird jetzt versucht,
 * sie in `LocalDateTime` zu konvertieren. Scheitert das Parsing,
 * wird `null` gespeichert – damit entstehen keine Laufzeitfehler mehr.
 */
class AuftraegeViewModel(
    private val repository: AuftragRepository = AuftragRepository()
) {

    /* ---------------------------------------------------- intern + Flows */
    private val _auftraegeFlow = MutableStateFlow<List<Auftrag>>(emptyList())
    val auftraegeFlow: StateFlow<List<Auftrag>> = _auftraegeFlow.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init { loadAuftraege() }

    /* ---------------------------------------------------- Basic CRUD */
    fun loadAuftraege() = scope.launch {
        _auftraegeFlow.value = repository.getAllAuftraege()
    }

    fun addAuftrag(a: Auftrag) = scope.launch {
        repository.insertAuftrag(a)
        _auftraegeFlow.update { it + a }
    }

    fun updateAuftrag(a: Auftrag) = scope.launch {
        repository.updateAuftrag(a)
        _auftraegeFlow.update { list -> list.map { if (it.id == a.id) a else it } }
    }

    fun deleteAuftrag(id: String) = scope.launch {
        repository.deleteAuftrag(id)
        _auftraegeFlow.update { it.filterNot { o -> o.id == id } }
    }

    /* ---------------------------------------------- Automatisches Anlegen
       Variante 1 – Aufrufer liefert gültige LocalDateTime‑Objekte.
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


    /* ---------------------------------------------- Automatisches Anlegen
       Variante 2 – Aufrufer liefert Strings; Parsing wird abgesichert.
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
        // Wird kein Start übergeben →  keine Schichten generieren
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
        val start = startStr.toLocalDateTimeOrNull() ?: return  // ohne gültigen Start kein Update
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
        mitarbeiter= null,
        fahrzeug   = null,
        material   = null,
        bemerkung  = bemerkung
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
