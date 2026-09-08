package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanFocus
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import java.util.Locale

@Composable
fun MagnificationBadge(
    zoomRatio: Float,
    modifier: Modifier = Modifier,
    isFrozen: Boolean = false
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Slate950.copy(alpha = 0.78f))
            .border(
                width = 1.dp,
                color = if (isFrozen) Color(0xFFEF4444) else CyanFocus.copy(alpha = 0.5f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("magnification_badge")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = if (isFrozen) Color(0xFFEF4444) else CyanFocus,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = String.format(Locale.US, "%.1fx", zoomRatio),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            if (isFrozen) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                )
                Text(
                    text = "CONGELADO",
                    color = Color(0xFFEF4444),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun ZoomControls(
    linearZoom: Float,
    zoomRatio: Float,
    minRatio: Float,
    maxRatio: Float,
    onLinearZoomChanged: (Float) -> Unit,
    onPresetSelected: (Float) -> Unit,
    onStepZoom: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Slate900.copy(alpha = 0.92f),
                        Slate950.copy(alpha = 0.98f)
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Preset zoom pills
        val presets = listOf(
            PresetOption("1x", 1.0f),
            PresetOption("2x", 2.0f),
            PresetOption("3x", 3.0f),
            PresetOption("5x", 5.0f),
            PresetOption("Max", maxRatio)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            presets.forEach { preset ->
                val isSelected = remember(zoomRatio, preset.ratio) {
                    if (preset.label == "Max") {
                        zoomRatio >= maxRatio * 0.95f
                    } else {
                        kotlin.math.abs(zoomRatio - preset.ratio) < 0.35f
                    }
                }

                val pillBg by animateColorAsState(
                    targetValue = if (isSelected) CyanFocus else Slate800,
                    animationSpec = spring(),
                    label = "pillBg"
                )

                val pillTextColor by animateColorAsState(
                    targetValue = if (isSelected) Slate950 else Color.White,
                    animationSpec = spring(),
                    label = "pillTextColor"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(pillBg)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) CyanGlow else Slate700,
                            shape = RoundedCornerShape(17.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = Color.White),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onPresetSelected(preset.ratio)
                            }
                        )
                        .padding(horizontal = 14.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("preset_${preset.label.lowercase()}")
                ) {
                    Text(
                        text = preset.label,
                        color = pillTextColor,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Zoom Slider with Precision Steppers
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Decrement Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Slate800)
                    .border(1.dp, Slate700, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onStepZoom(-0.5f)
                        }
                    )
                    .minimumInteractiveComponentSize()
                    .testTag("zoom_minus_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Reducir zoom",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Slider
            Slider(
                value = linearZoom,
                onValueChange = { newValue ->
                    onLinearZoomChanged(newValue)
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
                    .testTag("zoom_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = CyanFocus,
                    activeTrackColor = CyanFocus,
                    inactiveTrackColor = Slate800
                )
            )

            // Increment Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Slate800)
                    .border(1.dp, Slate700, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onStepZoom(0.5f)
                        }
                    )
                    .minimumInteractiveComponentSize()
                    .testTag("zoom_plus_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Aumentar zoom",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private data class PresetOption(val label: String, val ratio: Float)
