        package ui

        import androidx.compose.foundation.*

        import androidx.compose.foundation.gestures.detectTapGestures
        import androidx.compose.foundation.layout.*
        import androidx.compose.foundation.lazy.LazyColumn
        import androidx.compose.foundation.lazy.items
        import androidx.compose.material.*
        import androidx.compose.material.icons.Icons
        import androidx.compose.material.icons.filled.*
        import androidx.compose.runtime.*
        import androidx.compose.ui.Alignment
        import androidx.compose.ui.Modifier
        import androidx.compose.ui.geometry.Offset
        import androidx.compose.ui.graphics.Color
        import androidx.compose.ui.graphics.TransformOrigin
        import androidx.compose.ui.graphics.drawscope.Stroke
        import androidx.compose.ui.graphics.drawscope.withTransform
        import androidx.compose.ui.graphics.graphicsLayer
        import androidx.compose.ui.graphics.vector.ImageVector
        import androidx.compose.ui.input.key.Key.Companion.R
        import androidx.compose.ui.input.pointer.pointerInput
        import androidx.compose.ui.layout.ContentScale
        import androidx.compose.ui.platform.LocalDensity
        import androidx.compose.ui.res.painterResource
        import androidx.compose.ui.res.useResource
        import androidx.compose.ui.text.ExperimentalTextApi
        import androidx.compose.ui.text.TextStyle
        import androidx.compose.ui.text.drawText
        import androidx.compose.ui.text.rememberTextMeasurer
        import androidx.compose.ui.text.style.TextAlign
        import androidx.compose.ui.unit.Dp
        import androidx.compose.ui.unit.dp
        import androidx.compose.ui.unit.sp
        import androidx.compose.ui.window.Dialog
        import elemente.GrayIconButton
        import repository.AuftragRepository
        import models.Auftrag
        import models.Schicht
        import org.jetbrains.skia.Bitmap
        import view.*
        import java.time.Duration
        import java.time.LocalDateTime
        import java.time.format.DateTimeFormatter
        import java.util.*
        import androidx.compose.material.MaterialTheme.typography as typography1


        data class TickLabel(val label: String, val groupLabel: String?)

        // -----------------------------------------------------------------------------
        //  Public Shell
        // -----------------------------------------------------------------------------
        @Composable
        fun AppContent() {
            var current by remember { mutableStateOf<Screen>(Screen.Auftraege) }
            Row(Modifier.fillMaxSize()) {
                NavigationBar(current) { current = it }
                ContentArea(current, Modifier.weight(1f))
            }
        }

        sealed class Screen(val label: String, val icon: ImageVector) {
            object Auftraege   : Screen("📋 Aufträge", Icons.Filled.Menu)
            object Mitarbeiter : Screen("👷🏾‍♂️ Mitarbeiter", Icons.Filled.Person)
            object Fahrzeuge   : Screen("🚘 Fahrzeuge", Icons.Filled.Edit)
            object Material    : Screen("🛠️ Materialien", Icons.Filled.Build)
            object Diagram     : Screen("📈 Diagram", Icons.Filled.Edit)
        }


        @Composable
        private fun NavigationBar(selected: Screen, onSelect: (Screen) -> Unit) {



            Column(
                Modifier.width(220.dp).fillMaxHeight().padding(16.dp),
                Arrangement.spacedBy(8.dp)
            ) {



                Spacer(modifier = Modifier.height(16.dp))

                listOf(
                    Screen.Auftraege, Screen.Mitarbeiter, Screen.Fahrzeuge,
                    Screen.Material, Screen.Diagram
                )
                    .forEach { screen ->
                        GrayIconButton(
                            label    = screen.label,
                            tooltip  = screen.label,
                            selected = screen == selected,
                            onClick  = { onSelect(screen) },
                            fullWidth= true
                        )
                    }
            }
        }

        @Composable
        private fun ContentArea(screen: Screen, modifier: Modifier) {
            Box(modifier.fillMaxSize().padding(12.dp)) {
                when (screen) {
                    Screen.Auftraege   -> AuftraegeView()
                    Screen.Mitarbeiter -> MitarbeiterView()
                    Screen.Fahrzeuge   -> FahrzeugeView()
                    Screen.Material    -> MaterialView()
                    Screen.Diagram     -> {
                        val repo = remember { AuftragRepository() }
                        val auftraege = remember { repo.getAllAuftraege() }
                        ZoomableGanttDiagram(auftraege)
                    }
                }
            }
        }



        // -----------------------------------------------------------------------------
        //  Zoom-fähiges Gantt-Diagramm
        // -----------------------------------------------------------------------------
        private val ROW_HEIGHT_DP      = 36.dp              // Zeilenhöhe
        private const val MAX_COMPOSE_PX = 262_000f         // 18-Bit-Grenze Compose

        enum class TimeScale(
            val label: String,
            val minutesPerUnit: Float,
            val unitWidth: Dp,
            val formatter: DateTimeFormatter,
        ) {
            HOUR ("Stunde",  60f,   28.dp, DateTimeFormatter.ofPattern("HH:mm", Locale.GERMANY)), // ← HIER
            DAY  ("Tag",    1440f,  28.dp, DateTimeFormatter.ofPattern("dd.MM", Locale.GERMANY)),
            WEEK ("Woche",  10080f, 28.dp, DateTimeFormatter.ofPattern("'KW' w", Locale.GERMANY)),
            MONTH("Monat",  43200f, 28.dp, DateTimeFormatter.ofPattern("MMM", Locale.GERMANY));

            fun zoomIn()  = values().getOrNull(ordinal - 1) ?: this
            fun zoomOut() = values().getOrNull(ordinal + 1) ?: this
        }




        @Composable
        fun ZoomableGanttDiagram(
            allAuftraege: List<Auftrag>,
            modifier: Modifier = Modifier,
        ) {
            var scale by remember { mutableStateOf(TimeScale.DAY) }

            // Zeitraum: aktueller Monat ± 2 Monate
            val now = LocalDateTime.now()
            val start = now.minusMonths(2)
            val end = now.plusMonths(2)

            val filteredAuftraege = remember(allAuftraege, scale) {
                allAuftraege.map { auftrag ->
                    auftrag.copy(
                        schichten = auftrag.schichten?.filter {
                            it.startDatum?.isBefore(end) == true && it.endDatum?.isAfter(start) == true
                        }
                    )
                }.filter { it.schichten?.isNotEmpty() == true }
            }

            Column(modifier) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { scale = scale.zoomIn() }) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Zoom in")
                    }
                    Text(scale.label, style = typography1.body1)
                    IconButton(onClick = { scale = scale.zoomOut() }) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Zoom out")
                    }
                }
                Spacer(Modifier.height(8.dp))

                BaseGanttDiagram(
                    auftraege = filteredAuftraege,
                    timeScale = scale,
                )

            }
        }

        @Composable
        fun BaseGanttDiagram(
            auftraege: List<Auftrag>,
            timeScale: TimeScale,
            modifier: Modifier = Modifier,
            rowHeight: Dp = ROW_HEIGHT_DP
        ) {

            val density = LocalDensity.current

            val currentTime by produceState(initialValue = LocalDateTime.now()) {
                while (true) {
                    value = LocalDateTime.now()
                    kotlinx.coroutines.delay(20000) // alle Sekunde aktualisieren
                }
            }


            var selectedSchicht by remember { mutableStateOf<Schicht?>(null) }
            var selectedAuftrag by remember { mutableStateOf<Auftrag?>(null) }
            var highlightedSchicht by remember { mutableStateOf<Schicht?>(null) }
            var dialogAuftrag by remember { mutableStateOf<Auftrag?>(null) }

            val auftragFarben = remember(auftraege) {
                val random = Random(0)
                auftraege.associate { it.id to Color(random.nextFloat(), random.nextFloat(), random.nextFloat()) }
            }

            val minStart = remember(auftraege) {
                auftraege.flatMap { it.schichten.orEmpty() }
                    .mapNotNull { it.startDatum }
                    .minOrNull()
                    ?.withMinute(0)?.withSecond(0)?.withNano(0)
                    ?: LocalDateTime.now().withMinute(0).withSecond(0).withNano(0)
            }

            val end = remember(auftraege) {
                auftraege.flatMap { it.schichten.orEmpty() }
                    .mapNotNull { it.endDatum }
                    .maxOrNull()
                    ?.withMinute(0)?.withSecond(0)?.withNano(0)
                    ?: LocalDateTime.now().withMinute(0).withSecond(0).withNano(0)
            }

            val durationMinutes = Duration.between(minStart, end).toMinutes()
            val tickCount = (durationMinutes / timeScale.minutesPerUnit).toInt() + 1

            val unitWidthPx = with(density) { timeScale.unitWidth.toPx() }
            val rawCanvasWidthPx = tickCount * unitWidthPx
            val scaleFactor = if (rawCanvasWidthPx > MAX_COMPOSE_PX) MAX_COMPOSE_PX / rawCanvasWidthPx else 1f

            val scaledUnitPx = unitWidthPx * scaleFactor
            val canvasWidthPx = rawCanvasWidthPx * scaleFactor
            val canvasHeightPx = with(density) { auftraege.size * rowHeight.toPx() }

            val hScroll = rememberScrollState()
            val vScroll = rememberScrollState()

            val headerHeightDp = 150.dp
            val totalHeaderWidthDp = with(density) { canvasWidthPx.toDp() }

            LaunchedEffect(canvasWidthPx) {
                val now = LocalDateTime.now()
                val offsetMinutes = Duration.between(minStart, now).toMinutes()
                val initialOffsetPx = (offsetMinutes / timeScale.minutesPerUnit) * scaledUnitPx
                hScroll.scrollTo(initialOffsetPx.toInt().coerceAtMost(canvasWidthPx.toInt()))
            }

            Row(modifier.fillMaxWidth()) {
                // Linke Spalte: Auftrag-Buttons
                Column(
                    modifier = Modifier
                        .width(150.dp)
                        .verticalScroll(vScroll)
                ) {
                    Spacer(Modifier.height(headerHeightDp))

                    auftraege.forEach { a ->
                        val buttonColor = auftragFarben[a.id] ?: Color.Gray
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp) // Abstand rundum
                        ) {
                            Button(
                                onClick = { dialogAuftrag = a },
                                colors = ButtonDefaults.buttonColors(backgroundColor = buttonColor),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(rowHeight - 8.dp) // Höhe anpassen wegen Padding oben/unten
                            ) {
                                Text(
                                    text = a.sapANummer ?: a.id.take(6),
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }

                    }
                }

                // Rechte Seite: Header + Diagramm
                Column(modifier = Modifier.weight(1f)) {
                    // Header (nur horizontal scrollen!)
                    Box(
                        modifier = Modifier
                            .horizontalScroll(hScroll)
                            .height(headerHeightDp)
                    ) {
                        HeaderComposableRow(
                            minStart = minStart,
                            timeScale = timeScale,
                            scaledUnitPx = scaledUnitPx,
                            tickCount = tickCount,
                            zoom = with(density) { scaledUnitPx.toDp() },
                            rotate = true,
                            height = headerHeightDp
                        )
                    }

                    // Inhalt: horizontal + vertikal scrollbar
                    Box(
                        modifier = Modifier
                            .horizontalScroll(hScroll)
                            .verticalScroll(vScroll)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(
                                    width = totalHeaderWidthDp,
                                    height = with(density) { canvasHeightPx.toDp() }
                                )
                                .pointerInput(auftraege, scaledUnitPx, minStart, timeScale) {
                                    detectTapGestures { tapOffset ->
                                        val rowIndex = (tapOffset.y / rowHeight.toPx()).toInt()
                                        val auftrag = auftraege.getOrNull(rowIndex) ?: return@detectTapGestures

                                        auftrag.schichten.orEmpty().forEach { schicht ->
                                            val start = schicht.startDatum ?: return@forEach
                                            val end = schicht.endDatum ?: return@forEach
                                            val xStart = (Duration.between(minStart, start).toMinutes() / timeScale.minutesPerUnit) * scaledUnitPx
                                            val xEnd = (Duration.between(minStart, end).toMinutes() / timeScale.minutesPerUnit) * scaledUnitPx

                                            if (tapOffset.x in xStart..xEnd) {
                                                selectedSchicht = schicht
                                                selectedAuftrag = auftrag
                                                highlightedSchicht = schicht
                                            }
                                        }
                                    }
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                repeat(tickCount + 1) { i ->
                                    val x = i * scaledUnitPx
                                    drawLine(
                                        color = Color.LightGray,
                                        start = Offset(x, 0f),
                                        end = Offset(x, size.height),
                                        strokeWidth = 1f
                                    )
                                }

                                auftraege.forEachIndexed { row, auftrag ->
                                    val y0 = row * rowHeight.toPx() + 2f
                                    val barHeight = rowHeight.toPx() - 4f
                                    val color = auftragFarben[auftrag.id] ?: Color.Blue

                                    auftrag.schichten.orEmpty().forEach { s ->
                                        val st = s.startDatum ?: return@forEach
                                        val en = s.endDatum ?: return@forEach
                                        val xStart = (Duration.between(minStart, st).toMinutes() / timeScale.minutesPerUnit) * scaledUnitPx
                                        val xEnd = (Duration.between(minStart, en).toMinutes() / timeScale.minutesPerUnit) * scaledUnitPx

                                        val isHighlighted = s == highlightedSchicht
                                        val fillColor = if (isHighlighted) color.copy(alpha = 0.4f) else color.copy(alpha = 0.7f)
                                        val borderColor = if (isHighlighted) color.copy(alpha = 1f) else color

                                        drawRect(
                                            color = fillColor,
                                            topLeft = Offset(xStart, y0),
                                            size = androidx.compose.ui.geometry.Size(xEnd - xStart, barHeight)
                                        )
                                        drawRect(
                                            color = borderColor,
                                            topLeft = Offset(xStart, y0),
                                            size = androidx.compose.ui.geometry.Size(xEnd - xStart, barHeight),
                                            style = Stroke(1.5f)
                                        )
                                        // Rote Linie für aktuelles Datum/Zeit

                                        if (currentTime.isAfter(minStart) && currentTime.isBefore(end)) {
                                            val offsetMinutes = Duration.between(minStart, currentTime).toMinutes()
                                            val xNow = (offsetMinutes / timeScale.minutesPerUnit) * scaledUnitPx
                                            drawLine(
                                                color = Color.Red,
                                                start = Offset(xNow, 0f),
                                                end = Offset(xNow, size.height),
                                                strokeWidth = 2f
                                            )
                                        }



                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (selectedSchicht != null && selectedAuftrag != null) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("Schichtdetails") },
                    text = {
                        Column {
                            Text("Auftrag: ${selectedAuftrag?.sapANummer ?: selectedAuftrag?.id}")
                            Text("Start: ${selectedSchicht?.startDatum}")
                            Text("Ende: ${selectedSchicht?.endDatum}")
                            Text(
                                "Mitarbeiter: ${
                                    selectedSchicht?.mitarbeiter?.joinToString(", ") { "${it.vorname} ${it.name}" } ?: "Keine"
                                }"
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            selectedSchicht = null
                            selectedAuftrag = null
                        }) {
                            Text("OK")
                        }
                    }
                )
            }

            if (dialogAuftrag != null) {
                Dialog(onDismissRequest = { dialogAuftrag = null }) {
                    Surface(
                        modifier = Modifier
                            .width(600.dp)
                            .height(400.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = Color.White,
                        elevation = 8.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Auftrag: ${dialogAuftrag?.sapANummer ?: dialogAuftrag?.id}",
                                    style = MaterialTheme.typography.h6
                                )
                                IconButton(onClick = { dialogAuftrag = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Schließen")
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(dialogAuftrag?.schichten?.filterNotNull().orEmpty()) { schicht ->
                                    val start = schicht.startDatum?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) ?: "-"
                                    val end = schicht.endDatum?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "-"
                                    val maCount = schicht.mitarbeiter?.size ?: 0
                                    val pauseMin = schicht.pausenZeit ?: 0
                                    val totalMinutes = if (schicht.startDatum != null && schicht.endDatum != null) {
                                        Duration.between(schicht.startDatum, schicht.endDatum).toMinutes() - pauseMin
                                    } else null
                                    val hoursFormatted = totalMinutes?.let { String.format("%.2f", it / 60f) } ?: "-"

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("\uD83D\uDE80: $start", fontSize = 13.sp)
                                        Text("\uD83C\uDFC1: $end", fontSize = 13.sp)
                                        Text("👷🏼‍♂️: $maCount", fontSize = 13.sp)
                                        Text("⏸️: $pauseMin", fontSize = 13.sp)
                                        Text("🕔: $hoursFormatted", fontSize = 13.sp)
                                    }
                                }

                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(onClick = { dialogAuftrag = null }) {
                                    Text("Schließen")
                                }
                            }
                        }
                    }
                }
            }


        }



        @Composable
        fun HeaderComposableRow(
            minStart: LocalDateTime,
            timeScale: TimeScale,
            scaledUnitPx: Float,
            tickCount: Int,
            zoom: Dp,
            rotate: Boolean = true,
            height: Dp
        ) {
            val density = LocalDensity.current
            val unitWidthDp = with(density) { scaledUnitPx.toDp() }

            val tickLabels = remember(minStart, timeScale, tickCount) {
                val ticks = mutableListOf<TickLabel>()
                var tick = minStart

                repeat(tickCount) {
                    val label = tick.format(timeScale.formatter)
                    val groupLabel = when (timeScale) {
                        TimeScale.HOUR  -> if (tick.hour == 0) tick.format(DateTimeFormatter.ofPattern("EEE dd.MM", Locale.GERMANY)) else null
                        TimeScale.DAY   -> if (tick.dayOfWeek == java.time.DayOfWeek.MONDAY) "KW ${tick.format(DateTimeFormatter.ofPattern("w"))}" else null
                        TimeScale.WEEK  -> tick.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.GERMANY))
                        TimeScale.MONTH -> tick.format(DateTimeFormatter.ofPattern("yyyy", Locale.GERMANY))
                    }
                    ticks += TickLabel(label, groupLabel)
                    tick = when (timeScale) {
                        TimeScale.HOUR  -> tick.plusHours(1)
                        TimeScale.DAY   -> tick.plusDays(1)
                        TimeScale.WEEK  -> tick.plusWeeks(1)
                        TimeScale.MONTH -> tick.plusMonths(1)
                    }
                }
                ticks
            }

            Column(
                modifier = Modifier
                    .width(unitWidthDp * tickCount)
                    .height(height)
                    .background(Color.White),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    tickLabels.forEach { tick ->
                        Box(
                            modifier = Modifier
                                .width(unitWidthDp)
                                .fillMaxHeight()
                                .border(1.dp, Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            if (rotate) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .graphicsLayer {
                                            rotationZ = -90f
                                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                                        }
                                        .wrapContentSize(unbounded = true)
                                ) {
                                    val text = tick.groupLabel?.let { "$it ${tick.label} " } ?: tick.label
                                    Text(
                                        text = text,
                                        fontSize = 12.sp,
                                        color = Color.Black,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        softWrap = true,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(tick.label, fontSize = 12.sp, color = Color.Black)
                                    tick.groupLabel?.let {
                                        Text(it, fontSize = 10.sp, color = Color.DarkGray)
                                    }
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Zeitskala: ${timeScale.label}", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }








