package viewModel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import models.Fahrzeug
import repository.AuftragRepository

class FahrzeugeViewModel(
    private val repository: AuftragRepository = AuftragRepository()
) {
    private val _fahrzeugeFlow = MutableStateFlow<List<Fahrzeug>>(emptyList())
    val fahrzeugeFlow: StateFlow<List<Fahrzeug>> = _fahrzeugeFlow.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        loadFahrzeuge()
    }

    fun loadFahrzeuge() {
        scope.launch {
            val list = repository.getAllFahrzeug()
            _fahrzeugeFlow.update { list }
        }
    }

    fun addFahrzeug(
        bezeichnung: String,
        kennzeichen: String,
        bemerkung: String?
    ) {
        scope.launch {
            val neu = Fahrzeug(
                bezeichnung = bezeichnung,
                kennzeichen = kennzeichen,
                bemerkung = bemerkung
            )
            repository.insertFahrzeug(neu)
            _fahrzeugeFlow.update { it + neu }
        }
    }

    fun updateFahrzeug(
        id: String,
        bezeichnung: String,
        kennzeichen: String,
        bemerkung: String?
    ) {
        scope.launch {
            val updated = Fahrzeug(
                id = id,
                bezeichnung = bezeichnung,
                kennzeichen = kennzeichen,
                bemerkung = bemerkung
            )
            repository.updateFahrzeug(id, updated)
            _fahrzeugeFlow.update { list -> list.map { if (it.id == id) updated else it } }
        }
    }

    fun deleteFahrzeug(id: String) {
        scope.launch {
            repository.deleteFahrzeug(id)
            _fahrzeugeFlow.update { it.filterNot { f -> f.id == id } }
        }
    }
}
