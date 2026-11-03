package com.company.feature.editor.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicTheme
import com.company.feature.editor.resources.Res
import com.company.feature.editor.resources.color_picker_area
import com.company.feature.editor.resources.color_picker_done
import com.company.feature.editor.resources.color_picker_hex
import com.company.feature.editor.resources.color_picker_hue
import com.company.feature.editor.resources.color_picker_title
import com.company.ui.editor.rgbColor
import org.jetbrains.compose.resources.stringResource

/** Background of the color picker panel. */
private val PickerSurface = Color(0xFF232129)

/** Colors of the hue bar: red, yellow, green, cyan, blue, magenta, and red again. */
private val HueColors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)

/** Puts the popup at the top left of the window. The popup content fills the window. */
private object FullWindowPosition : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize) =
        IntOffset.Zero
}

/**
 * Color picker of the text sheet (9.3 '색 직접 고르기'): a saturation and brightness area, a hue bar, a swatch, a HEX
 * field, and [완료]. It replaces the three sliders of the old picker.
 *
 * The panel opens at the bottom of the screen. Each change shows on the text at once. [완료] or a tap outside the
 * panel closes it. The same panel changes the text color and the background color. It has no transparency.
 *
 * @param picker Open picker. Its color is the start color.
 * @param onChange Called with each new color as 0xRRGGBB.
 * @param onClose Closes the picker.
 */
@Composable
internal fun ColorPickerPanel(picker: ColorPicker, onChange: (Int) -> Unit, onClose: () -> Unit) {
    val start = remember(picker.target) { rgbToHsv(picker.rgb) }
    var hue by remember(picker.target) { mutableFloatStateOf(start[0]) }
    var saturation by remember(picker.target) { mutableFloatStateOf(start[1]) }
    var value by remember(picker.target) { mutableFloatStateOf(start[2]) }
    val color = Color.hsv(hue, saturation, value)
    val rgb = color.toArgb() and 0xFFFFFF
    var hex by remember(picker.target) { mutableStateOf(rgb.toHex()) }
    val change by rememberUpdatedState(onChange)

    /** Applies a color from the area or the bar, and shows its HEX value. */
    fun pick(newHue: Float = hue, newSaturation: Float = saturation, newValue: Float = value) {
        hue = newHue
        saturation = newSaturation
        value = newValue
        val picked = Color.hsv(newHue, newSaturation, newValue).toArgb() and 0xFFFFFF
        hex = picked.toHex()
        change(picked)
    }

    val title = stringResource(Res.string.color_picker_title)
    Popup(popupPositionProvider = FullWindowPosition, onDismissRequest = onClose, properties = PopupProperties(focusable = true)) {
        // 창 밖(투명한 바탕)을 누르면 닫는다. 미리보기의 글자가 바로 보이도록 바탕을 어둡게 하지 않는다.
        Box(
            modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onClose() } },
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(start = 10.dp, end = 10.dp, bottom = 12.dp)
                    .fillMaxWidth()
                    .shadow(24.dp, RoundedCornerShape(26.dp))
                    .clip(RoundedCornerShape(26.dp))
                    .background(PickerSurface)
                    // 창 안을 누른 것은 닫기로 넘기지 않는다.
                    .pointerInput(Unit) { detectTapGestures { } }
                    .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 16.dp)
                    .semantics { paneTitle = title },
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                val areaLabel = stringResource(Res.string.color_picker_area)
                SaturationValueArea(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    color = color,
                    label = areaLabel,
                    onPick = { s, v -> pick(newSaturation = s, newValue = v) },
                )
                HueBar(hue = hue, label = stringResource(Res.string.color_picker_hue), onPick = { pick(newHue = it) })
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .semantics { contentDescription = hex },
                    )
                    HexField(
                        hex = hex,
                        onHex = { input ->
                            val clean = "#" + input.uppercase().filter { it in "0123456789ABCDEF" }.take(6)
                            hex = clean
                            // 6자리를 다 넣었을 때만 손잡이를 옮긴다. 잘못된 값이면 이전 색을 그대로 둔다.
                            clean.removePrefix("#").takeIf { it.length == 6 }?.toIntOrNull(16)?.let { parsed ->
                                val hsv = rgbToHsv(parsed)
                                // 회색은 색상이 없으므로 색상 막대는 그대로 둔다.
                                pick(newHue = if (hsv[1] > 0f) hsv[0] else hue, newSaturation = hsv[1], newValue = hsv[2])
                                hex = clean
                            }
                        },
                        onDone = { hex = (Color.hsv(hue, saturation, value).toArgb() and 0xFFFFFF).toHex() },
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .pressClickable(onClick = onClose)
                            .clip(CircleShape)
                            .background(MagicColors.Accent)
                            .padding(horizontal = 22.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = stringResource(Res.string.color_picker_done), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MagicColors.OnAccent)
                    }
                }
            }
        }
    }
}

