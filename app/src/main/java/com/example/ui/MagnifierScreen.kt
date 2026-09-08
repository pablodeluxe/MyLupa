package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.camera.CameraManager
import com.example.model.VisionFilter
import com.example.ui.components.ExposureControlSheet
import com.example.ui.components.FilterSelectorSheet
import com.example.ui.components.FocusIndicator
import com.example.ui.components.MagnificationBadge
import com.example.ui.components.TorchButton
import com.example.ui.components.ZoomControls
import com.example.ui.theme.AmberTorch
import com.example.ui.theme.AmberTorchBright
import com.example.ui.theme.CyanFocus
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagnifierScreen(
    viewModel: MagnifierViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsState()

    val cameraManager = remember {
        CameraManager(context).apply {
            onZoomStateChanged = { zoomState ->
                viewModel.updateCameraZoomState(zoomState)
            }
            onTorchStateChanged = { isTorchOn ->
                viewModel.updateTorchState(isTorchOn)
            }
            onExposureStateChanged = { current, min, max ->
                viewModel.updateExposureLimits(current, min, max)
            }
            onError = { error ->
                viewModel.setError(error)
            }
        }
    }

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    // Digital zoom offset for frozen frame
    var frozenScale by remember { mutableFloatStateOf(1f) }
    var frozenOffset by remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            cameraManager.unbind()
        }
    }

    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val exposureSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        // 1. Camera Viewfinder or Frozen Frame View
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(uiState.isFrozen) {
                    if (!uiState.isFrozen) {
                        detectTransformGestures { _, _, zoomChange, _ ->
                            if (zoomChange != 1.0f) {
                                viewModel.adjustZoomRatioRelative(zoomChange, cameraManager)
                            }
                        }
                    } else {
                        detectTransformGestures { _, pan, zoomChange, _ ->
                            frozenScale = (frozenScale * zoomChange).coerceIn(1f, 5f)
                            frozenOffset += pan
                        }
                    }
                }
                .pointerInput(uiState.isFrozen) {
                    if (!uiState.isFrozen) {
                        detectTapGestures(
                            onTap = { offset ->
                                previewViewRef?.let { pv ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.triggerFocus(offset.x, offset.y, pv, cameraManager)
                                }
                            },
                            onDoubleTap = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (uiState.zoomRatio > 2.5f) {
                                    viewModel.setZoomRatio(1.0f, cameraManager)
                                } else {
                                    viewModel.setZoomRatio(3.0f, cameraManager)
                                }
                            }
                        )
                    } else {
                        detectTapGestures(
                            onDoubleTap = {
                                frozenScale = 1f
                                frozenOffset = Offset.Zero
                            }
                        )
                    }
                }
                .drawWithContent {
                    drawContent()
                    // Live filter overlay for Inverted mode
                    if (!uiState.isFrozen && uiState.activeFilter == VisionFilter.INVERTED) {
                        drawRect(Color.White, blendMode = BlendMode.Difference)
                    }
                }
        ) {
            if (!uiState.isFrozen) {
                // Live Viewfinder
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            previewViewRef = this
                            cameraManager.bindCamera(lifecycleOwner, this)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Frozen Frame View
                val bitmap = uiState.frozenBitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Imagen congelada",
                        colorFilter = uiState.activeFilter.colorFilter,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = frozenScale
                                scaleY = frozenScale
                                translationX = frozenOffset.x
                                translationY = frozenOffset.y
                            }
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "Imagen congelada",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Screen light illumination border (fallback / extra lighting)
            if (uiState.isScreenLightActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(18.dp, Color.White.copy(alpha = 0.85f))
                )
            }

            // Crosshair guide lines (subtle grid to help read horizontally)
            CrosshairGuide(modifier = Modifier.fillMaxSize())

            // Tap Focus reticle
            uiState.focusPoint?.let { point ->
                FocusIndicator(focusPoint = point)
            }
        }

        // 2. Top Header Bar
        TopBar(
            onOpenExposure = { viewModel.setExposureSheetVisible(true) },
            onOpenFilters = { viewModel.setFilterSheetVisible(true) },
            onOpenHelp = { viewModel.setHelpVisible(true) },
            activeFilter = uiState.activeFilter,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
        )

        // 3. Floating Magnification Badge
        MagnificationBadge(
            zoomRatio = uiState.zoomRatio,
            isFrozen = uiState.isFrozen,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp)
                .windowInsetsPadding(WindowInsets.statusBars)
        )

        // 4. Bottom Control Stack
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // Zoom Slider & Quick Preset Pills
            ZoomControls(
                linearZoom = uiState.linearZoom,
                zoomRatio = uiState.zoomRatio,
                minRatio = uiState.minZoomRatio,
                maxRatio = uiState.maxZoomRatio,
                onLinearZoomChanged = { viewModel.setLinearZoom(it, cameraManager) },
                onPresetSelected = { viewModel.setZoomRatio(it, cameraManager) },
                onStepZoom = { viewModel.stepZoom(it, cameraManager) }
            )

            // Primary Action Dock (Linterna, Congelar, Filtro rápido)
            PrimaryActionDock(
                isTorchOn = uiState.isTorchOn,
                isScreenLightActive = uiState.isScreenLightActive,
                onToggleTorch = { viewModel.toggleTorch(cameraManager) },
                isFrozen = uiState.isFrozen,
                onToggleFreeze = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleFreeze(previewViewRef, cameraManager)
                },
                activeFilter = uiState.activeFilter,
                onCycleFilter = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.cycleNextFilter()
                }
            )
        }

        // 5. Modal Sheets & Dialogs
        if (uiState.isFilterSheetVisible) {
            FilterSelectorSheet(
                activeFilter = uiState.activeFilter,
                onSelectFilter = { viewModel.setVisionFilter(it) },
                onDismiss = { viewModel.setFilterSheetVisible(false) },
                sheetState = filterSheetState
            )
        }

        if (uiState.isExposureControlVisible) {
            ExposureControlSheet(
                exposureIndex = uiState.exposureCompensationIndex,
                minExposure = uiState.minExposure,
                maxExposure = uiState.maxExposure,
                onExposureChanged = { viewModel.setExposureCompensation(it, cameraManager) },
                onDismiss = { viewModel.setExposureSheetVisible(false) },
                sheetState = exposureSheetState
            )
        }

        if (uiState.isHelpVisible) {
            HelpDialog(onDismiss = { viewModel.setHelpVisible(false) })
        }

        // Error snackbar/banner
        uiState.errorMessage?.let { errorMsg ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 120.dp, start = 20.dp, end = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Slate900.copy(alpha = 0.95f))
                    .border(1.dp, AmberTorch, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = errorMsg,
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.clearError() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    onOpenExposure: () -> Unit,
    onOpenFilters: () -> Unit,
    onOpenHelp: () -> Unit,
    activeFilter: VisionFilter,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Title / Brand
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Slate900.copy(alpha = 0.85f))
                    .border(1.dp, Slate700, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = AmberTorch,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = "Lupa",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }

        // Action Icons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brightness / Exposure Button
            TopBarButton(
                icon = Icons.Default.Brightness6,
                contentDescription = "Ajustar brillo",
                onClick = onOpenExposure,
                testTag = "exposure_button"
            )

            // Filter button with active indicator
            TopBarButton(
                icon = Icons.Default.InvertColors,
                contentDescription = "Modos de contraste",
                onClick = onOpenFilters,
                isActive = activeFilter != VisionFilter.NORMAL,
                testTag = "filters_button"
            )

            // Help button
            TopBarButton(
                icon = Icons.Default.HelpOutline,
                contentDescription = "Instrucciones de uso",
                onClick = onOpenHelp,
                testTag = "help_button"
            )
        }
    }
}

