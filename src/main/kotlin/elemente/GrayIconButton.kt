package elemente

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GrayIconButton(
    icon: ImageVector? = null,
    label: String = "",
    tooltip: String = "",
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fullWidth: Boolean = false
) {
    var isHovered by remember { mutableStateOf(false) }
    var showTooltip by remember { mutableStateOf(false) }

    LaunchedEffect(isHovered) {
        if (isHovered) {
            kotlinx.coroutines.delay(1_000)
            if (isHovered) showTooltip = true
        } else {
            showTooltip = false
        }
    }

    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .onPointerEvent(PointerEventType.Enter) { isHovered = true }
            .onPointerEvent(PointerEventType.Exit) { isHovered = false }
    ) {
        // Hier entscheidet sich jetzt die Breite des Buttons:
        val buttonMod = if (fullWidth) Modifier.fillMaxWidth() else Modifier.wrapContentWidth()

        Button(
            onClick = onClick,
            modifier = buttonMod,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (selected) Color(0xFF777777) else Color(0xFF555555)
            )
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = label, tint = Color.White)
                if (label.isNotEmpty()) Spacer(Modifier.width(8.dp))
            }
            if (label.isNotEmpty()) Text(label, color = Color.White)
        }

        if (showTooltip && tooltip.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .background(Color(0xFFEEEEEE), shape = RoundedCornerShape(4.dp))
                    .padding(4.dp)
            ) {
                Text(tooltip, color = Color.Black, style = MaterialTheme.typography.caption)
            }
        }
    }
}


// Kompakter Button: passt sich ausschließlich dem Inhalt an
@Composable
fun GrayContentButton(
    icon: ImageVector? = null,
    label: String = "",
    tooltip: String = "",
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GrayIconButton(
        icon = icon,
        label = label,
        tooltip = tooltip,
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        fullWidth = false
    )
}

// Füllender Button: nimmt die gesamte verfügbare Breite ein
@Composable
fun GrayFillButton(
    icon: ImageVector? = null,
    label: String = "",
    tooltip: String = "",
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GrayIconButton(
        icon = icon,
        label = label,
        tooltip = tooltip,
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        fullWidth = true
    )
}