package com.macrow.app.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.macrow.app.data.OpenFoodFacts
import com.macrow.app.domain.Meal
import com.macrow.app.ui.ScanState
import com.macrow.app.ui.ScanVm
import com.macrow.app.ui.UiErrors
import com.macrow.app.ui.components.EmptyState
import com.macrow.app.ui.components.GradientButton
import com.macrow.app.ui.components.LogSheet
import com.macrow.app.ui.components.MacrowCard
import com.macrow.app.ui.components.MacrowField
import com.macrow.app.ui.components.SoftButton
import com.macrow.app.ui.components.TopBar
import com.macrow.app.ui.macrowVm
import com.macrow.app.ui.theme.Tone
import com.macrow.app.ui.theme.macrow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Opens Google's barcode scanner (no camera permission needed). Returns the code, or null if you closed it. */
suspend fun scanBarcode(context: Context): String? = suspendCancellableCoroutine { cont ->
    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
        .build()
    GmsBarcodeScanning.getClient(context, options).startScan()
        .addOnSuccessListener { if (cont.isActive) cont.resume(it.rawValue) }
        .addOnCanceledListener { if (cont.isActive) cont.resume(null) }
        .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
}

@Composable
fun ScanScreen(planned: Boolean, meal: Meal, day: Long, newFoodId: Long, onNewFoodHandled: () -> Unit, onBack: () -> Unit, onCreateWithBarcode: (String) -> Unit) {
    val vm = macrowVm { ScanVm(it) }
    val p = MaterialTheme.macrow
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var manual by rememberSaveable { mutableStateOf("") }
    var started by rememberSaveable { mutableStateOf(false) }

    fun scan() {
        scope.launch {
            try {
                scanBarcode(context)?.let { vm.lookup(it) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                vm.fail("The scanner isn't available right now. Type the barcode below instead.")
            }
        }
    }

    // A food you just created for a barcode opens its log sheet when you come back.
    LaunchedEffect(newFoodId) { if (newFoodId > 0) { vm.openFood(newFoodId); started = true; onNewFoodHandled() } }

    // Open the scanner once on arrival, but not again after a rotation.
    LaunchedEffect(Unit) {
        if (!started) { started = true; scan() }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar("Scan a barcode", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AnimatedContent(
                state,
                transitionSpec = { (fadeIn(tween(240)) + slideInVertically(tween(260)) { it / 16 }) togetherWith fadeOut(tween(120)) },
                contentKey = { it::class },
                label = "scanState",
            ) { s ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (s) {
                        ScanState.Idle -> EmptyState(Icons.Rounded.QrCodeScanner, "Scan a package", "Point the camera at the barcode and the nutrition facts fill in. Foods you scan are kept for next time.")
                        ScanState.Looking -> EmptyState(Icons.Rounded.QrCodeScanner, "Looking it up…", "Checking your foods first, then Open Food Facts.")
                        is ScanState.Found -> EmptyState(Icons.Rounded.QrCodeScanner, "Found it", s.food.name)
                        is ScanState.Missing -> EmptyState(Icons.Rounded.QrCodeScanner, "Not in the database", "Barcode ${s.code} isn't in Open Food Facts yet. You can add it as your own food.") {
                            GradientButton("Create food with this barcode", { onCreateWithBarcode(s.code) })
                        }
                        is ScanState.Failed -> EmptyState(Icons.Rounded.QrCodeScanner, "Couldn't look that up", s.message)
                    }
                }
            }
            GradientButton("Scan with the camera", { vm.reset(); scan() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.QrCodeScanner)
            MacrowCard {
                Text("Or type the numbers under the barcode", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                val valid = OpenFoodFacts.isBarcode(manual)
                MacrowField(
                    manual, { manual = it.filter(Char::isDigit) }, "Barcode", Modifier.fillMaxWidth(), keyboard = KeyboardType.Number,
                    error = if (manual.isNotEmpty() && !valid) "Barcodes have 8 to 14 digits" else null, ime = ImeAction.Search, maxLength = 14,
                    onDone = { if (valid) vm.lookup(manual) },
                )
                Spacer(Modifier.height(8.dp))
                SoftButton("Look up", { if (valid) vm.lookup(manual) else UiErrors.say("Enter 8 to 14 digits.") }, Modifier.fillMaxWidth(), tone = Tone.Blue)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    (state as? ScanState.Found)?.let { found ->
        LogSheet(
            found.food, meal, if (planned) "Add to plan" else "Add to diary",
            onConfirm = { m, servings ->
                vm.log(day, m.name, found.food, servings, planned)
                UiErrors.say("Added ${found.food.name} to ${m.label.lowercase()}")
                onBack()
            },
            onDismiss = { vm.reset() },
        )
    }
}
