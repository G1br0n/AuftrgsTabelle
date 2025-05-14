package view

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import elemente.GrayIconButton
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
import ui.BaseGanttDiagram
import viewModel.AuftraegeViewModel
import java.awt.Dimension
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import javax.swing.JScrollPane
import javax.swing.ScrollPaneConstants

private const val ROW_HEIGHT = 46
private val AXIS_DATE_TIME = SimpleDateFormat("dd.MM HH:mm")
private val AXIS_DATE      = SimpleDateFormat("dd.MM")
private val dtfField       = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

private enum class Zoom(val label: String, val unit: DateTickUnit, val window: Duration, val fmt: SimpleDateFormat) {
    HOUR("1 h", DateTickUnit(DateTickUnitType.HOUR,1),  Duration.ofHours(1),  AXIS_DATE_TIME),
    DAY ("1 d", DateTickUnit(DateTickUnitType.HOUR,2),  Duration.ofDays(1),   AXIS_DATE_TIME),
    WEEK("1 w", DateTickUnit(DateTickUnitType.DAY ,1),  Duration.ofDays(7),   AXIS_DATE),
    MONTH("1 m", DateTickUnit(DateTickUnitType.DAY ,7),  Duration.ofDays(30),  AXIS_DATE)
}

// -----------------------------------------------------------------------------
//  Auswertung – angepasstes Gantt-Chart (+/-1 Monat um „heute“)
// -----------------------------------------------------------------------------
@Composable
fun AuswertungView(vm: AuftraegeViewModel = remember { AuftraegeViewModel() }) {

    /* ------------------------------------------------------------ *
     *  1) Daten laden und vorbereiten
     * ------------------------------------------------------------ */
    val auftraege by vm.auftraegeFlow.collectAsState()
    val schichten = remember(auftraege) {
        auftraege.flatMap { a ->
            a.schichten.orEmpty().mapNotNull { s ->
                val (start, end) = s.startDatum to s.endDatum
                if (start != null && end != null) Triple(a.sapANummer ?: a.id, start, end) else null
            }
        }.sortedBy { it.second }
    }

    val dataset  = remember(schichten) { buildDataset(schichten) }
    val colorMap = remember(dataset)   { mutableMapOf<String, java.awt.Color>() }

    /* ------------------------------------------------------------ *
     *  2) Standard-Zeitfenster: Vormonat + Aktuell + Folgemonat
     * ------------------------------------------------------------ */
    val zone = ZoneId.systemDefault()
    val today       = LocalDate.now(zone)                      // z. B. 2025-05-13
    val defaultFrom = today.minusMonths(1)                     // 1 Monat zurück
        .withDayOfMonth(1)                                     // 1. Tag
        .atStartOfDay()                                        // 00:00
    val nextMonth   = today.plusMonths(1)
    val defaultTo   = LocalDateTime.of(                        // letzter Tag + 23:59:59
        nextMonth.year,
        nextMonth.month,
        nextMonth.lengthOfMonth(),
        23, 59, 59, 999_000_000
    )

    /* ------------------------------------------------------------ *
     *  3) UI-State
     * ------------------------------------------------------------ */
    val earliest = schichten.minOfOrNull { it.second } ?: defaultFrom
    val latest   = schichten.maxOfOrNull { it.third  } ?: defaultTo
    val spanMs   = Duration.between(earliest, latest).toMillis().toDouble()

    var zoom       by remember { mutableStateOf(Zoom.DAY) }
    var slider     by remember { mutableStateOf(0f) }
    var rangeStart by remember { mutableStateOf<LocalDateTime?>(defaultFrom) }
    var rangeEnd   by remember { mutableStateOf<LocalDateTime?>(defaultTo) }

    /* ------------------------------------------------------------ *
     *  4) UI
     * ------------------------------------------------------------ */
    Column(Modifier.fillMaxSize()) {
        /* ---------- Header-Controls ------------------------------------ */
        Row(
            Modifier.fillMaxWidth(),
            Arrangement.spacedBy(8.dp),
            Alignment.CenterVertically
        ) {
            Zoom.values().forEach { z ->
                OutlinedButton(
                    onClick = { zoom = z },
                    colors = ButtonDefaults.outlinedButtonColors()
                ) { Text(z.label) }
            }

            fun fmt(d: LocalDateTime?) = d?.format(dtfField) ?: ""

            OutlinedTextField(
                value = fmt(rangeStart),
                onValueChange = { t ->
                    rangeStart = t.takeIf { it.isNotBlank() }
                        ?.let { runCatching { LocalDateTime.parse(it, dtfField) }.getOrNull() }
                },
                label = { Text("von") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = fmt(rangeEnd),
                onValueChange = { t ->
                    rangeEnd = t.takeIf { it.isNotBlank() }
                        ?.let { runCatching { LocalDateTime.parse(it, dtfField) }.getOrNull() }
                },
                label = { Text("bis") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            GrayIconButton(
                label    = "Reset",
                tooltip  = "Reset",
                icon     = Icons.Default.Refresh,
                selected = false,
                onClick  = {
                    rangeStart = defaultFrom
                    rangeEnd   = defaultTo
                    slider     = 0f
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        /* ---------- Timeline-Slider ------------------------------------ */
        val windowMs = zoom.window.toMillis().toDouble()
        Slider(
            value         = slider,
            onValueChange = { slider = it },
            enabled       = (rangeStart == null && rangeEnd == null && spanMs > windowMs),
            modifier      = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        /* ---------- Gantt-Chart ---------------------------------------- */
        key(dataset) {
            SwingPanel<JScrollPane>(
                factory  = { createChart(dataset) },
                modifier = Modifier.fillMaxSize()
            ) { scroll ->
                val panel    = scroll.viewport.view as? ChartPanel ?: return@SwingPanel
                val plot     = panel.chart.categoryPlot
                val axis     = plot.rangeAxis as DateAxis
                val renderer = plot.renderer as GanttRenderer

                plot.dataset            = dataset
                axis.tickUnit           = zoom.unit
                axis.dateFormatOverride = zoom.fmt
                renderer.defaultItemLabelGenerator = StandardCategoryItemLabelGenerator()

                /* ---------- Farben pro SAP -------------------------------- */
                repeat(dataset.seriesCount) { idx ->
                    val key = dataset.getSeriesKey(idx).toString()
                    val color = colorMap.getOrPut(key) {
                        java.awt.Color.getHSBColor((idx * 0.618f) % 1f, .5f, .75f)
                    }
                    renderer.setSeriesPaint(idx, color)
                }

                /* ---------- Achsen-Range ---------------------------------- */
                val startMs = (rangeStart ?: earliest)
                    .atZone(zone).toInstant().toEpochMilli().toDouble()
                val endMs   = (rangeEnd   ?: earliest.plus(zoom.window))
                    .atZone(zone).toInstant().toEpochMilli().toDouble()

                if (rangeStart != null && rangeEnd != null) {
                    axis.setRange(startMs, endMs)
                } else {
                    val pad   = zoom.window.toMillis() / 4.0
                    val begin = earliest.atZone(zone)
                        .toInstant().toEpochMilli().toDouble() - pad
                    val full  = spanMs + pad
                    val s     = begin + (full - windowMs) * slider
                    axis.setRange(s, s + windowMs)
                }

                /* ---------- Panel-Größe ----------------------------------- */
                panel.preferredSize = Dimension(1200, dataset.rowCount * ROW_HEIGHT)
            }
        }
    }
}


// -----------------------------------------------------------------------------
//  Helpers
// -----------------------------------------------------------------------------
private fun buildDataset(items: List<Triple<String, LocalDateTime, LocalDateTime>>): TaskSeriesCollection {
    val zone = ZoneId.systemDefault()
    val coll = TaskSeriesCollection()

    items.groupBy { it.first }.toSortedMap().forEach { (sap, list) ->
        val parentStart = list.minOf { it.second }
        val parentEnd   = list.maxOf { it.third }
        val parentTask  = Task("SAP $sap", Date.from(parentStart.atZone(zone).toInstant()), Date.from(parentEnd.atZone(zone).toInstant()))
        list.forEachIndexed { i, (_, s, e) ->
            parentTask.addSubtask(Task("${i+1}", Date.from(s.atZone(zone).toInstant()), Date.from(e.atZone(zone).toInstant())))
        }
        coll.add(TaskSeries(sap).apply { add(parentTask) })
    }
    return coll
}

private fun createChart(ds: TaskSeriesCollection): JScrollPane {
    val chart = ChartFactory.createGanttChart(
        null, // Titel
        "Kategorie", // Domain-Achse
        "Zeit",      // Range-Achse
        ds,
        false, // Legende
        true,  // Tooltips
        false  // URLs
    )

    val plot = chart.categoryPlot as CategoryPlot
    val gantt = plot.renderer as GanttRenderer

    gantt.apply {
        itemMargin      = 0.2
        setShadowVisible(false)   // war vorher isShadowVisible → setShadowVisible(Boolean)
        maximumBarWidth = 0.95
        setDefaultItemLabelsVisible(true)
    }

    (plot.domainAxis).apply {
        categoryMargin = 0.05
        lowerMargin    = 0.02
        upperMargin    = 0.02
    }
    (plot.rangeAxis as DateAxis).apply {
        lowerMargin = 0.02
        upperMargin = 0.02
    }

    val panel = ChartPanel(chart).apply {
        isMouseWheelEnabled = true
        isRangeZoomable     = true
    }
    return JScrollPane(
        panel,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    )
}

