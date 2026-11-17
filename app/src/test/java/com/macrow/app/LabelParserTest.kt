package com.macrow.app

import com.macrow.app.domain.LabelBasis
import com.macrow.app.domain.OcrLine
import com.macrow.app.domain.parseLabel
import com.macrow.app.domain.rowsFrom
import com.macrow.app.domain.toPrefill
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LabelParserTest {
    private val usPanel = listOf(
        "Nutrition Facts", "Servings per container 8", "Serving size 2/3 cup (55g)", "Amount per serving",
        "Calories 230", "% Daily Value*", "Total Fat 8g 10%", "Saturated Fat 1g 5%", "Trans Fat 0g", "Cholesterol 0mg 0%",
        "Sodium 160mg 7%", "Total Carbohydrate 37g 13%", "Dietary Fiber 4g 14%", "Total Sugars 12g", "Includes 10g Added Sugars 20%",
        "Protein 3g", "Vitamin D 2mcg 10%", "Calcium 260mg 20%", "Iron 8mg 45%", "Potassium 240mg 5%",
    )

    @Test fun readsAUsNutritionFactsPanel() {
        val scan = parseLabel(usPanel)
        val v = scan.perServing!!
        assertEquals(230.0, v.kcal!!, 0.01)
        assertEquals(8.0, v.fat!!, 0.01) // not the saturated fat
        assertEquals(37.0, v.carbs!!, 0.01)
        assertEquals(4.0, v.fiber!!, 0.01)
        assertEquals(12.0, v.sugar!!, 0.01) // not the added sugars
        assertEquals(3.0, v.protein!!, 0.01)
        assertEquals(160.0, v.sodiumMg!!, 0.01)
        assertEquals(260.0, v.calciumMg!!, 0.01)
        assertEquals(8.0, v.ironMg!!, 0.01)
        assertEquals(240.0, v.potassiumMg!!, 0.01)
        assertEquals(55.0, scan.servingGrams!!, 0.01)
        assertEquals("2/3 cup", scan.servingLabel)
        assertNull(scan.per100)
        assertTrue(scan.found)
        assertFalse(scan.basisGuessed)
    }

    @Test fun ignoresCaloriesFromFatOnTheSameRow() {
        val v = parseLabel(listOf("Nutrition Facts", "Calories 250 Calories from Fat 110", "Total Fat 12g 18%")).perServing!!
        assertEquals(250.0, v.kcal!!, 0.01)
    }

    @Test fun readsATwoColumnTableInTheOrderTheHeaderSays() {
        val scan = parseLabel(
            listOf(
                "NUTRITION INFORMATION", "Serving size: 1 pack (60 g)", "Per 100 g Per serving", "Energy 450 kcal 270 kcal",
                "Protein 10 g 6 g", "Carbohydrates 60 g 36 g", "Fat 18 g 10.8 g", "Sugars 8 g 4.8 g", "Sodium 1.5 g 0.9 g",
            ),
        )
        assertEquals(450.0, scan.per100!!.kcal!!, 0.01)
        assertEquals(270.0, scan.perServing!!.kcal!!, 0.01)
        assertEquals(6.0, scan.perServing!!.protein!!, 0.01)
        assertEquals(10.8, scan.perServing!!.fat!!, 0.01)
        assertEquals(900.0, scan.perServing!!.sodiumMg!!, 0.01) // grams turned into milligrams
        assertEquals(1500.0, scan.per100!!.sodiumMg!!, 0.01)
        assertEquals(60.0, scan.servingGrams!!, 0.01)
        assertEquals("1 pack", scan.servingLabel)
    }

    @Test fun servingColumnFirstIsReadInThatOrder() {
        val scan = parseLabel(listOf("Per serving Per 100 g", "Energy 90 kcal 300 kcal", "Protein 3 g 10 g", "Carbohydrate 15 g 50 g", "Fat 2 g 6.7 g"))
        assertEquals(90.0, scan.perServing!!.kcal!!, 0.01)
        assertEquals(300.0, scan.per100!!.kcal!!, 0.01)
    }

    @Test fun headersReadOnSeparateRowsStillGiveTwoColumns() {
        val scan = parseLabel(listOf("Per 100g", "Per serving (30g)", "Energy 400 kcal 120 kcal", "Protein 8 g 2.4 g", "Fat 20 g 6 g", "Carbohydrate 50 g 15 g"))
        assertEquals(400.0, scan.per100!!.kcal!!, 0.01)
        assertEquals(120.0, scan.perServing!!.kcal!!, 0.01)
    }

    @Test fun onlyAPer100ColumnIsReadAsPer100() {
        val scan = parseLabel(listOf("Nutrition Information", "Per 100 g", "Energy 520 kJ / 124 kcal", "Protein 5 g", "Carbohydrate 20 g", "Fat 3 g"))
        assertNull(scan.perServing)
        assertEquals(124.0, scan.per100!!.kcal!!, 0.01)
    }

    @Test fun kilojoulesAloneAreConvertedToCalories() {
        val v = parseLabel(listOf("Nutrition Facts", "Energy 1046 kJ", "Protein 5 g", "Fat 10 g", "Carbohydrate 30 g")).perServing!!
        assertEquals(250.0, v.kcal!!, 1.0)
    }

    @Test fun saltIsTurnedIntoSodium() {
        val v = parseLabel(listOf("Nutrition Facts", "Calories 100", "Protein 2 g", "Salt 1.5 g")).perServing!!
        assertEquals(600.0, v.sodiumMg!!, 0.01)
    }

    @Test fun commaDecimalsAndLetterOAreRepaired() {
        val v = parseLabel(listOf("Nutrition Facts", "Calories 120", "Total Fat 2,5 g", "Protein 1O g", "Total Carbohydrate 2O g")).perServing!!
        assertEquals(2.5, v.fat!!, 0.01)
        assertEquals(10.0, v.protein!!, 0.01)
        assertEquals(20.0, v.carbs!!, 0.01)
    }

    @Test fun anUnclearLabelIsFlaggedAsGuessed() {
        val scan = parseLabel(listOf("Calories 100", "Protein 2 g", "Total Fat 1 g", "Carbohydrate 20 g"))
        assertTrue(scan.basisGuessed)
        assertTrue(scan.found)
    }

    @Test fun textThatIsNotALabelFindsNothing() {
        val scan = parseLabel(listOf("Buy one get one free", "Open daily 8 to 5", "Best before 12/2027"))
        assertFalse(scan.found)
        assertNull(scan.perServing)
    }

    @Test fun neverInventsNumbers() {
        val scan = parseLabel(listOf("Nutrition Facts", "Calories 200", "Protein 4 g"))
        assertNull(scan.perServing!!.fat)
        assertNull(scan.perServing!!.sodiumMg)
    }

    @Test fun prefillUsesTheChosenColumn() {
        val scan = parseLabel(listOf("Serving size: 1 pack (60 g)", "Per 100 g Per serving", "Energy 450 kcal 270 kcal", "Protein 10 g 6 g", "Fat 18 g 10.8 g", "Carbohydrates 60 g 36 g"))
        val perServing = scan.toPrefill(LabelBasis.PER_SERVING)!!
        assertEquals("1 pack", perServing.servingLabel)
        assertEquals(60.0, perServing.servingGrams, 0.01)
        assertEquals(270.0, perServing.values.kcal!!, 0.01)
        val per100 = scan.toPrefill(LabelBasis.PER_100)!!
        assertEquals("100 g", per100.servingLabel)
        assertEquals(450.0, per100.values.kcal!!, 0.01)
    }

    @Test fun rowsAreBuiltFromLinePositions() {
        val lines = listOf(
            OcrLine("Protein", 10, 100, 120, 130), OcrLine("6 g", 300, 102, 340, 132), OcrLine("10 g", 200, 99, 250, 129),
            OcrLine("Fat", 10, 160, 60, 190), OcrLine("10.8 g", 300, 161, 360, 191),
        )
        val rows = rowsFrom(lines)
        assertEquals(listOf("Protein 10 g 6 g", "Fat 10.8 g"), rows)
        assertNotNull(parseLabel(rows))
    }

    @Test fun aMisreadUnitInATwoColumnRowIsRepairedFromTheServingSize() {
        val scan = parseLabel(
            listOf("Serving size: 1 pack (60 g)", "Per 100 g Per serving", "Energy 450 kcal 270 kcal", "Protein 109 6g", "Carbohydrates 60 g 36 g", "Fat 18 g 10.8 g"),
        )
        assertEquals(10.0, scan.per100!!.protein!!, 0.01)
        assertEquals(6.0, scan.perServing!!.protein!!, 0.01)
    }
}
