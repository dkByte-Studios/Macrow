package com.macrow.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.macrow.app.ui.components.rememberHaptic
import com.macrow.app.ui.macrowVm
import com.macrow.app.ui.theme.Tone
import com.macrow.app.ui.theme.macrow

@Composable
fun ScanScreen(planned: Boolean, meal: Meal, day: Long, newFoodId: Long, onNewFoodHandled: () -> Unit, onBack: () -> Unit, onCreateWithBarcode: (String) -> Unit, onScanLabel: (String?) -> Unit) {
    val vm = macrowVm { ScanVm(it) }
    val p = MaterialTheme.macrow
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = rememberHaptic()
    var manual by rememberSaveable { mutableStateOf("") }
    var granted by remember { mutableStateOf(hasCamera(context)) }
    var denied by rememberSaveable { mutableStateOf(false) }

    val askCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        denied = !ok
    }
    // Coming back from Android settings after turning the camera on.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = hasCamera(context) }
    // Ask once when the screen opens; this is the one place the camera is needed.
    LaunchedEffect(Unit) { if (!granted && !denied) askCamera.launch(Manifest.permission.CAMERA) }
    // A food you just created for a barcode opens its log sheet when you come back.
    LaunchedEffect(newFoodId) { if (newFoodId > 0) { vm.openFood(newFoodId); onNewFoodHandled() } }

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar("Scan a barcode", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AnimatedContent(
                state,
                transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(120)) },
                contentKey = { it::class },
                label = "scanState",
            ) { s ->
                when (s) {
                    ScanState.Idle -> if (granted) {
                        Box(Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(28.dp)).background(p.ink)) {
                            CameraScanner(
                                onCode = { code -> haptic(HapticFeedbackType.Confirm); vm.lookup(code) },
                                onError = { vm.fail(it) },
                                modifier = Modifier.fillMaxSize(),
                            )
                            Text(
                                "Point at a barcode", Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
                                style = MaterialTheme.typography.labelLarge, color = androidx.compose.ui.graphics.Color.White,
                            )
                        }
                    } else {
                        EmptyState(
                            Icons.Rounded.CameraAlt,
                            if (denied) "Camera is off" else "Camera needed to scan",
                            if (denied) "Macrow can't see the camera. Turn it on in Android settings, or type the barcode below." else "Macrow reads the barcode on your phone. No picture is saved or sent.",
                        ) {
                            if (denied) {
                                GradientButton("Open settings", { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())) })
                            } else {
                                GradientButton("Allow camera", { askCamera.launch(Manifest.permission.CAMERA) })
                            }
                        }
                    }
                    ScanState.Looking -> EmptyState(Icons.Rounded.QrCodeScanner, "Looking it up…", "Checking your foods first, then Open Food Facts.")
                    is ScanState.Found -> EmptyState(Icons.Rounded.QrCodeScanner, "Found it", s.food.name)
                    is ScanState.Missing -> EmptyState(Icons.Rounded.QrCodeScanner, "Not in the database", "Barcode ${s.code} isn't in Open Food Facts yet. You can add it as your own food.") {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            GradientButton("Scan its nutrition label", { onScanLabel(s.code) })
                            SoftButton("Type the numbers myself", { onCreateWithBarcode(s.code) })
                            SoftButton("Scan another", { vm.reset() })
                        }
                    }
                    is ScanState.Failed -> EmptyState(Icons.Rounded.QrCodeScanner, "Couldn't look that up", s.message) {
                        SoftButton("Try again", { vm.reset() })
                    }
                }
            }
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
            SoftButton("Scan a nutrition label", { onScanLabel(null) }, Modifier.fillMaxWidth(), tone = Tone.Gold, icon = Icons.Rounded.CameraAlt)
            Text(
                "Scanned foods come from Open Food Facts and are kept on your phone for next time.",
                style = MaterialTheme.typography.bodySmall, color = p.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
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

private fun hasCamera(context: android.content.Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
