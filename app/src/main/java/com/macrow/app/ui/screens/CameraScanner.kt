package com.macrow.app.ui.screens

import android.os.Handler
import android.os.Looper
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.macrow.app.data.OpenFoodFacts
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The live camera with a barcode reader on top. Everything is read on the phone: no picture is saved or sent.
 * [onCode] fires once with the first valid product barcode, then the reader stops looking.
 */
@androidx.annotation.OptIn(markerClass = [ExperimentalGetImage::class]) // reading the camera frame for the barcode reader is the documented way to use ML Kit
@Composable
fun CameraScanner(onCode: (String) -> Unit, onError: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val latestCode by rememberUpdatedState(onCode)
    val latestError by rememberUpdatedState(onError)
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torch by remember { mutableStateOf(false) }
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    DisposableEffect(owner) {
        val main = Handler(Looper.getMainLooper())
        val executor = Executors.newSingleThreadExecutor()
        val reader = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
                .build(),
        )
        val found = AtomicBoolean(false)
        var provider: ProcessCameraProvider? = null
        var mine: Array<UseCase> = emptyArray()
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val p = future.get().also { provider = it }
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis.setAnalyzer(executor) { proxy ->
                    val media = proxy.image
                    if (media == null || found.get()) {
                        proxy.close()
                    } else {
                        reader.process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees))
                            .addOnSuccessListener { codes ->
                                val raw = codes.firstNotNullOfOrNull { c -> c.rawValue?.takeIf { OpenFoodFacts.isBarcode(it) } }
                                // A 12 digit UPC-A is the same product as a 13 digit EAN with a leading zero.
                                val code = raw?.let { if (it.length == 12) "0$it" else it }
                                if (code != null && found.compareAndSet(false, true)) main.post { latestCode(code) }
                            }
                            .addOnCompleteListener { proxy.close() }
                    }
                }
                mine = arrayOf(preview, analysis)
                camera = p.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (e: Exception) {
                latestError("The camera could not start. Type the barcode below instead.")
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            runCatching { if (mine.isNotEmpty()) provider?.unbind(*mine) }
            reader.close()
            executor.shutdown()
            camera = null
        }
    }

    Box(modifier) {
        AndroidView({ previewView }, Modifier.fillMaxSize())
        // Four corner brackets to aim with.
        Canvas(Modifier.fillMaxSize().padding(36.dp)) {
            val len = size.minDimension * 0.16f
            val w = 6f
            val c = Color(0xFF3DDC9B)
            fun corner(x: Float, y: Float, dx: Float, dy: Float) {
                drawLine(c, Offset(x, y), Offset(x + dx * len, y), w, StrokeCap.Round)
                drawLine(c, Offset(x, y), Offset(x, y + dy * len), w, StrokeCap.Round)
            }
            corner(0f, 0f, 1f, 1f)
            corner(size.width, 0f, -1f, 1f)
            corner(0f, size.height, 1f, -1f)
            corner(size.width, size.height, -1f, -1f)
        }
        val cam = camera
        if (cam != null && cam.cameraInfo.hasFlashUnit()) {
            IconButton(
                { torch = !torch; cam.cameraControl.enableTorch(torch) },
                Modifier.align(Alignment.TopEnd).padding(8.dp).size(48.dp),
            ) {
                Icon(
                    if (torch) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                    if (torch) "Turn the flashlight off" else "Turn the flashlight on",
                    tint = Color.White,
                )
            }
        }
    }
}
