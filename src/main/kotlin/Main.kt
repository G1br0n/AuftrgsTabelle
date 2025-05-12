import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

import ui.AppContent

import java.time.LocalDate



    /**
     * Einstiegspunkt der Desktop-Anwendung
     */
    fun main() = application {

        val windowState = rememberWindowState(
            size = DpSize(width = 1200.dp, height = 900.dp)
        )

        Window(
            onCloseRequest = ::exitApplication,
            title = "Mitarbeiterverwaltung",
            state = windowState,
        ) {
            MaterialTheme {
                AppContent()
            }
        }
    }

