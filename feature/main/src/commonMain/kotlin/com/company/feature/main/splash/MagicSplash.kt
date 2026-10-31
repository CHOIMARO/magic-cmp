package com.company.feature.main.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicPreview
import com.company.core.designsystem.theme.MagicTheme
import com.company.core.designsystem.theme.PHONE_PREVIEW_HEIGHT
import com.company.core.designsystem.theme.PHONE_PREVIEW_WIDTH

/**
 * Splash screen of the app.
 *
 * A sparkle pen goes over the "magic" word and shows the word from left to right. At the end the pen lands at the
 * top right of the word, and the splash fades out. The splash covers the screen and blocks touches until it ends.
 *
 * @param onFinished Called when the splash ends. Remove the splash at this time.
 * @param modifier Modifier for the splash. Make the splash fill the screen.
 */
@Composable
fun MagicSplash(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val clock = remember { Animatable(0f) }
    val currentOnFinished by rememberUpdatedState(onFinished)
    LaunchedEffect(clock) {
        clock.animateTo(
            targetValue = SplashTimeline.TotalMillis.toFloat(),
            animationSpec = tween(SplashTimeline.TotalMillis, easing = LinearEasing),
        )
        currentOnFinished()
    }
    SplashContent(time = { clock.value }, modifier = modifier)
}

/**
 * Draws the splash at the time that [time] gives.
 *
 * The animation reads the time only in the draw phase, so the splash does not recompose on each frame.
 */
@Composable
private fun SplashContent(time: () -> Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .graphicsLayer { alpha = SplashTimeline.overlayAlphaAt(time()) }
            .background(MagicColors.Background)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // 프로토타입처럼 글자 묶음을 화면 가운데에서 왼쪽으로 11dp, 위로 12dp 옮긴다.
        Box(modifier = Modifier.padding(end = 22.dp).offset(y = (-12).dp)) {
            SplashWord(time)
            Canvas(modifier = Modifier.matchParentSize()) { drawSparkles(time()) }
        }
    }
}

/** The "magic" word. It shows from left to right behind the pen. */
@Composable
private fun SplashWord(time: () -> Float) {
    // 큰 글꼴 설정에서도 펜과 글자가 어긋나지 않게 글자 크기를 dp로 고정한다.
    val fontSize = with(LocalDensity.current) { SplashTimeline.WordSizeDp.dp.toSp() }
    Text(
        text = "magic",
        modifier = Modifier.revealMask(time),
        color = MagicColors.OnSurface,
        fontSize = fontSize,
        fontStyle = FontStyle.Italic,
        fontFamily = MagicTheme.fonts.serif,
        letterSpacing = (-0.02).em,
        maxLines = 1,
        softWrap = false,
        style = TextStyle(
            lineHeight = 1.em,
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
        ),
    )
}

/** Extra space around the word for the mask, so that the italic letters do not get cut. */
private val MaskSidePadding = (SplashTimeline.WordSizeDp * 0.12f).dp
private val MaskBottomPadding = (SplashTimeline.WordSizeDp * 0.32f).dp

/**
 * Shows the content from left to right, as the CSS mask of the prototype.
 *
 * The mask box is larger than the content by [MaskSidePadding] and [MaskBottomPadding]. The extra space does not
 * change the layout size.
 */
private fun Modifier.revealMask(time: () -> Float): Modifier = this
    .layout { measurable, constraints ->
        val side = MaskSidePadding.roundToPx()
        val bottom = MaskBottomPadding.roundToPx()
        val placeable = measurable.measure(constraints)
        layout(placeable.width - side * 2, placeable.height - side - bottom) { placeable.place(-side, -side) }
    }
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val (opaqueEnd, clearStart) = SplashTimeline.revealEdges(SplashTimeline.revealAt(time()))
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Black,
                1f to Color.Transparent,
                startX = size.width * opaqueEnd,
                endX = size.width * clearStart,
            ),
            blendMode = BlendMode.DstIn,
        )
    }
    .padding(start = MaskSidePadding, top = MaskSidePadding, end = MaskSidePadding, bottom = MaskBottomPadding)

