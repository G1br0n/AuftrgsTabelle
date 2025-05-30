@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import repository.AuftragRepository
import models.*
import java.time.format.DateTimeFormatter
import java.time.Duration
import java.util.Locale
import elemente.DatePickerField
import elemente.GrayContentButton
import elemente.GrayFillButton
import java.time.LocalDate
import java.time.LocalTime

// Hilfsklassen für Filter-Ergebnisse
sealed class FilterResult

data class AuftragResult(val auftrag: Auftrag) : FilterResult()
data class MitarbeiterResult(
    val auftrag: Auftrag,
    val schicht: Schicht,
    val mitarbeiter: Person,      // neu
    val hours: Float
) : FilterResult()
data class FahrzeugResult(val auftrag: Auftrag, val schicht: Schicht) : FilterResult()
data class MaterialResult(val auftrag: Auftrag, val schicht: Schicht) : FilterResult()

// Filter-Typen
enum class FilterType(val label: String) {
    AUFTRAG("Auftrag"),
    MITARBEITER("Mitarbeiter"),
    FAHRZEUG("Fahrzeug"),
    MATERIAL("Material")
}


// ViewModel für den Filter
class FilterViewModel(private val repo: AuftragRepository) {
    var filterType by mutableStateOf(FilterType.AUFTRAG)
    var startDate by mutableStateOf<LocalDate?>(null)
    var endDate by mutableStateOf<LocalDate?>(null)
    var sapANummer by mutableStateOf("")
    var selectedPerson by mutableStateOf<Person?>(null)
    var selectedFahrzeug by mutableStateOf<Fahrzeug?>(null)
    var selectedMaterial by mutableStateOf<Material?>(null)
    var results by mutableStateOf<List<FilterResult>>(emptyList())
    var mitarbeiterQuery by mutableStateOf("")
    var fahrzeugQuery by mutableStateOf("")
    var materialQuery by mutableStateOf("")

