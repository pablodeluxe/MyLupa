package com.example.camera

import android.content.Context
import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.core.ZoomState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.model.CameraOption
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCamera2Interop::class)
class CameraManager(private val context: Context) {

    private var camera: Camera? = null
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var currentLifecycleOwner: LifecycleOwner? = null
    private var currentPreviewView: PreviewView? = null
    private var currentCameraId: String? = null

    private val systemCameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager

    var onZoomStateChanged: ((ZoomState) -> Unit)? = null
    var onTorchStateChanged: ((Boolean) -> Unit)? = null
    var onExposureStateChanged: ((Int, Int, Int) -> Unit)? = null
    var onAvailableCamerasDiscovered: ((List<CameraOption>, String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        targetCameraId: String? = null,
        onBound: (() -> Unit)? = null
    ) {
        currentLifecycleOwner = lifecycleOwner
        currentPreviewView = previewView

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                val (availableCameras, defaultId) = discoverCameras(provider)
                val selectedId = targetCameraId ?: currentCameraId ?: defaultId
                currentCameraId = selectedId

                onAvailableCamerasDiscovered?.invoke(availableCameras, selectedId)

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                val selector = buildCameraSelector(provider, selectedId)

                provider.unbindAll()
                val boundCamera = provider.bindToLifecycle(lifecycleOwner, selector, preview)
                camera = boundCamera
                cameraControl = boundCamera.cameraControl
                cameraInfo = boundCamera.cameraInfo

                // Observe zoom state
                boundCamera.cameraInfo.zoomState.observe(lifecycleOwner) { zoomState ->
                    if (zoomState != null) {
                        onZoomStateChanged?.invoke(zoomState)
                    }
                }

                // Observe torch state
                boundCamera.cameraInfo.torchState.observe(lifecycleOwner) { torchState ->
                    onTorchStateChanged?.invoke(torchState == TorchState.ON)
                }

                // Report initial exposure bounds
                val exposureState = boundCamera.cameraInfo.exposureState
                onExposureStateChanged?.invoke(
                    exposureState.exposureCompensationIndex,
                    exposureState.exposureCompensationRange.lower,
                    exposureState.exposureCompensationRange.upper
                )

                onBound?.invoke()
            } catch (e: Exception) {
                Log.e("CameraManager", "Error binding camera", e)
                onError?.invoke(e.localizedMessage ?: "Error al inicializar la cámara")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun switchCamera(cameraId: String, onBound: (() -> Unit)? = null) {
        val lifecycleOwner = currentLifecycleOwner ?: return
        val previewView = currentPreviewView ?: return
        val provider = cameraProvider ?: return

        try {
            currentCameraId = cameraId

            val preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

            val selector = buildCameraSelector(provider, cameraId)

            provider.unbindAll()
            val boundCamera = provider.bindToLifecycle(lifecycleOwner, selector, preview)
            camera = boundCamera
            cameraControl = boundCamera.cameraControl
            cameraInfo = boundCamera.cameraInfo

            boundCamera.cameraInfo.zoomState.observe(lifecycleOwner) { zoomState ->
                if (zoomState != null) {
                    onZoomStateChanged?.invoke(zoomState)
                }
            }

            boundCamera.cameraInfo.torchState.observe(lifecycleOwner) { torchState ->
                onTorchStateChanged?.invoke(torchState == TorchState.ON)
            }

            val exposureState = boundCamera.cameraInfo.exposureState
            onExposureStateChanged?.invoke(
                exposureState.exposureCompensationIndex,
                exposureState.exposureCompensationRange.lower,
                exposureState.exposureCompensationRange.upper
            )

            onBound?.invoke()
        } catch (e: Exception) {
            Log.e("CameraManager", "Error switching camera to $cameraId", e)
            onError?.invoke("No se pudo cambiar a la cámara seleccionada: ${e.localizedMessage}")
        }
    }

    private fun discoverCameras(provider: ProcessCameraProvider): Pair<List<CameraOption>, String> {
        val options = mutableListOf<CameraOption>()

        val backCameras = mutableListOf<Pair<CameraInfo, Float?>>()
        val frontCameras = mutableListOf<Pair<CameraInfo, Float?>>()

        for (info in provider.availableCameraInfos) {
            val focalLength = try {
                val camera2 = Camera2CameraInfo.from(info)
                val focalLengths = camera2.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                focalLengths?.firstOrNull()
            } catch (e: Exception) {
                null
            }

            if (info.lensFacing == CameraSelector.LENS_FACING_FRONT) {
                frontCameras.add(info to focalLength)
            } else {
                backCameras.add(info to focalLength)
            }
        }

        // Sort back cameras: prioritize cameras with flash, then by focal length
        backCameras.sortWith(
            compareByDescending<Pair<CameraInfo, Float?>> { it.first.hasFlashUnit() }
                .thenBy { it.second ?: 99f }
        )

        for ((index, pair) in backCameras.withIndex()) {
            val info = pair.first
            val focalLength = pair.second
            val id = try {
                Camera2CameraInfo.from(info).cameraId
            } catch (e: Exception) {
                "0"
            }
            val hasFlash = info.hasFlashUnit()

            val name = when {
                backCameras.size == 1 -> "Cámara Trasera Principal"
                index == 0 && (focalLength != null && focalLength < 3.2f) -> "Cámara Gran Angular / Macro"
                index == 0 -> "Cámara Trasera Principal (1x)"
                index == 1 && backCameras.size > 2 -> "Cámara Trasera (Estándar)"
                index == backCameras.lastIndex -> "Cámara Teleobjetivo / Zoom"
                else -> "Cámara Trasera #$id"
            }

            val subtitle = buildString {
                if (focalLength != null) append("${String.format(Locale.US, "%.1f", focalLength)} mm • ")
                if (hasFlash) append("Con flash") else append("Sin flash")
                append(" (ID: $id)")
            }

            options.add(
                CameraOption(
                    id = id,
                    name = name,
                    subtitle = subtitle,
                    isBackCamera = true,
                    hasFlash = hasFlash,
                    focalLengthMm = focalLength
                )
            )
        }

        for ((index, pair) in frontCameras.withIndex()) {
            val info = pair.first
            val focalLength = pair.second
            val id = try {
                Camera2CameraInfo.from(info).cameraId
            } catch (e: Exception) {
                "1"
            }
            val hasFlash = info.hasFlashUnit()

            val name = if (frontCameras.size == 1) "Cámara Frontal" else "Cámara Frontal #$id"
            val subtitle = buildString {
                if (focalLength != null) append("${String.format(Locale.US, "%.1f", focalLength)} mm • ")
                append(if (hasFlash) "Con flash" else "Sin flash")
                append(" (ID: $id)")
            }

            options.add(
                CameraOption(
                    id = id,
                    name = name,
                    subtitle = subtitle,
                    isBackCamera = false,
                    hasFlash = hasFlash,
                    focalLengthMm = focalLength
                )
            )
        }

        if (options.isEmpty()) {
            options.add(
                CameraOption(
                    id = "0",
                    name = "Cámara Principal",
                    subtitle = "Cámara predeterminada",
                    isBackCamera = true,
                    hasFlash = true
                )
            )
        }

        val defaultId = backCameras.firstOrNull()?.let {
            try {
                Camera2CameraInfo.from(it.first).cameraId
            } catch (e: Exception) {
                options.first().id
            }
        } ?: options.first().id

        return options to defaultId
    }

    private fun buildCameraSelector(provider: ProcessCameraProvider, targetCameraId: String?): CameraSelector {
        if (targetCameraId != null) {
            val selector = CameraSelector.Builder()
                .addCameraFilter { cameraInfos ->
                    val matched = cameraInfos.filter { info ->
                        try {
                            Camera2CameraInfo.from(info).cameraId == targetCameraId
                        } catch (e: Exception) {
                            false
                        }
                    }
                    if (matched.isNotEmpty()) matched else cameraInfos
                }
                .build()

            // Verify if provider can handle this selector
            if (provider.hasCamera(selector)) {
                return selector
            }
        }

        // Fallback
        return if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    fun setLinearZoom(zoom: Float) {
        try {
            cameraControl?.setLinearZoom(zoom.coerceIn(0f, 1f))
        } catch (e: Exception) {
            Log.w("CameraManager", "Error setting linear zoom", e)
        }
    }

    fun setZoomRatio(ratio: Float) {
        try {
            cameraControl?.setZoomRatio(ratio)
        } catch (e: Exception) {
            Log.w("CameraManager", "Error setting zoom ratio", e)
        }
    }

    fun setTorch(enable: Boolean, onResult: ((Boolean) -> Unit)? = null) {
        try {
            if (cameraInfo?.hasFlashUnit() == true && cameraControl != null) {
                val future = cameraControl?.enableTorch(enable)
                future?.addListener({
                    try {
                        future.get()
                        onTorchStateChanged?.invoke(enable)
                        onResult?.invoke(true)
                    } catch (e: Exception) {
                        Log.w("CameraManager", "CameraControl.enableTorch failed, using system torch mode", e)
                        val success = setSystemTorchMode(enable)
                        onResult?.invoke(success)
                    }
                }, ContextCompat.getMainExecutor(context))
            } else {
                val success = setSystemTorchMode(enable)
                onResult?.invoke(success)
            }
        } catch (e: Exception) {
            Log.w("CameraManager", "Error in setTorch", e)
            val success = setSystemTorchMode(enable)
            onResult?.invoke(success)
        }
    }

    private fun setSystemTorchMode(enable: Boolean): Boolean {
        val cm = systemCameraManager ?: return false
        val flashId = getRearCameraWithFlashId() ?: return false
        return try {
            cm.setTorchMode(flashId, enable)
            onTorchStateChanged?.invoke(enable)
            true
        } catch (e: Exception) {
            Log.e("CameraManager", "Error setting system torch mode on camera $flashId", e)
            false
        }
    }

    fun getRearCameraWithFlashId(): String? {
        val cm = systemCameraManager ?: return null
        try {
            for (id in cm.cameraIdList) {
                val chars = cm.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                if (facing == CameraCharacteristics.LENS_FACING_BACK && hasFlash) {
                    return id
                }
            }
            for (id in cm.cameraIdList) {
                val chars = cm.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                if (hasFlash) return id
            }
        } catch (e: Exception) {
            Log.e("CameraManager", "Error finding camera with flash", e)
        }
        return null
    }

    fun hasAnyFlashUnit(): Boolean {
        return (cameraInfo?.hasFlashUnit() == true) || (getRearCameraWithFlashId() != null)
    }

    fun hasFlashUnit(): Boolean {
        return cameraInfo?.hasFlashUnit() ?: (getRearCameraWithFlashId() != null)
    }

    fun setExposure(index: Int) {
        try {
            cameraControl?.setExposureCompensationIndex(index)
        } catch (e: Exception) {
            Log.w("CameraManager", "Error setting exposure", e)
        }
    }

    fun focusOnPoint(previewView: PreviewView, x: Float, y: Float) {
        try {
            val factory = previewView.meteringPointFactory
            val point = factory.createPoint(x, y)
            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                .build()
            cameraControl?.startFocusAndMetering(action)
        } catch (e: Exception) {
            Log.w("CameraManager", "Error focusing on point", e)
        }
    }

    fun capturePreviewBitmap(previewView: PreviewView): Bitmap? {
        return try {
            previewView.bitmap
        } catch (e: Exception) {
            Log.e("CameraManager", "Error capturing preview bitmap", e)
            null
        }
    }

    fun unbind() {
        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            Log.w("CameraManager", "Error unbinding camera", e)
        }
    }
}
