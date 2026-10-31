package com.company.feature.editor.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.FixedGrid
import com.company.core.designsystem.component.MagicButton
import com.company.core.designsystem.component.MagicButtonStyle
import com.company.core.designsystem.component.StripeBox
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.theme.MagicColors
import com.company.domain.model.editor.Clip
import com.company.domain.model.editor.EditorProject
import com.company.domain.model.editor.PushDirection
import com.company.domain.model.editor.TransitionSlot
import com.company.domain.model.editor.TransitionType
import com.company.domain.model.editor.transitionAt
import com.company.domain.model.editor.transitionLength
import com.company.core.media.CoverColor
import com.company.core.media.SceneLook
import com.company.core.media.TransitionLook
import com.company.core.media.openingLook
import com.company.core.media.transitionLook
import com.company.feature.editor.resources.Res
import com.company.feature.editor.resources.none
import com.company.feature.editor.resources.transition_apply_all
import com.company.feature.editor.resources.transition_direction
import com.company.feature.editor.resources.transition_intro_boundary
import com.company.feature.editor.resources.transition_intro_opening
import com.company.ui.editor.displayName
import com.company.ui.media.MediaThumbnail
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan
import org.jetbrains.compose.resources.stringResource

/** Length of one loop of a transition card, in milliseconds. */
private const val CARD_LOOP_MILLIS = 1700L

/** Hue of the scene before the boundary in the cards. */
private const val CARD_FROM_HUE = 35f

/** Hue of the scene after the boundary in the cards. */
private const val CARD_TO_HUE = 200f

/** Time before the clip end of the last frame of a video clip, in seconds. It is one frame at 30 fps. */
private const val LAST_FRAME_OFFSET = 1.0 / 30

/** Guide under the title of the transition sheet. */
@Composable
internal fun transitionIntro(state: EditorState): String? = when (val slot = state.transitionSlot) {
    null -> null
    TransitionSlot.Opening -> stringResource(Res.string.transition_intro_opening)
    is TransitionSlot.Boundary -> {
        val index = state.project.clips.indexOfFirst { it.id == slot.clipId }
        if (index < 1) {
            null
        } else {
            val seconds = (state.project.transitionLength(index) * 10).toInt() / 10.0
            stringResource(Res.string.transition_intro_boundary, index, index + 1, seconds.toString())
        }
    }
}

/**
 * Transition sheet (F08): a card for each transition with a short preview in a loop, the side of the push, and
 * stringResource(Res.string.transition_apply_all).
 *
 * @param state Editor state.
 * @param onIntent Called with each user action.
 */
@Composable
internal fun TransitionSheet(state: EditorState, onIntent: (EditorIntent) -> Unit) {
    val slot = state.transitionSlot ?: return
    val opening = slot == TransitionSlot.Opening
    val current = state.project.transitionAt(slot)
    val clock = rememberTextPreviewClock(running = true)
    val types: List<TransitionType?> = listOf<TransitionType?>(null) + TransitionType.entries.filter { !opening || it.canOpen }
    FixedGrid(items = types, columns = 4, horizontalSpacing = 8.dp, verticalSpacing = 8.dp) { type ->
        val selected = current?.type == type
        Column(
            modifier = Modifier.pressClickable { onIntent(EditorIntent.PickTransition(type)) },
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val shape = RoundedCornerShape(14.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(shape)
                    .then(if (selected) Modifier.border(2.dp, MagicColors.Accent, shape) else Modifier.border(1.dp, Color.White.copy(alpha = 0.08f), shape)),
            ) {
                val progress = transitionPreviewProgress(clock.longValue, opening)
                val direction = if (type == TransitionType.PUSH) current?.direction ?: PushDirection.RIGHT else PushDirection.RIGHT
                val look = when {
                    type == null -> if (opening) openingLook(TransitionType.DISSOLVE, progress) else noTransitionLook(progress)
                    opening -> openingLook(type, progress)
                    else -> transitionLook(type, progress, direction)
                }
                TransitionScenes(look = look, fromHue = CARD_FROM_HUE, toHue = CARD_TO_HUE, modifier = Modifier.matchParentSize())
            }
            Text(
                text = type?.displayName ?: stringResource(Res.string.none),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MagicColors.OnSurface else MagicColors.OnSurface60,
                maxLines = 1,
            )
        }
    }
    if (current?.type == TransitionType.PUSH) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = stringResource(Res.string.transition_direction), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MagicColors.OnSurface70)
            Row(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MagicColors.Background).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                PushDirection.entries.forEach { direction ->
                    val selected = current.direction == direction
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .pressClickable { onIntent(EditorIntent.PickPushDirection(direction)) }
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) MagicColors.OnSurface else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            direction.icon,
                            contentDescription = direction.displayName,
                            tint = if (selected) MagicColors.Background else MagicColors.OnSurface.copy(alpha = 0.75f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
    if (!opening && state.project.clips.size > 2) {
        MagicButton(
            text = stringResource(Res.string.transition_apply_all),
            onClick = { onIntent(EditorIntent.ApplyTransitionToAll) },
            modifier = Modifier.fillMaxWidth(),
            style = MagicButtonStyle.SECONDARY,
            height = 52.dp,
        )
    }
}

