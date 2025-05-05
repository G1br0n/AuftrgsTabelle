// viewModel/AuftraegeViewModel.kt
package viewModel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import models.Auftrag
import models.Schicht
import repository.AuftragRepository

class AuftraegeViewModel(
    private val repository: AuftragRepository = AuftragRepository()
) {
    private val _auftraegeFlow = MutableStateFlow<List<Auftrag>>(emptyList())
    val auftraegeFlow: StateFlow<List<Auftrag>> = _auftraegeFlow.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        loadAuftraege()
    }

    fun loadAuftraege() {
        scope.launch {
            val list = repository.getAllAuftraege()
            _auftraegeFlow.update { list }
        }
    }

    fun addAuftrag(a: Auftrag) {
        scope.launch {
            repository.insertAuftrag(a)
            _auftraegeFlow.update { it + a }
        }
    }

    fun updateAuftrag(a: Auftrag) {
        scope.launch {
            repository.updateAuftrag(a)
            _auftraegeFlow.update { list -> list.map { if (it.id == a.id) a else it } }
        }
    }

    fun deleteAuftrag(id: String) {
        scope.launch {
            repository.deleteAuftrag(id)
            _auftraegeFlow.update { it.filterNot { o -> o.id == id } }
        }
    }

    /**
     * Fügt eine neue Schicht zum Auftrag hinzu, indem
     * wir den Auftrag um die Schicht erweitern und
     * dann repository.updateAuftrag() aufrufen.
     */
    fun addSchicht(auftragId: String, s: Schicht) {
        scope.launch {
            // aktuellen Auftrag aus dem Flow holen
            val current = _auftraegeFlow.value.find { it.id == auftragId }
            current?.let { auftrag ->
                // neue Liste aus alter + neuer Schicht
                val updated = auftrag.copy(
                    schichten = (auftrag.schichten.orEmpty() + s)
                )
                repository.updateAuftrag(updated)
                // Flow neu laden
                loadAuftraege()
            }
        }
    }

    /**
     * Aktualisiert eine bestehende Schicht wie gehabt.
     */
    fun updateSchicht(id: String, s: Schicht) {
        scope.launch {
            repository.updateSchicht(id, s)
            loadAuftraege()
        }
    }

    /**
     * Entfernt eine Schicht, indem wir den Auftrag
     * ohne diese Schicht neu schreiben.
     */
    fun deleteSchicht(auftragId: String, schichtId: String) {
        scope.launch {
            val current = _auftraegeFlow.value.find { it.id == auftragId }
            current?.let { auftrag ->
                val updated = auftrag.copy(
                    schichten = auftrag.schichten.orEmpty()
                        .filterNot { it.id == schichtId }
                )
                repository.updateAuftrag(updated)
                loadAuftraege()
            }
        }
    }
}
