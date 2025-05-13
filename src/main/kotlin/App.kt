package ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

import elemente.GrayIconButton
import models.Auftrag
import models.Schicht
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.axis.DateAxis
import org.jfree.chart.axis.DateTickUnit
import org.jfree.chart.axis.DateTickUnitType
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator
import org.jfree.chart.plot.CategoryPlot
import org.jfree.chart.renderer.category.GanttRenderer
import org.jfree.data.gantt.Task
import org.jfree.data.gantt.TaskSeries
import org.jfree.data.gantt.TaskSeriesCollection
import repository.AuftragRepository
import view.AuftraegeView
import view.FahrzeugeView
import view.MaterialView
import view.MitarbeiterView
import viewModel.AuftraegeViewModel
import java.awt.Dimension
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import javax.swing.JComponent
import javax.swing.JScrollPane
import javax.swing.ScrollPaneConstants

// -------------------------  AppContent -------------------------
@Composable
fun AppContent() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Auftraege) }
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            NavigationBar(currentScreen) { currentScreen = it }
        }
        Box(Modifier.weight(6f)) {
            ContentArea(currentScreen)
        }
    }
}

sealed class Screen(val label: String, val icon: ImageVector) {
    object Auftraege     : Screen("📋 Aufträge", Icons.Filled.Menu)
    object Mitarbeiter   : Screen("👷🏼‍♂️ Mitarbeiter", Icons.Filled.Person)
    object Fahrzeuge     : Screen("🚘 Fahrzeuge", Icons.Filled.Edit)
    object Material      : Screen("🛠️ Materialien", Icons.Filled.Build)
    object Qualification : Screen("Qualification", Icons.Filled.Edit)
    object Auswertung    : Screen("📊 Auswertung", Icons.Filled.Edit)
}

// -------------------------  NavigationBar -------------------------
@Composable
fun NavigationBar(selected: Screen, onSelect: (Screen) -> Unit) {
    Column(
        Modifier
            .fillMaxHeight()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf(
            Screen.Auftraege,
            Screen.Mitarbeiter,
            Screen.Fahrzeuge,
            Screen.Material,
            Screen.Auswertung
        ).forEach { screen ->
            GrayIconButton(
                label     = screen.label,
                tooltip   = screen.label,
                selected  = screen == selected,
                onClick   = { onSelect(screen) },
                fullWidth = true
            )
        }
    }
}

// -------------------------  ContentArea -------------------------
@Composable
fun ContentArea(currentScreen: Screen) {
    when (currentScreen) {
        is Screen.Auftraege     -> AuftraegeView()
        is Screen.Mitarbeiter   -> MitarbeiterView()
        is Screen.Fahrzeuge     -> FahrzeugeView()
        is Screen.Material      -> MaterialView()
        is Screen.Qualification -> TODO()
        is Screen.Auswertung    -> AuswertungView()
    }
}

// -------------------------  AuswertungView -------------------------

// -------------------------  AuswertungView -------------------------

/**
 * Vollständige, lauffähige Version der Auswertung-Ansicht.
 *
 * – verwendet das vorhandene **AuftraegeViewModel** (kein produceState mehr)
 * – reagiert auf jeden DB-Änderungs-Flow in Echtzeit
 * – behandelt das Null-Problem, wenn `viewport.view` noch nicht gesetzt ist
 */
// -------------------------  AuswertungView -------------------------
@Composable
fun AuswertungView(viewModel: AuftraegeViewModel = remember { AuftraegeViewModel() }) {

    val auftraege by viewModel.auftraegeFlow.collectAsState()

    /* flache Schichtenliste */
    val schichtItems = remember(auftraege) {
        auftraege.flatMap { a ->
            a.schichten.orEmpty().mapNotNull { s ->
                val start = s.startDatum; val end = s.endDatum
                if (start != null && end != null)
                    Triple(a.sapANummer ?: a.id, start, end)
                else null
            }
        }.sortedBy { it.second }
    }

    /* aktueller Zoom-Modus */
    var scale by remember { mutableStateOf(TimeScale.DAILY) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        /* --- Buttons für die vier Modi --- */
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeScale.values().forEach { ts ->
                Button(
                    onClick = { scale = ts },
                    colors  = ButtonDefaults.buttonColors(
                        backgroundColor = if (scale == ts) MaterialTheme.colors.primary else MaterialTheme.colors.surface
                    )
                ) { Text(ts.label) }
            }
        }
        Spacer(Modifier.height(8.dp))

        /* --- Gantt-Diagramm --- */
        key(schichtItems) {
            SwingPanel<JScrollPane>(
                modifier = Modifier.fillMaxSize(),
                factory  = { createGanttComponent(emptyList()) },
                update = { scroll ->

                    val chartPanel = scroll.viewport.view as? ChartPanel ?: return@SwingPanel
                    val plot       = chartPanel.chart.categoryPlot
                    val axis       = plot.rangeAxis as DateAxis
                    val renderer   = plot.renderer as GanttRenderer

                    /* --- NEU: kompletten Datensatz tauschen --- */
                    val ds = buildSeriesBySap(schichtItems)
                    plot.dataset = ds

                    /* --- Achse zoom/raster --- */
                    axis.tickUnit       = scale.tickUnit
                    axis.isAutoRange    = true
                    axis.fixedAutoRange = scale.fixedRangeMillis ?: 0.0

                    /* --- Zufallsfarben pro SAP-Zeile (Series) --- */
                    for (i in 0 until ds.seriesCount) {
                        val seed   = ds.getSeriesKey(i).hashCode().toLong()   // deterministisch
                        val rand   = java.util.Random(seed)
                        val color  = java.awt.Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256))
                        renderer.setSeriesPaint(i, color)
                    }

                    /* Item-Labels einschalten (zeigt Task-Label) */
                    renderer.setDefaultItemLabelsVisible(true)
                }

            )
        }
    }
}


