package com.example.cameraoverlaysample.ui

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.cameraoverlaysample.ui.theme.CameraOverlaySampleTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun MainScreen(

) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // Camera permission state
        val cameraPermissionState = rememberPermissionState(
            Manifest.permission.CAMERA
        ) {
            // handling
        }

        if (cameraPermissionState.status.isGranted) {
            CameraContent()
            MaskContent()
        } else {
            LaunchedEffect(cameraPermissionState.status) {
                cameraPermissionState.launchPermissionRequest()
            }
        }
    }
}

@Composable
private fun CameraContent() {
    val context = LocalContext.current
    val previewView: PreviewView = remember { PreviewView(context) }
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    AndroidView(
        factory = {
            // CameraX Preview UseCase
            val previewUseCase = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            coroutineScope.launch {
                val cameraProvider = context.getCameraProvider()
                try {
                    // Must unbind the use-cases before rebinding them.
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, cameraSelector, previewUseCase
                    )
                } catch (ex: Exception) {
                    Log.e("CameraPreview", "Use case binding failed", ex)
                }
            }

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun MaskContent() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = Color.Black.copy(0.4f), size = size)
        clipRect(
            top = size.height / 3,
            left = 24.dp.toPx(),
            right = size.width - 24.dp.toPx(),
            bottom = size.height - (size.height / 3),
        ) {
            drawRect(color = Color.Transparent, size = size, blendMode = BlendMode.Clear)
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun MaskContentPreview() {
    CameraOverlaySampleTheme {
        Surface {
            MaskContent()
        }
    }
}


suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also { future ->
        future.addListener({
            continuation.resume(future.get())
        }, executor)
    }
}

val Context.executor: Executor
    get() = ContextCompat.getMainExecutor(this)