/**
 * Saturation and brightness area. The left side is white, the right side is the full color of [hue], and the bottom
 * is black. A drag or a tap moves the round handle.
 *
 * @param hue Hue from 0 to 360.
 * @param saturation Saturation from 0 to 1.
 * @param value Brightness from 0 to 1.
 * @param color Current color. The handle shows it.
 * @param label Name of the area for screen readers.
 * @param onPick Called with the saturation and the brightness under the finger.
 */
@Composable
private fun SaturationValueArea(hue: Float, saturation: Float, value: Float, color: Color, label: String, onPick: (Float, Float) -> Unit) {
    val pick by rememberUpdatedState(onPick)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(184.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.hsv(hue, 1f, 1f))
            .background(Brush.horizontalGradient(listOf(Color.White, Color.White.copy(alpha = 0f))))
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0f), Color.Black)))
            .semantics { contentDescription = label }
            .pointerInput(Unit) {
                followFinger { position ->
                    pick((position.x / size.width).coerceIn(0f, 1f), 1f - (position.y / size.height).coerceIn(0f, 1f))
                }
            },
    ) {
        PickerHandle(x = maxWidth * saturation, y = maxHeight * (1f - value), size = 26.dp, color = color)
    }
}

/**
 * Hue bar from red through yellow, green, blue, and purple back to red. A drag or a tap moves the handle.
 *
 * @param hue Hue from 0 to 360.
 * @param label Name of the bar for screen readers.
 * @param onPick Called with the hue under the finger.
 */
@Composable
private fun HueBar(hue: Float, label: String, onPick: (Float) -> Unit) {
    val pick by rememberUpdatedState(onPick)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .clip(CircleShape)
            .background(Brush.horizontalGradient(HueColors))
            .semantics { contentDescription = label }
            .pointerInput(Unit) { followFinger { position -> pick((position.x / size.width).coerceIn(0f, 1f) * 359.9f) } },
    ) {
        PickerHandle(x = maxWidth * (hue / 360f), y = maxHeight / 2, size = 30.dp, color = Color.hsv(hue, 1f, 1f))
    }
}

/** Round handle with a white ring, centered at [x] and [y] of its parent. */
@Composable
private fun PickerHandle(x: Dp, y: Dp, size: Dp, color: Color) {
    Box(
        modifier = Modifier
            .offset(x = x - size / 2, y = y - size / 2)
            .size(size)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(color)
            .border(3.dp, Color.White, CircleShape),
    )
}

/** Calls [onMove] with the finger position from the first touch until the finger goes up. */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.followFinger(onMove: (Offset) -> Unit) {
    awaitEachGesture {
        val down = awaitFirstDown()
        down.consume()
        onMove(down.position)
        while (true) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            change.consume()
            onMove(change.position)
        }
    }
}

/**
 * HEX field of the color picker. It takes up to 6 hex digits after "#".
 *
 * @param hex Text of the field.
 * @param onHex Called with each change of the text.
 * @param onDone Called when the field loses the focus. The field then shows the current color again.
 * @param modifier Modifier for the field.
 */
@Composable
private fun HexField(hex: String, onHex: (String) -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(Res.string.color_picker_hex)
    BasicTextField(
        value = hex,
        onValueChange = { onHex(it.removePrefix("#")) },
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MagicColors.Background)
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .onFocusChanged { if (!it.isFocused) onDone() }
            .semantics { contentDescription = label },
        singleLine = true,
        textStyle = TextStyle(
            color = MagicColors.OnSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = MagicTheme.fonts.sans,
            letterSpacing = 0.96.sp,
        ),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        cursorBrush = SolidColor(MagicColors.Accent),
    )
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
    return floatArrayOf(hue.coerceIn(0f, 359.9f), if (max == 0f) 0f else delta / max, max)
}

/** Formats 0xRRGGBB as "#RRGGBB". */
private fun Int.toHex(): String = "#" + toString(16).uppercase().padStart(6, '0')