/** Draws the sparks, the flash, and the pen over the word box. */
private fun DrawScope.drawSparkles(time: Float) {
    val width = size.width
    val height = size.height
    SplashTimeline.Sparks.forEach { spark ->
        val frame = SplashTimeline.sparkAt(spark, time)
        if (frame.alpha <= 0f || frame.scale <= 0f) return@forEach
        val center = Offset(width * spark.xFraction, height * spark.yFraction + frame.dropDp.dp.toPx())
        val side = spark.sizeDp.dp.toPx() * frame.scale
        val color = if (spark.light) MagicColors.OnSurface else MagicColors.AccentSparkle
        if (spark.star) {
            drawStar(StarPoints, center, side, color, frame.alpha)
        } else {
            drawCircle(color, radius = side / 2, center = center, alpha = frame.alpha)
        }
    }

    val land = Offset(width + SplashTimeline.LandOffsetDp.dp.toPx(), height * SplashTimeline.LandYFraction)
    val flash = SplashTimeline.flashAt(time)
    if (flash.alpha > 0f) {
        val side = SplashTimeline.FlashSizeDp.dp.toPx() * flash.scale
        drawStar(ThinStarPoints, land, side, MagicColors.OnSurface, flash.alpha, flash.rotation)
    }

    val pen = SplashTimeline.penAt(time)
    if (pen.alpha > 0f) {
        val center = Offset(width * pen.xFraction + pen.xOffsetDp.dp.toPx(), height * pen.yFraction)
        val side = SplashTimeline.StarSizeDp.dp.toPx() * pen.scale
        drawStar(StarPoints, center, side, MagicColors.AccentSparkle, pen.alpha, pen.rotation)
    }
}

/** CSS clip-path: polygon(50% 0%, 60% 40%, 100% 50%, 60% 60%, 50% 100%, 40% 60%, 0% 50%, 40% 40%) */
private val StarPoints = listOf(
    Offset(0.5f, 0f), Offset(0.6f, 0.4f), Offset(1f, 0.5f), Offset(0.6f, 0.6f),
    Offset(0.5f, 1f), Offset(0.4f, 0.6f), Offset(0f, 0.5f), Offset(0.4f, 0.4f),
)

/** CSS clip-path: polygon(50% 0%, 53% 47%, 100% 50%, 53% 53%, 50% 100%, 47% 53%, 0% 50%, 47% 47%) */
private val ThinStarPoints = listOf(
    Offset(0.5f, 0f), Offset(0.53f, 0.47f), Offset(1f, 0.5f), Offset(0.53f, 0.53f),
    Offset(0.5f, 1f), Offset(0.47f, 0.53f), Offset(0f, 0.5f), Offset(0.47f, 0.47f),
)

/**
 * Draws a star from [points] in a square box.
 *
 * @param points Corners of the star as fractions of the box.
 * @param center Center of the box.
 * @param side Width and height of the box.
 * @param rotation Rotation in degrees around [center].
 */
private fun DrawScope.drawStar(
    points: List<Offset>,
    center: Offset,
    side: Float,
    color: Color,
    alpha: Float,
    rotation: Float = 0f,
) {
    val path = Path().apply {
        points.forEachIndexed { index, point ->
            val x = center.x + (point.x - 0.5f) * side
            val y = center.y + (point.y - 0.5f) * side
            if (index == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    rotate(rotation, pivot = center) { drawPath(path, color, alpha = alpha) }
}

@Preview(name = "스플래시 · 쓰는 중", widthDp = PHONE_PREVIEW_WIDTH, heightDp = PHONE_PREVIEW_HEIGHT)
@Composable
private fun SplashWritingPreview() {
    MagicPreview { SplashContent(time = { 900f }, modifier = Modifier.fillMaxSize()) }
}

@Preview(name = "스플래시 · 내려앉음", widthDp = PHONE_PREVIEW_WIDTH, heightDp = PHONE_PREVIEW_HEIGHT)
@Composable
private fun SplashLandedPreview() {
    MagicPreview { SplashContent(time = { 1800f }, modifier = Modifier.fillMaxSize()) }
}
