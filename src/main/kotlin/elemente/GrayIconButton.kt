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
    enabled: Boolean = true
) {
    var isHovered by remember { mutableStateOf(false) }
    var showTooltip by remember { mutableStateOf(false) }

    LaunchedEffect(isHovered) {
        if (isHovered && enabled) {
            kotlinx.coroutines.delay(1_000)
            if (isHovered) showTooltip = true
        } else showTooltip = false
    }

    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .onPointerEvent(PointerEventType.Enter) { isHovered = true }
            .onPointerEvent(PointerEventType.Exit) { isHovered = false }
    ) {
        val buttonMod = if (fullWidth) Modifier.fillMaxWidth() else Modifier.wrapContentWidth()

        val bgColor = when {
            !enabled   -> AppStyle.Colors.TextSecondary
            selected   -> Color(0xFF777777) // optional: eigene AppStyle-Farbe
            else       -> AppStyle.Colors.Background
        }

        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = buttonMod,
            colors = ButtonDefaults.buttonColors(
                backgroundColor      = AppStyle.Colors.Primary,
                contentColor         = AppStyle.Colors.TextPrimary,
                disabledBackgroundColor = AppStyle.Colors.TextSecondary,
                disabledContentColor = Color.DarkGray
            )
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = label, tint = AppStyle.Colors.TextPrimary)
                if (label.isNotEmpty()) Spacer(Modifier.width(8.dp))
            }
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    color = AppStyle.Colors.TextPrimary,
                    fontSize = AppStyle.TextSizes.Normal
                )
            }
        }

        if (showTooltip && tooltip.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .background(AppStyle.Colors.Secondary, RoundedCornerShape(4.dp))
                    .padding(4.dp)
            ) {
                Text(
                    tooltip,
                    color = AppStyle.Colors.TextPrimary,
                    fontSize = AppStyle.TextSizes.Small
                )
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
