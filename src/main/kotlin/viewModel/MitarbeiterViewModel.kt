// viewModel/MitarbeiterViewModel.kt
package viewModel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import models.Person
import repository.AuftragRepository

/**
 * ViewModel-ähnliche Klasse für Mitarbeiter (Personen) in Desktop Compose
 * Verwaltet Personen über ein StateFlow und führt CRUD-Operationen im Repository durch
 */
class MitarbeiterViewModel(
    private val repository: AuftragRepository = AuftragRepository()
) {
    private val _personenFlow = MutableStateFlow<List<Person>>(emptyList())
    /** Öffentlicher, unveränderbarer Flow der Personenliste */
    val personenFlow: StateFlow<List<Person>> = _personenFlow.asStateFlow()

    // Eigenständiger CoroutineScope für Hintergrund-Operationen
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())



    // …

    fun movePerson(from: Int, to: Int) {
        _personenFlow.update { list ->
            val mutable = list.toMutableList()
            val item = mutable.removeAt(from)
            mutable.add(to, item)
            // in DB alle Positionen neu schreiben:
            mutable.forEachIndexed { idx, p ->
                repository.updatePersonPosition(p.id, idx)
            }
            mutable
        }
    }


    init {
        loadPersonen()
    }

    /** Lädt alle Personen (Mitarbeiter) aus dem Repository */
    fun loadPersonen() {
        scope.launch {
            val list = repository.getAllPerson()
            _personenFlow.update { list }
        }
    }

    /**
     * Fügt eine neue Person hinzu und aktualisiert den Flow
     */
    fun addPerson(
        vorname: String,
        name: String,
        firma: String?,
        bemerkung: String?
    ) {
        scope.launch {
            val neu = Person(
                vorname = vorname,
                name = name,
                firma = firma,
                bemerkung = bemerkung
            )
            repository.insertPerson(neu)
            _personenFlow.update { it + neu }
        }
    }

    /**
     * Aktualisiert eine bestehende Person und aktualisiert den Flow
     */
    fun updatePerson(
        id: String,
        vorname: String,
        name: String,
        firma: String?,
        bemerkung: String?
    ) {
        scope.launch {
            val updated = Person(
                id = id,
                vorname = vorname,
                name = name,
                firma = firma,
                bemerkung = bemerkung
            )
            repository.updatePerson(id, updated)
            _personenFlow.update { list ->
                list.map { if (it.id == id) updated else it }
            }
        }
    }

    /**
     * Löscht eine Person nach ID und aktualisiert den Flow
     */
    fun deletePerson(id: String) {
        scope.launch {
            repository.deletePerson(id)
            _personenFlow.update { it.filterNot { p -> p.id == id } }
        }
    }
}
