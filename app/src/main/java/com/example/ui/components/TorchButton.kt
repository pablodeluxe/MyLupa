package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberTorch
import com.example.ui.theme.AmberTorchBright
import com.example.ui.theme.AmberTorchGlow
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate950

@Composable
fun TorchButton(
    isTorchOn: Boolean,
    onToggleTorch: () -> Unit,
    modifier: Modifier = Modifier,
    isScreenLightActive: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val isActive = isTorchOn || isScreenLightActive

    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) AmberTorch else Slate800,
        animationSpec = spring(),
        label = "torchBgColor"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isActive) Slate950 else Color.White,
        animationSpec = spring(),
        label = "torchIconColor"
    )

    val elevation by animateDpAsState(
        targetValue = if (isActive) 12.dp else 2.dp,
        animationSpec = spring(),
        label = "torchElevation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(60.dp)
                .shadow(
                    elevation = elevation,
                    shape = CircleShape,
                    spotColor = if (isActive) AmberTorchBright else Color.Transparent,
                    ambientColor = if (isActive) AmberTorchGlow else Color.Transparent
                )
                .clip(CircleShape)
                .background(
                    brush = if (isActive) {
                        Brush.verticalGradient(
                            colors = listOf(AmberTorchBright, AmberTorch)
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(Slate800, Slate950)
                        )
                    }
                )
                .border(
                    width = if (isActive) 2.dp else 1.dp,
                    color = if (isActive) AmberTorchBright else Slate700,
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = AmberTorchBright),
                    role = Role.Switch,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleTorch()
                    }
                )
                .minimumInteractiveComponentSize()
                .testTag("torch_button")
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = if (isActive) "Desactivar linterna" else "Activar linterna",
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isActive) "Linterna ON" else "Linterna",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isActive) AmberTorchBright else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
