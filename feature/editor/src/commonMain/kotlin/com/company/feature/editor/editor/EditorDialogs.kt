package com.company.feature.editor.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicTheme
import com.company.ui.editor.rgbColor

/**
 * Popup that asks the user before a change. It shows on top of the sheets.
 *
 * @param title Title of the popup.
 * @param message Text under the title.
 * @param confirmLabel Label of the confirm button.
 * @param onConfirm Action of the confirm button.
 * @param onDismiss Action of the cancel button and of a tap outside.
 */
@Composable
internal fun EditorConfirmDialog(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    DialogCard(onDismiss = onDismiss) {
        Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp)
        Text(text = message, fontSize = 14.sp, lineHeight = 21.sp, color = MagicColors.OnSurface70)
        DialogButtons(confirmLabel = confirmLabel, onConfirm = onConfirm, onDismiss = onDismiss)
    }
}

/**
 * Color picker of the color decoration: hue, saturation, and brightness sliders, and a HEX field.
 *
 * @param initial Start color as 0xRRGGBB.
 * @param onPick Called with the picked color as 0xRRGGBB.
 * @param onDismiss Closes the picker without a change.
 */
@Composable
internal fun CustomColorDialog(initial: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val start = remember { rgbToHsv(initial) }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var saturation by remember { mutableFloatStateOf(start[1]) }
    var value by remember { mutableFloatStateOf(start[2]) }
    val color = Color.hsv(hue, saturation, value)
    val rgb = color.toArgb() and 0xFFFFFF
    var hex by remember { mutableStateOf(rgb.toHex()) }
    DialogCard(onDismiss = onDismiss) {
        Text(text = "색 직접 고르기", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(color).border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape))
            BasicTextField(
                value = hex,
                onValueChange = { input ->
                    val clean = input.uppercase().filter { it in "#0123456789ABCDEF" }.take(7)
                    hex = clean
                    clean.removePrefix("#").takeIf { it.length == 6 }?.toIntOrNull(16)?.let { parsed ->
                        val hsv = rgbToHsv(parsed)
                        hue = hsv[0]
                        saturation = hsv[1]
                        value = hsv[2]
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MagicColors.Background)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                singleLine = true,
                textStyle = TextStyle(color = MagicColors.OnSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = MagicTheme.fonts.sans),
                cursorBrush = SolidColor(MagicColors.Accent),
            )
        }
        ColorSlider(label = "색", value = hue, range = 0f..359f) {
            hue = it
            hex = Color.hsv(hue, saturation, value).toArgb().and(0xFFFFFF).toHex()
        }
        ColorSlider(label = "선명도", value = saturation, range = 0f..1f) {
            saturation = it
            hex = Color.hsv(hue, saturation, value).toArgb().and(0xFFFFFF).toHex()
        }
        ColorSlider(label = "밝기", value = value, range = 0f..1f) {
            value = it
            hex = Color.hsv(hue, saturation, value).toArgb().and(0xFFFFFF).toHex()
        }
        DialogButtons(confirmLabel = "고르기", onConfirm = { onPick(rgb) }, onDismiss = onDismiss)
    }
}

@Composable
private fun ColorSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = label, modifier = Modifier.widthIn(min = 44.dp), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MagicColors.OnSurface70)
        SheetSlider(value = value, range = range, onChange = onChange, modifier = Modifier.weight(1f))
    }
}

/** Card of a popup on a dark scrim. A tap outside the card closes it. */
@Composable
private fun DialogCard(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(MagicColors.SurfaceHighest)
                    .padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) { content() }
        }
    }
}

/** Cancel and confirm buttons at the bottom right of a popup. */
@Composable
private fun DialogButtons(confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
    ) {
        Box(
            modifier = Modifier.height(44.dp).pressClickable(onClick = onDismiss).clip(CircleShape).padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "취소", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MagicColors.OnSurface80)
        }
        Box(
            modifier = Modifier.height(44.dp).pressClickable(onClick = onConfirm).clip(CircleShape).background(MagicColors.Accent).padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = confirmLabel, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MagicColors.OnAccent)
        }
    }
}

/** Converts 0xRRGGBB to hue (0..360), saturation (0..1), and brightness (0..1). */
private fun rgbToHsv(rgb: Int): FloatArray {
    val color = rgbColor(rgb)
    val max = maxOf(color.red, color.green, color.blue)
    val min = minOf(color.red, color.green, color.blue)
    val delta = max - min
    val hue = when {
        delta == 0f -> 0f
        max == color.red -> 60f * (((color.green - color.blue) / delta) % 6f)
        max == color.green -> 60f * ((color.blue - color.red) / delta + 2f)
        else -> 60f * ((color.red - color.green) / delta + 4f)
    }.let { if (it < 0f) it + 360f else it }
    return floatArrayOf(hue.coerceIn(0f, 359f), if (max == 0f) 0f else delta / max, max)
}

/** Formats 0xRRGGBB as "#RRGGBB". */
private fun Int.toHex(): String = "#" + toString(16).uppercase().padStart(6, '0')
