package com.macrow.app

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.macrow.app.domain.LabelScan
import com.macrow.app.domain.OcrLine
import com.macrow.app.domain.parseLabel
import com.macrow.app.domain.rowsFrom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/** Reads pictures of nutrition labels with the same text reader and parser the app uses, on the device itself. */
@RunWith(AndroidJUnit4::class)
class LabelReadTest {
    private fun scan(asset: String): LabelScan {
        val bitmap = InstrumentationRegistry.getInstrumentation().context.assets.open(asset).use { BitmapFactory.decodeStream(it) }
        val reader = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val text = Tasks.await(reader.process(InputImage.fromBitmap(bitmap, 0)), 60, TimeUnit.SECONDS)
        reader.close()
        val lines = text.textBlocks.flatMap { it.lines }.mapNotNull { l -> l.boundingBox?.let { OcrLine(l.text, it.left, it.top, it.right, it.bottom) } }
        val rows = rowsFrom(lines)
        println("LABELROWS " + rows.joinToString(" | "))
        return parseLabel(rows)
    }

    @Test fun readsAUsPanel() {
        val v = scan("label_us.png").perServing
        assertNotNull(v)
        assertEquals(230.0, v!!.kcal!!, 0.5)
        assertEquals(8.0, v.fat!!, 0.5)
        assertEquals(37.0, v.carbs!!, 0.5)
        assertEquals(3.0, v.protein!!, 0.5)
        assertEquals(160.0, v.sodiumMg!!, 1.0)
    }

    @Test fun readsATwoColumnTable() {
        val s = scan("label_two_column.png")
        assertTrue(s.found)
        assertEquals(270.0, s.perServing!!.kcal!!, 0.5)
        assertEquals(450.0, s.per100!!.kcal!!, 0.5)
        assertEquals(6.0, s.perServing!!.protein!!, 0.5)
        assertEquals(60.0, s.servingGrams!!, 0.5)
    }
}
