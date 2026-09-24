package com.example.model

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

/**
 * Filter modes to assist vision and reading fine print under different lighting conditions.
 */
enum class VisionFilter(
    val title: String,
    val description: String,
    val colorFilter: ColorFilter?
) {
    NORMAL(
        title = "Normal",
        description = "Color natural",
        colorFilter = null
    ),
    HIGH_CONTRAST(
        title = "Alto contraste",
        description = "Aumenta la nitidez del texto",
        colorFilter = ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    1.6f, 0f, 0f, 0f, -40f,
                    0f, 1.6f, 0f, 0f, -40f,
                    0f, 0f, 1.6f, 0f, -40f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    ),
    INVERTED(
        title = "Invertido",
        description = "Fondo oscuro, texto claro",
        colorFilter = ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    ),
    GRAYSCALE(
        title = "Monocromo",
        description = "Escala de grises pura",
        colorFilter = ColorFilter.colorMatrix(
            ColorMatrix().apply { setToSaturation(0f) }
        )
    ),
    YELLOW_BLACK(
        title = "Amarillo / Negro",
        description = "Máxima legibilidad médica",
        colorFilter = ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    0.6f, 0.6f, 0.1f, 0f, 50f,
                    0.6f, 0.6f, 0.1f, 0f, 50f,
                    0f, 0f, 0f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    )
}

data class CameraOption(
    val id: String,
    val name: String,
    val subtitle: String,
    val isBackCamera: Boolean,
    val hasFlash: Boolean,
    val focalLengthMm: Float? = null
)

data class FocusPoint(
    val x: Float,
    val y: Float,
    val timestamp: Long = System.currentTimeMillis()
)

data class MagnifierUiState(
    val linearZoom: Float = 0f,
    val zoomRatio: Float = 1.0f,
    val minZoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 8.0f,
    val isTorchOn: Boolean = false,
    val isTorchAvailable: Boolean = true,
    val isScreenLightActive: Boolean = false,
    val isFrozen: Boolean = false,
    val frozenBitmap: Bitmap? = null,
    val activeFilter: VisionFilter = VisionFilter.NORMAL,
    val exposureCompensationIndex: Int = 0,
    val minExposure: Int = -4,
    val maxExposure: Int = 4,
    val isExposureControlVisible: Boolean = false,
    val isFilterSheetVisible: Boolean = false,
    val isHelpVisible: Boolean = false,
    val hasCameraPermission: Boolean = false,
    val availableCameras: List<CameraOption> = emptyList(),
    val selectedCameraId: String? = null,
    val isCameraMenuExpanded: Boolean = false,
    val focusPoint: FocusPoint? = null,
    val errorMessage: String? = null
)
