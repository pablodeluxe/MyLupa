package com.example.ui

import android.graphics.Bitmap
import androidx.camera.core.ZoomState
import androidx.camera.view.PreviewView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.camera.CameraManager
import com.example.model.FocusPoint
import com.example.model.MagnifierUiState
import com.example.model.VisionFilter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MagnifierViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MagnifierUiState())
    val uiState: StateFlow<MagnifierUiState> = _uiState.asStateFlow()

    private var focusDismissJob: Job? = null

    fun updateCameraZoomState(zoomState: ZoomState) {
        _uiState.update { current ->
            current.copy(
                linearZoom = zoomState.linearZoom,
                zoomRatio = zoomState.zoomRatio,
                minZoomRatio = zoomState.minZoomRatio,
                maxZoomRatio = zoomState.maxZoomRatio
            )
        }
    }

    fun updateTorchState(isOn: Boolean) {
        _uiState.update { it.copy(isTorchOn = isOn) }
    }

    fun updateExposureLimits(current: Int, min: Int, max: Int) {
        _uiState.update {
            it.copy(
                exposureCompensationIndex = current,
                minExposure = min,
                maxExposure = max
            )
        }
    }

    fun setLinearZoom(zoom: Float, cameraManager: CameraManager?) {
        val clamped = zoom.coerceIn(0f, 1f)
        _uiState.update { it.copy(linearZoom = clamped) }
        cameraManager?.setLinearZoom(clamped)
    }

    fun setZoomRatio(ratio: Float, cameraManager: CameraManager?) {
        val minRatio = _uiState.value.minZoomRatio
        val maxRatio = _uiState.value.maxZoomRatio
        val clamped = ratio.coerceIn(minRatio, maxRatio)
        cameraManager?.setZoomRatio(clamped)
    }

    fun adjustZoomRatioRelative(scaleFactor: Float, cameraManager: CameraManager?) {
        val current = _uiState.value.zoomRatio
        val target = current * scaleFactor
        setZoomRatio(target, cameraManager)
    }

    fun stepZoom(deltaMultiplier: Float, cameraManager: CameraManager?) {
        val currentRatio = _uiState.value.zoomRatio
        val newRatio = currentRatio + deltaMultiplier
        setZoomRatio(newRatio, cameraManager)
    }

    fun toggleTorch(cameraManager: CameraManager?) {
        val newState = !_uiState.value.isTorchOn
        if (cameraManager != null && cameraManager.hasFlashUnit()) {
            cameraManager.toggleTorch(newState)
        } else {
            // If device/emulator has no hardware flash unit, toggle bright screen border illuminator!
            _uiState.update { it.copy(isScreenLightActive = !_uiState.value.isScreenLightActive) }
        }
    }

    fun toggleScreenLight() {
        _uiState.update { it.copy(isScreenLightActive = !it.isScreenLightActive) }
    }

    fun toggleFreeze(previewView: PreviewView?, cameraManager: CameraManager?) {
        if (_uiState.value.isFrozen) {
            unfreeze()
        } else {
            val bitmap = previewView?.let { cameraManager?.capturePreviewBitmap(it) }
            freeze(bitmap)
        }
    }

    fun freeze(bitmap: Bitmap?) {
        _uiState.update {
            it.copy(
                isFrozen = true,
                frozenBitmap = bitmap
            )
        }
    }

    fun unfreeze() {
        _uiState.update {
            it.copy(
                isFrozen = false,
                frozenBitmap = null
            )
        }
    }

    fun setVisionFilter(filter: VisionFilter) {
        _uiState.update { it.copy(activeFilter = filter) }
    }

    fun cycleNextFilter() {
        val allFilters = VisionFilter.values()
        val currentIndex = allFilters.indexOf(_uiState.value.activeFilter)
        val nextIndex = (currentIndex + 1) % allFilters.size
        _uiState.update { it.copy(activeFilter = allFilters[nextIndex]) }
    }

    fun setExposureCompensation(index: Int, cameraManager: CameraManager?) {
        _uiState.update { it.copy(exposureCompensationIndex = index) }
        cameraManager?.setExposure(index)
    }

    fun triggerFocus(x: Float, y: Float, previewView: PreviewView, cameraManager: CameraManager?) {
        _uiState.update { it.copy(focusPoint = FocusPoint(x, y)) }
        cameraManager?.focusOnPoint(previewView, x, y)

        focusDismissJob?.cancel()
        focusDismissJob = viewModelScope.launch {
            delay(2200)
            _uiState.update { it.copy(focusPoint = null) }
        }
    }

    fun setFilterSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(isFilterSheetVisible = visible) }
    }

    fun setExposureSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(isExposureControlVisible = visible) }
    }

    fun setHelpVisible(visible: Boolean) {
        _uiState.update { it.copy(isHelpVisible = visible) }
    }

    fun setCameraPermission(granted: Boolean) {
        _uiState.update { it.copy(hasCameraPermission = granted) }
    }

    fun setError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