    fun applyFilter() {
        val sd = startDate?.atStartOfDay()
        val ed = endDate?.atTime(LocalTime.MAX)
        val alle = repo.getAllAuftraege()
        results = when (filterType) {
            FilterType.AUFTRAG -> alle
                .filter { a -> sapANummer.isBlank() || a.sapANummer.equals(sapANummer, true) }
                .map { AuftragResult(it) }

            FilterType.MITARBEITER -> alle.flatMap { auftrag ->
                auftrag.schichten.orEmpty().flatMap { sch ->
                    sch.mitarbeiter
                        .filter { p -> mitarbeiterQuery.isBlank()
                                || "${p.vorname} ${p.name}"
                            .contains(mitarbeiterQuery, ignoreCase = true)
                        }
                        .map { p ->
                            val dur = Duration.between(sch.startDatum, sch.endDatum ?: sch.startDatum)
                            val hours = ( (dur.toMinutes() - sch.pausenZeit) / 60.0f )
                                .let { kotlin.math.round(it * 100) / 100f }
                            MitarbeiterResult(auftrag, sch, p, hours)
                        }
                }
            }

            FilterType.FAHRZEUG -> alle.flatMap { auftrag ->
                auftrag.schichten.orEmpty().filter { sch ->
                    selectedFahrzeug?.let { f -> sch.fahrzeug.any { it.id == f.id } } == true &&
                            (sd == null || sch.startDatum?.isAfter(sd) == true) &&
                            (ed == null || sch.startDatum?.isBefore(ed) == true)
                }.map { sch -> FahrzeugResult(auftrag, sch) }
            }

            FilterType.MATERIAL -> alle.flatMap { auftrag ->
                auftrag.schichten.orEmpty().filter { sch ->
                    selectedMaterial?.let { m -> sch.material.any { it.id == m.id } } == true &&
                            (sd == null || sch.startDatum?.isAfter(sd) == true) &&
                            (ed == null || sch.startDatum?.isBefore(ed) == true)
                }.map { sch -> MaterialResult(auftrag, sch) }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FilterableDropdown(
    items: List<T>,
    text: String,
    onTextChange: (String) -> Unit,
    selected: T?,
    onSelect: (T) -> Unit,
    label: String,
    labelFor: (T) -> String
) {
    var expanded by remember { mutableStateOf(false) }

    // Filtere die Items anhand des aktuellen Texts
    val filtered = remember(text, items) {
        if (text.isBlank()) items else items.filter { labelFor(it).contains(text, ignoreCase = true) }
    }



        Box(Modifier.fillMaxWidth()) {
            TextField(
                value = text,
                onValueChange = { onTextChange(it) /* expanded bleibt unverändert */ },
                label = { Text(label) },
                trailingIcon = {
                    IconButton(onClick = { expanded = !expanded }) {
                        Text("🔎")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                filtered.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(labelFor(item)) },
                        onClick = {
                            onSelect(item)
                            onTextChange(labelFor(item))
                            expanded = false
                        }
                    )
                }
            }
        }
    }


@ExperimentalMaterial3Api
@Composable
fun FilterScreen() {
    val repo = remember { AuftragRepository() }
    val vm = remember { FilterViewModel(repo) }


    val dtf = remember {
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withLocale(Locale.GERMANY)
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Filter-Typ-Auswahl
        Row {
            FilterType.values().forEach { t ->
                GrayContentButton(
                    label = t.label,
                    tooltip = t.label,
                    selected = vm.filterType == t,
                    onClick = { vm.filterType = t },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        // Datumsauswahl
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DatePickerField(
                "Start Datum",
                vm.startDate,
                { vm.startDate = it },
                modifier = Modifier.weight(1f)
            )
            DatePickerField(
                "End Datum",
                vm.endDate,
                { vm.endDate = it },
                modifier = Modifier.weight(1f)
            )
        }


        when (vm.filterType) {
            FilterType.AUFTRAG -> {
                FilterableDropdown(
                    items = repo.getAllAuftraege(),
                    text = vm.sapANummer,                       // nimm den Text aus dem ViewModel
                    onTextChange = { vm.sapANummer = it },      // schreib ihn bei jeder Eingabe zurück
                    selected = vm.results.filterIsInstance<AuftragResult>()
                        .firstOrNull()?.auftrag,
                    onSelect = { vm.sapANummer = it.sapANummer ?: "" },
                    label = "Auftrag",
                    labelFor = { it.sapANummer ?: it.id.toString() }
                )

                val res = vm.results.filterIsInstance<AuftragResult>()
                Text("Ergebnisse: ${res.size}")
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(res) { idx, r ->
                        val sch = r.auftrag.schichten.orEmpty()
                        val start = sch.mapNotNull { it.startDatum }.minOrNull()?.format(dtf) ?: "-"
                        val end = sch.mapNotNull { it.endDatum ?: it.startDatum }.maxOrNull()?.format(dtf) ?: "-"
                        val mCount = sch.flatMap { it.mitarbeiter }.distinctBy { it.id }.size
                        val fCount = sch.flatMap { it.fahrzeug }.distinctBy { it.id }.size
                        val matCount = sch.flatMap { it.material }.distinctBy { it.id }.size
                        Text("${idx+1}. ${r.auftrag.sapANummer ?: r.auftrag.id} | $start - $end | MA:$mCount | FZ:$fCount | MAT:$matCount")
                    }
                }
            }
            FilterType.MITARBEITER -> {
                FilterableDropdown(
                    items = repo.getAllPerson(),
                    text = vm.mitarbeiterQuery,                    // aktueller Text aus dem VM
                    onTextChange = { vm.mitarbeiterQuery = it },   // schreib jede Eingabe ins VM
                    selected = vm.selectedPerson,
                    onSelect = { vm.selectedPerson = it },
                    label = "Mitarbeiter",
                    labelFor = { "${it.vorname} ${it.name}" }
                )

                val mr = vm.results.filterIsInstance<MitarbeiterResult>()
                val totalShifts = mr.size
                val totalHours = mr.sumOf { it.hours.toDouble() }
                Text("Schichten: $totalShifts | Stunden gesamt: ${"%.2f".format(totalHours)} h")
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(mr) { idx, r ->
                        val name = "${r.mitarbeiter.vorname} ${r.mitarbeiter.name}"
                        val s = r.schicht.startDatum?.format(dtf)
                        val e = (r.schicht.endDatum ?: r.schicht.startDatum)?.format(dtf)
                        Text(
                            "${idx+1}. $name | ${r.auftrag.sapANummer} | $s - $e | Dauer: ${"%.2f".format(r.hours)} h | Pause: ${r.schicht.pausenZeit} min"
                        )
                    }
                }
            }
            FilterType.FAHRZEUG -> {
                FilterableDropdown(
                    items = repo.getAllFahrzeug(),
                    text = vm.fahrzeugQuery,                           // ➊
                    onTextChange = { vm.fahrzeugQuery = it },          // ➋
                    selected = vm.selectedFahrzeug,
                    onSelect = {
                        vm.selectedFahrzeug = it
                        vm.fahrzeugQuery = "${it.kennzeichen} - ${it.bezeichnung}" // ➌
                    },
                    label = "Fahrzeug",
                    labelFor = { "${it.kennzeichen} - ${it.bezeichnung}" }
                )

                val fr = vm.results.filterIsInstance<FahrzeugResult>()
                Text("Gefundene Einträge: ${fr.size}")
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(fr) { idx, r ->
                        val s = r.schicht.startDatum?.format(dtf)
                        val e = (r.schicht.endDatum ?: r.schicht.startDatum)?.format(dtf)
                        Text("${idx+1}. ${r.auftrag.sapANummer ?: r.auftrag.id} | $s - $e")
                    }
                }
            }
            FilterType.MATERIAL -> {
                FilterableDropdown(
                    items = repo.getAllMaterial(),
                    text = vm.materialQuery,                            // ➊
                    onTextChange = { vm.materialQuery = it },           // ➋
                    selected = vm.selectedMaterial,
                    onSelect = {
                        vm.selectedMaterial = it
                        vm.materialQuery = it.bezeichnung.toString()   // ➌
                    },
                    label = "Material",
                    labelFor = { it.bezeichnung.toString() }
                )

                val mr = vm.results.filterIsInstance<MaterialResult>()
                Text("Gefundene Einträge: ${mr.size}")
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(mr) { idx, r ->
                        val s = r.schicht.startDatum?.format(dtf)
                        val e = (r.schicht.endDatum ?: r.schicht.startDatum)?.format(dtf)
                        Text("${idx+1}. ${r.auftrag.sapANummer ?: r.auftrag.id} | $s - $e")
                    }
                }
            }
        }

        GrayFillButton(
            label = "Filter Anwenden",
            tooltip = "Filter auf die Daten anwenden",
            selected = false,
            onClick = { vm.applyFilter() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
