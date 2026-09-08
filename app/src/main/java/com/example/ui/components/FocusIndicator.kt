package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.model.FocusPoint
import com.example.ui.theme.CyanFocus
import kotlin.math.roundToInt

@Composable
fun FocusIndicator(
    focusPoint: FocusPoint,
    modifier: Modifier = Modifier
) {
    val scale = remember(focusPoint.timestamp) { Animatable(1.6f) }
    val alpha = remember(focusPoint.timestamp) { Animatable(1.0f) }

    LaunchedEffect(focusPoint.timestamp) {
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        )
        alpha.animateTo(
            targetValue = 0.0f,
            animationSpec = tween(durationMillis = 1600, delayMillis = 400, easing = FastOutSlowInEasing)
        )
    }

    val boxSize = 72.dp
    val boxSizePx = remember(boxSize) { 72f }

    Canvas(
        modifier = modifier
            .offset {
                IntOffset(
                    x = (focusPoint.x - (boxSize.toPx() / 2)).roundToInt(),
                    y = (focusPoint.y - (boxSize.toPx() / 2)).roundToInt()
                )
            }
            .size(boxSize)
    ) {
        val currentScale = scale.value
        val currentAlpha = alpha.value
        val strokeWidth = 2.5.dp.toPx()
        val cornerLength = 16.dp.toPx()

        val color = CyanFocus.copy(alpha = currentAlpha)

        // Center dot
        drawCircle(
            color = color,
            radius = 3.dp.toPx(),
            center = center
        )

        // Corner bracket reticle
        val w = size.width * currentScale
        val h = size.height * currentScale
        val left = (size.width - w) / 2
        val top = (size.height - h) / 2
        val right = left + w
        val bottom = top + h

        // Top-Left corner
        drawLine(color, Offset(left, top), Offset(left + cornerLength, top), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(left, top), Offset(left, top + cornerLength), strokeWidth, StrokeCap.Round)

        // Top-Right corner
        drawLine(color, Offset(right, top), Offset(right - cornerLength, top), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(right, top), Offset(right, top + cornerLength), strokeWidth, StrokeCap.Round)

        // Bottom-Left corner
        drawLine(color, Offset(left, bottom), Offset(left + cornerLength, bottom), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(left, bottom), Offset(left, bottom - cornerLength), strokeWidth, StrokeCap.Round)

        // Bottom-Right corner
        drawLine(color, Offset(right, bottom), Offset(right - cornerLength, bottom), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(right, bottom), Offset(right, bottom - cornerLength), strokeWidth, StrokeCap.Round)

        // Subtle circular ring
        drawCircle(
            color = color.copy(alpha = currentAlpha * 0.5f),
            radius = (w / 2) * 0.7f,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}