/* ---------- alle Schichten einer SAP-Nr. gemeinsam ---------- */
/* ---------- alle Schichten einer SAP-Nr. in nur **einer** Task-Zeile ---------- */
private fun buildSeriesBySap(
    schichtItems: List<Triple<String, LocalDateTime, LocalDateTime>>
): TaskSeriesCollection {

    val zone = ZoneId.systemDefault()
    val coll = TaskSeriesCollection()

    schichtItems
        .groupBy { it.first }                      // nach SAP gruppieren
        .toSortedMap()
        .forEach { (sapNr, shifts) ->

            /* Eltern-Task repräsentiert den Auftrag;
               Start = früheste Schicht,  Ende = späteste Schicht               */
            val startAll = shifts.minOf { it.second }
            val endAll   = shifts.maxOf { it.third }

            val parent = Task(
                "SAP $sapNr",                                   // Kategorie-Label
                Date.from(startAll.atZone(zone).toInstant()),
                Date.from(endAll  .atZone(zone).toInstant())
            )

            /* jede Schicht als Sub-Task – dadurch mehrere Balken **in derselben Zeile** */
            shifts.forEachIndexed { idx, (_, s, e) ->
                parent.addSubtask(
                    Task(
                        "(${idx + 1})",                        // (Label wird gleich ausgeblendet)
                        Date.from(s.atZone(zone).toInstant()),
                        Date.from(e.atZone(zone).toInstant())
                    )
                )
            }

            val series = TaskSeries("SAP $sapNr")
            series.add(parent)
            coll.add(series)
        }
    return coll
}



/* ---------- Chart-Factory Helfer (unverändert bis auf Achsen-Label) ---------- */
private fun createGanttComponent(
    schichtItems: List<Triple<String, LocalDateTime, LocalDateTime>>
): JScrollPane {

    val dataset = buildSeriesBySap(schichtItems)   // statt buildShiftSeries

    val chart = ChartFactory.createGanttChart(
        null,                       // Titel
        "Schicht",                  // Kategorie-Achse
        "Zeit",                     // Zeit-Achse
        dataset,
        false, true, false
    )

    val plot     = chart.categoryPlot as CategoryPlot
    val renderer = plot.renderer as GanttRenderer
    renderer.setDefaultItemLabelGenerator(StandardCategoryItemLabelGenerator())
    renderer.setDefaultItemLabelsVisible(true)

    val axis = DateAxis("Zeit").apply {
        lowerMargin = 0.02
        upperMargin = 0.02
    }
    plot.rangeAxis = axis

    val chartPanel = ChartPanel(chart).apply {
        preferredSize       = Dimension(2400, 600)
        isRangeZoomable     = true
        isMouseWheelEnabled = true
    }

    return JScrollPane(
        chartPanel,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    )
}
// ---------- enum für die vier Ansichten ----------
private enum class TimeScale(
    val label: String,
    val tickUnit: DateTickUnit,          // Raster
    val fixedRangeMillis: Double?        // Sichtfenster (null = kompletter Bereich)
) {
    HOURLY    ("1 Std",  DateTickUnit(DateTickUnitType.HOUR, 1),   60 * 60 * 1_000.0),
    H8        ("8 Std",  DateTickUnit(DateTickUnitType.HOUR, 8),   8  * 60 * 60 * 1_000.0),
    DAILY     ("1 Tag",  DateTickUnit(DateTickUnitType.DAY , 1),   24 * 60 * 60 * 1_000.0),
    WEEKLY    ("1 Woche",DateTickUnit(DateTickUnitType.DAY , 7),   7  * 24 * 60 * 60 * 1_000.0);
}
