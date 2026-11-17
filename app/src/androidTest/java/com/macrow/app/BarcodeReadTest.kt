package com.macrow.app

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/** Runs the same barcode reader the app uses on a picture of a real EAN-13 code, on the device itself. */
@RunWith(AndroidJUnit4::class)
class BarcodeReadTest {
    @Test fun readsAnEan13Picture() {
        val bitmap = InstrumentationRegistry.getInstrumentation().context.assets.open("ean13.png").use { BitmapFactory.decodeStream(it) }
        val reader = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E).build(),
        )
        val codes = Tasks.await(reader.process(InputImage.fromBitmap(bitmap, 0)), 30, TimeUnit.SECONDS)
        assertEquals("3017620422003", codes.firstOrNull()?.rawValue)
        reader.close()
    }
}
