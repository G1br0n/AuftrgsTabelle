package elemente

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

object AppStyle {

    var textScale by mutableStateOf(0.8f)

    // === Farben ===
    object Colors {
        val Primary       = Color(0xFF444444) // Hauptakzent (dunkelgrau)
        val Secondary     = Color(0xFF666666) // Alternative Schaltfläche / Leicht heller
        val Warning       = Color(0xFFFFD700) // Gelb bleibt als Kontrast erhalten (optional Grauton: 0xFF999900)
        val Error         = Color(0xFFCC3333) // Weniger aggressives Rot (eher dunkelrot)
        val Background    = Color(0xFF1E1E1E) // Tiefes, angenehmes Hintergrundgrau
        val TextPrimary   = Color(0xFFFFFFFF) // Weiß für Kontrast
        val TextSecondary = Color(0xFFAAAAAA) // Hellgrau für weniger wichtige Inhalte
    }




    object TextSizes {
        val Small: TextUnit
            get() = (14f * textScale).sp
        val Normal: TextUnit
            get() = (16f * textScale).sp
        val Large: TextUnit
            get() = (20f * textScale).sp
        val ExtraLarge: TextUnit
            get() = (24f * textScale).sp
        val UltraLarge: TextUnit
            get() = (32f * textScale).sp
    }
}


    // === Texte / Strings ===
    object Strings {
        const val AppName = "Meine Super App"
        const val ErrorMessage = "Etwas ist schiefgelaufen"
        const val Confirm = "Bestätigen"
        const val Cancel = "Abbrechen"
        const val Loading = "Lade..."
    }
