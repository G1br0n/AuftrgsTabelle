package ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import elemente.GrayIconButton
import view.AuftraegeView

import view.FahrzeugeView
import view.MaterialView
import view.MitarbeiterView


//M1 -------------------------  AppContent -------------------------
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

sealed class Screen(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Auftraege : Screen("Aufträge", Icons.Filled.Menu)
    object Mitarbeiter : Screen("Mitarbeiter", Icons.Filled.Person)
    object Fahrzeuge : Screen("Fahrzeuge", Icons.Filled.Settings)
    object Material : Screen("Materialien", Icons.Filled.Lock)
    object Qualification : Screen("Qualification", Icons.Filled.Lock)
    object Auswertung : Screen("Auswertung", Icons.Filled.Lock)
}

//M1 -------------------------  NavigationBar -------------------------
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
            ).forEach { screen ->
            GrayIconButton(
                icon = screen.icon,
                label = screen.label,
                tooltip = screen.label,
                selected = screen == selected,
                onClick = { onSelect(screen) },
                fullWidth = true,

            )
        }
    }
}

//M1 -------------------------  ContentArea -------------------------
@Composable
fun ContentArea(currentScreen: Screen) {
    when (currentScreen) {
        is Screen.Auftraege   -> AuftraegeView()
        is Screen.Mitarbeiter -> MitarbeiterView()
        is Screen.Fahrzeuge   -> FahrzeugeView()
        is Screen.Material   -> MaterialView()
        is Screen.Qualification -> TODO()
        is Screen.Auswertung -> TODO()
    }
}

//M1 -------------------------  AuftraegeView -------------------------
@Composable
fun AuftraegeView() {
    Column(Modifier.padding(16.dp)) {
        Text("Aufträge Übersicht", style = MaterialTheme.typography.h5)
        Spacer(Modifier.height(8.dp))
        Text("Hier werden die Aufträge angezeigt...")
    }
}










