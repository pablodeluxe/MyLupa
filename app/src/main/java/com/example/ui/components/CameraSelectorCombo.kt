package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CameraRear
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CameraOption
import com.example.ui.theme.AmberTorch
import com.example.ui.theme.CyanFocus
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

@Composable
fun CameraSelectorCombo(
    availableCameras: List<CameraOption>,
    selectedCameraId: String?,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCameraSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val currentCamera = availableCameras.firstOrNull { it.id == selectedCameraId }
        ?: availableCameras.firstOrNull()

    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "cameraDropdownArrow"
    )

    // Combo Box Pill Container
    Box(
        modifier = modifier.wrapContentSize(Alignment.TopStart)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Slate900.copy(alpha = 0.9f))
                .border(
                    width = 1.dp,
                    color = if (isExpanded) CyanFocus else Slate700,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = CyanFocus),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onExpandedChange(!isExpanded)
                    }
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .testTag("camera_selector_combo")
        ) {
            // Leading camera icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (currentCamera?.isBackCamera == true) Slate800 else CyanFocus.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = if (currentCamera?.isBackCamera == false) Icons.Default.CameraFront else Icons.Default.CameraRear,
                    contentDescription = null,
                    tint = if (currentCamera?.isBackCamera == false) CyanFocus else AmberTorch,
                    modifier = Modifier.size(13.dp)
                )
            }

            // Camera Name label
            Text(
                text = currentCamera?.name ?: "Cámara",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp)
            )

            // Multiple cameras badge or dropdown indicator
            if (availableCameras.size > 1) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Slate800)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "${availableCameras.size}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanFocus
                        )
                    )
                }
            }

            // Arrow indicator
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = if (isExpanded) "Ocultar lista de cámaras" else "Mostrar lista de cámaras",
                tint = if (isExpanded) CyanFocus else Slate300,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(arrowRotation)
            )
        }

        // Dropdown Menu displaying all available cameras
            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier
                    .background(Slate900)
                    .border(1.dp, Slate700, RoundedCornerShape(12.dp))
                    .widthIn(min = 280.dp, max = 340.dp)
            ) {
                // Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Elegir Cámara para la Lupa",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Selecciona la lente ideal para aumento cercano o macro",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                HorizontalDivider(color = Slate800, thickness = 1.dp)

                if (availableCameras.isEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Buscando cámaras...",
                                color = Slate300,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = { onExpandedChange(false) }
                    )
                } else {
                    availableCameras.forEach { camera ->
                        val isSelected = camera.id == (selectedCameraId ?: currentCamera?.id)

                        DropdownMenuItem(
                            text = {
                                Column(
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = camera.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) CyanFocus else Color.White
                                            )
                                        )
                                        if (camera.hasFlash) {
                                            Icon(
                                                imageVector = Icons.Default.FlashOn,
                                                contentDescription = "Flash disponible",
                                                tint = AmberTorch,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = camera.subtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp,
                                            color = if (isSelected) CyanFocus.copy(alpha = 0.8f) else Slate300
                                        )
                                    )
                                }
                            },
                            leadingIcon = {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) CyanFocus.copy(alpha = 0.2f) else Slate800)
                                ) {
                                    Icon(
                                        imageVector = if (camera.isBackCamera) Icons.Default.CameraRear else Icons.Default.CameraFront,
                                        contentDescription = null,
                                        tint = if (isSelected) CyanFocus else if (camera.isBackCamera) AmberTorch else Slate300,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            trailingIcon = {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Seleccionada",
                                        tint = CyanFocus,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCameraSelected(camera.id)
                            },
                            modifier = Modifier
                                .background(if (isSelected) Slate800.copy(alpha = 0.6f) else Color.Transparent)
                                .testTag("camera_option_${camera.id}")
                        )
                    }
                }
            }
        }
    }