/**
 * Progress of a transition preview in a loop of [CARD_LOOP_MILLIS]. Each loop shows the scene before the transition,
 * the transition, and the scene after it. The opening starts earlier and takes longer.
 *
 * @param clockMillis Time since the preview started.
 * @param opening True for the opening transition.
 * @return The progress from 0 to 1.
 */
internal fun transitionPreviewProgress(clockMillis: Long, opening: Boolean): Float {
    val phase = clockMillis % CARD_LOOP_MILLIS
    return if (opening) ((phase - 300) / 700f).coerceIn(0f, 1f) else ((phase - 500) / 600f).coerceIn(0f, 1f)
}

/** Look without a transition: the next scene shows at once in the middle. */
private fun noTransitionLook(progress: Float): TransitionLook =
    TransitionLook(from = SceneLook(), to = SceneLook(alpha = if (progress >= 0.5f) 1f else 0f))

/** Icon of the side that a push comes from. */
internal val PushDirection.icon: ImageVector
    get() = when (this) {
        PushDirection.LEFT -> Icons.AutoMirrored.Rounded.ArrowForward
        PushDirection.RIGHT -> Icons.AutoMirrored.Rounded.ArrowBack
        PushDirection.UP -> Icons.Rounded.ArrowDownward
        PushDirection.DOWN -> Icons.Rounded.ArrowUpward
    }

// =====================================================================
// Scenes of a transition
// =====================================================================

/**
 * Draws a transition with two striped placeholder scenes: black, the scene before the boundary, the scene after it,
 * and the cover color. The cards of the sheet and the placeholder stage use it.
 *
 * @param look Look of the transition at one moment.
 * @param fromHue Hue of the scene before the boundary.
 * @param toHue Hue of the scene after the boundary.
 * @param modifier Modifier for the scenes. Give it a size.
 * @param stripeOffset Stripe shift of both scenes.
 */
@Composable
internal fun TransitionScenes(
    look: TransitionLook,
    fromHue: Float,
    toHue: Float,
    modifier: Modifier = Modifier,
    stripeOffset: Dp = 0.dp,
) {
    TransitionScenes(
        look = look,
        modifier = modifier,
        from = { sceneModifier, colorFilter ->
            StripeBox(hue = fromHue, lightness = 0.5f, colorFilter = colorFilter, stripeOffset = stripeOffset, modifier = sceneModifier)
        },
        to = { sceneModifier, colorFilter ->
            StripeBox(hue = toHue, lightness = 0.5f, colorFilter = colorFilter, stripeOffset = stripeOffset, modifier = sceneModifier)
        },
    )
}

/**
 * Draws a transition: black, the scene before the boundary, the scene after it, and the cover color.
 *
 * @param look Look of the transition at one moment.
 * @param modifier Modifier for the scenes. Give it a size.
 * @param from Scene before the boundary. Give it the modifier and the color filter.
 * @param to Scene after the boundary. Give it the modifier and the color filter.
 */
@Composable
internal fun TransitionScenes(
    look: TransitionLook,
    modifier: Modifier = Modifier,
    from: @Composable (Modifier, ColorFilter?) -> Unit,
    to: @Composable (Modifier, ColorFilter?) -> Unit,
) {
    Box(modifier = modifier.clip(RoundedCornerShape(0.dp)).background(Color.Black)) {
        if (!look.fromIsBlack) Scene(look.from, from)
        Scene(look.to, to)
        if (look.coverAlpha > 0f) {
            val cover = if (look.cover == CoverColor.WHITE) Color.White else Color.Black
            Box(modifier = Modifier.matchParentSize().background(cover.copy(alpha = look.coverAlpha.coerceIn(0f, 1f))))
        }
    }
}

