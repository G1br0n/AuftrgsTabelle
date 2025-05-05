package viewModel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import models.Material
import repository.AuftragRepository

class MaterialViewModel(
    private val repository: AuftragRepository = AuftragRepository()
) {
    private val _materialFlow = MutableStateFlow<List<Material>>(emptyList())
    val materialFlow: StateFlow<List<Material>> = _materialFlow.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        loadMaterial()
    }

    fun loadMaterial() {
        scope.launch {
            val list = repository.getAllMaterial()
            _materialFlow.update { list }
        }
    }

    fun addMaterial(
        bezeichnung: String,
        bemerkung: String?
    ) {
        scope.launch {
            val neu = Material(
                bezeichnung = bezeichnung,
                bemerkung = bemerkung
            )
            repository.insertMaterial(neu)
            _materialFlow.update { it + neu }
        }
    }

    fun updateMaterial(
        id: String,
        bezeichnung: String,
        bemerkung: String?
    ) {
        scope.launch {
            val updated = Material(
                id = id,
                bezeichnung = bezeichnung,
                bemerkung = bemerkung
            )
            repository.updateMaterial(id, updated)
            _materialFlow.update { list -> list.map { if (it.id == id) updated else it } }
        }
    }

    fun deleteMaterial(id: String) {
        scope.launch {
            repository.deleteMaterial(id)
            _materialFlow.update { it.filterNot { m -> m.id == id } }
        }
    }
}
