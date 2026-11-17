package com.macrow.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.macrow.app.domain.LabelBasis
import com.macrow.app.domain.LabelPrefill
import com.macrow.app.domain.LabelScan
import com.macrow.app.domain.OcrLine
import com.macrow.app.domain.parseLabel
import com.macrow.app.domain.pretty
import com.macrow.app.domain.rowsFrom
import com.macrow.app.domain.toPrefill
import com.macrow.app.ui.components.EmptyState
import com.macrow.app.ui.components.GradientButton
import com.macrow.app.ui.components.MacrowCard
import com.macrow.app.ui.components.MacrowSheet
import com.macrow.app.ui.components.SoftButton
import com.macrow.app.ui.components.TopBar
import com.macrow.app.ui.theme.Tone
import com.macrow.app.ui.theme.macrow
import java.util.concurrent.Executors

private sealed interface Phase {
    data object Aim : Phase
    data object Reading : Phase
    data class Failed(val message: String) : Phase
    data class Choose(val scan: LabelScan) : Phase
}

/** Camera and photo picker for a nutrition facts panel. The reading happens on the phone; no picture is kept. */
@Composable
fun LabelScreen(onBack: () -> Unit, onResult: (LabelPrefill) -> Unit) {
    val p = MaterialTheme.macrow
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasCamera(context)) }
    var denied by rememberSaveable { mutableStateOf(false) }
    var phase by remember { mutableStateOf<Phase>(Phase.Aim) }
    val latestResult by rememberUpdatedState(onResult)

    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val capture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).setOutputFormat(ImageCapture.OUTPUT_FORMAT_JPEG).build() }
    val worker = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { recognizer.close(); worker.shutdown() } }

    fun finish(scan: LabelScan) {
        val perServing = scan.toPrefill(LabelBasis.PER_SERVING)?.takeIf { (scan.perServing?.core ?: 0) >= 2 }
        val per100 = scan.toPrefill(LabelBasis.PER_100)?.takeIf { (scan.per100?.core ?: 0) >= 2 }
        phase = when {
            !scan.found -> Phase.Failed("Couldn't find a nutrition table in that picture. Fill the frame with the table, hold steady, and make sure it's well lit.")
            perServing != null && per100 != null -> Phase.Choose(scan)
            else -> { latestResult((perServing ?: per100)!!); Phase.Aim }
        }
    }

    fun read(image: InputImage) {
        phase = Phase.Reading
        recognizer.process(image)
            .addOnSuccessListener { text ->
                val lines = text.textBlocks.flatMap { it.lines }.mapNotNull { l -> l.boundingBox?.let { OcrLine(l.text, it.left, it.top, it.right, it.bottom) } }
                finish(parseLabel(rowsFrom(lines)))
            }
            .addOnFailureListener { phase = Phase.Failed("Couldn't read that picture. Try again.") }
    }

    val askCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> granted = ok; denied = !ok }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) runCatching { read(InputImage.fromFilePath(context, uri)) }.onFailure { phase = Phase.Failed("Couldn't open that picture.") }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = hasCamera(context) }
    LaunchedEffect(Unit) { if (!granted && !denied) askCamera.launch(Manifest.permission.CAMERA) }

    fun takePhoto() {
        phase = Phase.Reading
        capture.takePicture(
            worker,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    var why = "no picture"
                    val input = try { decodePhoto(image)?.let { InputImage.fromBitmap(it, image.imageInfo.rotationDegrees) } } catch (e: Throwable) { why = e.javaClass.simpleName + ": " + e.message; null } finally { image.close() }
                    ContextCompat.getMainExecutor(context).execute { if (input != null) read(input) else phase = Phase.Failed("Couldn't take the photo ($why). Try again.") }
                }

                override fun onError(exception: ImageCaptureException) {
                    ContextCompat.getMainExecutor(context).execute { phase = Phase.Failed("Couldn't take the photo (camera error ${exception.imageCaptureError}). Try again.") }
                }
            },
        )
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("Scan nutrition label", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (granted) {
                Box(Modifier.fillMaxWidth().height(400.dp).clip(RoundedCornerShape(28.dp)).background(p.ink)) {
                    LabelCamera(capture, onError = { phase = Phase.Failed(it) }, Modifier.fillMaxSize())
                    Canvas(Modifier.fillMaxSize().padding(24.dp)) {
                        val len = size.minDimension * 0.14f
                        val c = Color(0xFF3DDC9B)
                        fun corner(x: Float, y: Float, dx: Float, dy: Float) {
                            drawLine(c, Offset(x, y), Offset(x + dx * len, y), 6f, StrokeCap.Round)
                            drawLine(c, Offset(x, y), Offset(x, y + dy * len), 6f, StrokeCap.Round)
                        }
                        corner(0f, 0f, 1f, 1f); corner(size.width, 0f, -1f, 1f); corner(0f, size.height, 1f, -1f); corner(size.width, size.height, -1f, -1f)
                    }
                    if (phase == Phase.Reading) {
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                            Text("Reading the label…", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        }
                    }
                }
                GradientButton("Take photo", { if (phase != Phase.Reading) takePhoto() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.CameraAlt)
            } else {
                EmptyState(
                    Icons.Rounded.CameraAlt, if (denied) "Camera is off" else "Camera needed",
                    if (denied) "Turn the camera on in Android settings, or choose a photo of the label instead." else "Macrow reads the label on your phone. No picture is saved or sent.",
                ) {
                    if (denied) GradientButton("Open settings", { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())) })
                    else GradientButton("Allow camera", { askCamera.launch(Manifest.permission.CAMERA) })
                }
            }
            SoftButton("Choose a photo", { pickPhoto.launch("image/*") }, Modifier.fillMaxWidth(), tone = Tone.Blue, icon = Icons.Rounded.Image)
            (phase as? Phase.Failed)?.let { f ->
                MacrowCard(Modifier.fillMaxWidth()) {
                    Text("Couldn't read it", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(f.message, style = MaterialTheme.typography.bodyMedium, color = p.sub)
                }
            }
            Text(
                "Fill the frame with the nutrition table, hold steady and use good light. You can check and fix every number before saving.",
                style = MaterialTheme.typography.bodySmall, color = p.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    (phase as? Phase.Choose)?.let { c ->
        MacrowSheet({ phase = Phase.Aim }) {
            Text("Which column?", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text("This label shows two sets of numbers.", style = MaterialTheme.typography.bodyMedium, color = p.sub)
            Spacer(Modifier.height(12.dp))
            val s = c.scan
            GradientButton(
                "Per serving${s.servingLabel?.let { " ($it)" } ?: ""}: ${s.perServing?.kcal?.pretty() ?: "?"} kcal",
                { latestResult(s.toPrefill(LabelBasis.PER_SERVING)!!); phase = Phase.Aim }, Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            SoftButton("Per 100 g: ${s.per100?.kcal?.pretty() ?: "?"} kcal", { latestResult(s.toPrefill(LabelBasis.PER_100)!!); phase = Phase.Aim }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun LabelCamera(capture: ImageCapture, onError: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val latestError by rememberUpdatedState(onError)
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    DisposableEffect(owner) {
        var provider: ProcessCameraProvider? = null
        var mine: Array<UseCase> = emptyArray()
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val p = future.get().also { provider = it }
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                mine = arrayOf(preview, capture)
                p.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
            } catch (e: Exception) {
                latestError("The camera could not start. Choose a photo of the label instead.")
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose { runCatching { if (mine.isNotEmpty()) provider?.unbind(*mine) } }
    }
    AndroidView({ previewView }, modifier)
}

/** Turns the JPEG the camera produced into a bitmap, shrunk if it is huge so a big sensor cannot run the phone out of memory. */
private fun decodePhoto(image: ImageProxy): Bitmap? {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 3000) sample *= 2
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
}

private fun hasCamera(context: Context) = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
