package com.example.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
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
import java.util.concurrent.TimeUnit

class CameraManager(private val context: Context) {

    private var camera: Camera? = null
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null
    private var cameraProvider: ProcessCameraProvider? = null

    var onZoomStateChanged: ((ZoomState) -> Unit)? = null
    var onTorchStateChanged: ((Boolean) -> Unit)? = null
    var onExposureStateChanged: ((Int, Int, Int) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onBound: (() -> Unit)? = null
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                // Prefer back camera for magnifying glass, fallback to front if absent
                val selector = if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    onError?.invoke("No se encontró ninguna cámara disponible en el dispositivo.")
                    return@addListener
                }

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

    fun toggleTorch(enable: Boolean) {
        try {
            if (hasFlashUnit()) {
                cameraControl?.enableTorch(enable)
            }
        } catch (e: Exception) {
            Log.w("CameraManager", "Error toggling torch", e)
        }
    }

    fun hasFlashUnit(): Boolean {
        return cameraInfo?.hasFlashUnit() ?: false
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