/**
 * Transition of the open transition sheet on the stage (v8). It plays the picked transition again and again with the
 * clips of the slot: the last frame of the clip before the boundary and the first frame of the clip after it.
 *
 * @param project Project with the clips and the transitions.
 * @param slot Place of the transition sheet.
 * @param modifier Modifier for the scenes. Give it a size.
 */
@Composable
internal fun SheetTransitionLoop(project: EditorProject, slot: TransitionSlot, modifier: Modifier = Modifier) {
    val transition = project.transitionAt(slot) ?: return
    val opening = slot == TransitionSlot.Opening
    val toIndex = when (slot) {
        TransitionSlot.Opening -> 0
        is TransitionSlot.Boundary -> project.clips.indexOfFirst { it.id == slot.clipId }
    }
    if (toIndex !in project.clips.indices || (!opening && toIndex < 1)) return
    val to = project.clips[toIndex]
    val from = if (opening) null else project.clips[toIndex - 1]
    val clock = rememberTextPreviewClock(running = true)
    val progress = transitionPreviewProgress(clock.longValue, opening)
    val look = if (opening) openingLook(transition.type, progress) else transitionLook(transition.type, progress, transition.direction)
    TransitionScenes(
        look = look,
        modifier = modifier,
        from = { sceneModifier, colorFilter -> from?.let { ClipFrame(it, it.lastFrameTime, sceneModifier, colorFilter) } },
        to = { sceneModifier, colorFilter -> ClipFrame(to, to.inPoint, sceneModifier, colorFilter) },
    )
}

/** Source time of the last frame of the clip. */
private val Clip.lastFrameTime: Double
    get() = (outPoint - LAST_FRAME_OFFSET).coerceAtLeast(inPoint)

/** Still frame of a clip at the source time [time]. A clip without a source file shows its stripes. */
@Composable
private fun ClipFrame(clip: Clip, time: Double, modifier: Modifier, colorFilter: ColorFilter?) {
    MediaThumbnail(uri = clip.sourceUri, type = clip.type, hue = clip.thumbnailHue, modifier = modifier, time = time, colorFilter = colorFilter)
}

/** One scene with the opacity, the size, the move, the turn, the skew, the blur, and the colors of [look]. */
@Composable
private fun BoxScope.Scene(look: SceneLook, content: @Composable (Modifier, ColorFilter?) -> Unit) {
    if (look.alpha <= 0f) return
    content(
        Modifier
            .matchParentSize()
            .graphicsLayer {
                alpha = look.alpha.coerceIn(0f, 1f)
                scaleX = look.scale
                scaleY = look.scale
                rotationZ = look.rotationDegrees
                translationX = look.translateX * size.width
                translationY = look.translateY * size.height
            }
            .then(if (look.blur > 0f) Modifier.blur((look.blur * 264.375f).dp) else Modifier)
            .then(
                if (look.skewXDegrees != 0f) {
                    Modifier.drawWithContent {
                        val skew = tan(look.skewXDegrees * PI / 180).toFloat()
                        withTransform({ transform(Matrix().apply { values[Matrix.SkewX] = skew }) }) { this@drawWithContent.drawContent() }
                    }
                } else {
                    Modifier
                },
            ),
        look.colorFilter(),
    )
}

/**
 * Color filter of the hue rotation and the saturation of a scene, as the CSS filters hue-rotate() and saturate().
 *
 * @return The filter, or null when the scene keeps its colors.
 */
private fun SceneLook.colorFilter(): ColorFilter? {
    if (hueDegrees == 0f && saturation == 1f) return null
    val hue = hueMatrix(hueDegrees)
    val sat = ColorMatrix().apply { setToSaturation(saturation) }
    hue.timesAssign(sat)
    return ColorFilter.colorMatrix(hue)
}

/** Hue rotation matrix of the CSS hue-rotate() filter. */
private fun hueMatrix(degrees: Float): ColorMatrix {
    val radians = degrees * PI / 180
    val c = cos(radians).toFloat()
    val s = sin(radians).toFloat()
    return ColorMatrix(
        floatArrayOf(
            0.213f + c * 0.787f - s * 0.213f, 0.715f - c * 0.715f - s * 0.715f, 0.072f - c * 0.072f + s * 0.928f, 0f, 0f,
            0.213f - c * 0.213f + s * 0.143f, 0.715f + c * 0.285f + s * 0.140f, 0.072f - c * 0.072f - s * 0.283f, 0f, 0f,
            0.213f - c * 0.213f - s * 0.787f, 0.715f - c * 0.715f + s * 0.715f, 0.072f + c * 0.928f + s * 0.072f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
}
