package com.example

import com.example.model.CameraOption
import com.example.model.VisionFilter
import com.example.ui.MagnifierViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `test initial viewModel state`() {
        val viewModel = MagnifierViewModel()
        val state = viewModel.uiState.value

        assertEquals(1.0f, state.zoomRatio)
        assertEquals(0f, state.linearZoom)
        assertFalse(state.isFrozen)
        assertFalse(state.isTorchOn)
        assertEquals(VisionFilter.NORMAL, state.activeFilter)
        assertTrue(state.availableCameras.isEmpty())
        assertNull(state.selectedCameraId)
        assertFalse(state.isCameraMenuExpanded)
    }

    @Test
    fun `test available cameras discovery and selection`() {
        val viewModel = MagnifierViewModel()
        val dummyCameras = listOf(
            CameraOption(
                id = "0",
                name = "Cámara Trasera Principal",
                subtitle = "4.3 mm • Con flash (ID: 0)",
                isBackCamera = true,
                hasFlash = true
            ),
            CameraOption(
                id = "2",
                name = "Cámara Gran Angular / Macro",
                subtitle = "1.8 mm • Con flash (ID: 2)",
                isBackCamera = true,
                hasFlash = true
            ),
            CameraOption(
                id = "1",
                name = "Cámara Frontal",
                subtitle = "3.1 mm • Sin flash (ID: 1)",
                isBackCamera = false,
                hasFlash = false
            )
        )

        viewModel.setAvailableCameras(dummyCameras, "0")

        var state = viewModel.uiState.value
        assertEquals(3, state.availableCameras.size)
        assertEquals("0", state.selectedCameraId)

        // Open menu
        viewModel.setCameraMenuExpanded(true)
        assertTrue(viewModel.uiState.value.isCameraMenuExpanded)

        // Select macro camera
        viewModel.selectCamera("2", null)
        state = viewModel.uiState.value
        assertEquals("2", state.selectedCameraId)
        assertFalse(state.isCameraMenuExpanded)

        // Cycle to next camera
        viewModel.switchNextCamera(null)
        state = viewModel.uiState.value
        assertEquals("1", state.selectedCameraId)

        // Cycle once more (should loop back to 0)
        viewModel.switchNextCamera(null)
        state = viewModel.uiState.value
        assertEquals("0", state.selectedCameraId)
    }

    @Test
    fun `test selecting camera unfreezes frame`() {
        val viewModel = MagnifierViewModel()
        viewModel.freeze(null)
        assertTrue(viewModel.uiState.value.isFrozen)

        val dummyCameras = listOf(
            CameraOption("0", "Trasera", "ID 0", isBackCamera = true, hasFlash = true),
            CameraOption("1", "Frontal", "ID 1", isBackCamera = false, hasFlash = false)
        )
        viewModel.setAvailableCameras(dummyCameras, "0")

        // Selecting a new camera unfreezes the preview so user sees live camera feed
        viewModel.selectCamera("1", null)
        assertFalse(viewModel.uiState.value.isFrozen)
        assertNull(viewModel.uiState.value.frozenBitmap)
    }

    @Test
    fun `test vision filters cycling`() {
        val viewModel = MagnifierViewModel()
        assertEquals(VisionFilter.NORMAL, viewModel.uiState.value.activeFilter)

        viewModel.cycleNextFilter()
        assertEquals(VisionFilter.HIGH_CONTRAST, viewModel.uiState.value.activeFilter)

        viewModel.cycleNextFilter()
        assertEquals(VisionFilter.INVERTED, viewModel.uiState.value.activeFilter)
    }
}