@Composable
private fun TopBarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    testTag: String = ""
) {
    val haptic = LocalHapticFeedback.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(if (isActive) CyanFocus.copy(alpha = 0.2f) else Slate900.copy(alpha = 0.75f))
            .border(
                width = 1.dp,
                color = if (isActive) CyanFocus else Slate700,
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .minimumInteractiveComponentSize()
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) CyanFocus else Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun PrimaryActionDock(
    isTorchOn: Boolean,
    isScreenLightActive: Boolean,
    onToggleTorch: () -> Unit,
    isFrozen: Boolean,
    onToggleFreeze: () -> Unit,
    activeFilter: VisionFilter,
    onCycleFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Slate950)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Flashlight Button
        TorchButton(
            isTorchOn = isTorchOn,
            onToggleTorch = onToggleTorch,
            isScreenLightActive = isScreenLightActive
        )

        // Center Freeze / Resume Shutter
        FreezeButton(
            isFrozen = isFrozen,
            onClick = onToggleFreeze
        )

        // Quick Filter Cycle Button
        QuickFilterButton(
            activeFilter = activeFilter,
            onClick = onCycleFilter
        )
    }
}

@Composable
private fun FreezeButton(
    isFrozen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val outerRingColor by animateColorAsState(
        targetValue = if (isFrozen) Color(0xFFEF4444) else CyanFocus,
        animationSpec = spring(),
        label = "freezeRingColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(Slate800)
                .border(3.dp, outerRingColor, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = outerRingColor),
                    role = Role.Button,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    }
                )
                .minimumInteractiveComponentSize()
                .testTag("freeze_button")
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (isFrozen) Color(0xFFEF4444) else Color.White)
            ) {
                Icon(
                    imageVector = if (isFrozen) Icons.Default.PlayArrow else Icons.Default.CameraAlt,
                    contentDescription = if (isFrozen) "Reanudar vista en vivo" else "Congelar imagen",
                    tint = Slate950,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isFrozen) "Reanudar" else "Congelar",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isFrozen) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isFrozen) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickFilterButton(
    activeFilter: VisionFilter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isFiltered = activeFilter != VisionFilter.NORMAL

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(if (isFiltered) Slate800 else Slate900)
                .border(
                    width = if (isFiltered) 2.dp else 1.dp,
                    color = if (isFiltered) CyanFocus else Slate700,
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = CyanFocus),
                    role = Role.Button,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
                )
                .minimumInteractiveComponentSize()
                .testTag("quick_filter_button")
        ) {
            Icon(
                imageVector = Icons.Default.InvertColors,
                contentDescription = "Cambiar filtro visual",
                tint = if (isFiltered) CyanFocus else Color.White,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = activeFilter.title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isFiltered) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isFiltered) CyanFocus else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CrosshairGuide(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val strokeWidth = 1.dp.toPx()
        val guideColor = Color.White.copy(alpha = 0.12f)
        val center = Offset(size.width / 2, size.height / 2)

        // Center cross tick marks
        val tickSize = 24.dp.toPx()
        drawLine(
            color = guideColor,
            start = Offset(center.x - tickSize, center.y),
            end = Offset(center.x + tickSize, center.y),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = guideColor,
            start = Offset(center.x, center.y - tickSize),
            end = Offset(center.x, center.y + tickSize),
            strokeWidth = strokeWidth
        )

        // Horizontal reading guide lines
        val readingLineOffset = 48.dp.toPx()
        drawLine(
            color = guideColor.copy(alpha = 0.08f),
            start = Offset(40.dp.toPx(), center.y - readingLineOffset),
            end = Offset(size.width - 40.dp.toPx(), center.y - readingLineOffset),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = guideColor.copy(alpha = 0.08f),
            start = Offset(40.dp.toPx(), center.y + readingLineOffset),
            end = Offset(size.width - 40.dp.toPx(), center.y + readingLineOffset),
            strokeWidth = strokeWidth
        )
    }
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = AmberTorch
                )
                Text(
                    text = "Cómo usar la Lupa",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HelpItem(
                    title = "Ajustar Zoom",
                    description = "Desliza la barra inferior, pulsa los botones +/- o haz el gesto de pellizcar con dos dedos sobre la pantalla."
                )
                HelpItem(
                    title = "Linterna",
                    description = "Pulsa el botón de linterna para iluminar texto oscuro, frascos de medicamentos o menús."
                )
                HelpItem(
                    title = "Congelar Imagen",
                    description = "Pulsa el botón circular central para pausar la imagen y leer sin que te tiemble la mano. Puedes hacer zoom en la imagen congelada."
                )
                HelpItem(
                    title = "Enfoque Táctil",
                    description = "Toca cualquier punto de la pantalla para enfocar nítidamente las letras."
                )
                HelpItem(
                    title = "Modos de Contraste",
                    description = "Cambia a Alto Contraste o Invertido para leer texto pequeño con mayor comodidad visual."
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("help_close_button")
            ) {
                Text(text = "Entendido", color = AmberTorch, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Slate900,
        titleContentColor = Color.White,
        textContentColor = Slate300
    )
}

@Composable
private fun HelpItem(title: String, description: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = CyanFocus
            )
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Slate300,
                lineHeight = 18.sp
            )
        )
    }
}
