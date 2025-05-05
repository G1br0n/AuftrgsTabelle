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
    fullWidth: Boolean = false,
    enabled: Boolean = true                        // <-- NEU
) {
    var isHovered  by remember { mutableStateOf(false) }
    var showTooltip by remember { mutableStateOf(false) }

    LaunchedEffect(isHovered) {
        if (isHovered && enabled) {               // Tooltip nur bei aktivem Button
            kotlinx.coroutines.delay(1_000)
            if (isHovered) showTooltip = true
        } else showTooltip = false
    }

    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .onPointerEvent(PointerEventType.Enter) { isHovered = true }
            .onPointerEvent(PointerEventType.Exit)  { isHovered = false }
    ) {
        val buttonMod = if (fullWidth) Modifier.fillMaxWidth() else Modifier.wrapContentWidth()

        Button(
            onClick  = onClick,
            enabled  = enabled,                   // <-- weiterreichen
            modifier = buttonMod,
            colors   = ButtonDefaults.buttonColors(
                backgroundColor =
                    if (!enabled) Color(0xFF999999)
                    else if (selected) Color(0xFF777777)
                    else Color(0xFF555555)
            )
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = label, tint = Color.White)
                if (label.isNotEmpty()) Spacer(Modifier.width(8.dp))
            }
            if (label.isNotEmpty()) Text(label, color = Color.White)
        }

        if (showTooltip && tooltip.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .background(Color(0xFFEEEEEE), RoundedCornerShape(4.dp))
                    .padding(4.dp)
            ) {
                Text(tooltip, color = Color.Black, style = MaterialTheme.typography.caption)
            }
        }
    }
}

/* --------------------------------------------------------------
   Wrapper bleiben gleich; sie bekommen optional das neue Flag.
-------------------------------------------------------------- */

@Composable
fun GrayContentButton(
    icon: ImageVector? = null,
    label: String = "",
    tooltip: String = "",
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) = GrayIconButton(
    icon, label, tooltip, selected, onClick,
    modifier = modifier, fullWidth = false, enabled = enabled
)

@Composable
fun GrayFillButton(
    icon: ImageVector? = null,
    label: String = "",
    tooltip: String = "",
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) = GrayIconButton(
    icon, label, tooltip, selected, onClick,
    modifier = modifier, fullWidth = true, enabled = enabled
)
