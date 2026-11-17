package com.macrow.app

import com.macrow.app.data.BackupData
import com.macrow.app.data.BackupException
import com.macrow.app.data.BuiltInFoods
import com.macrow.app.data.Entry
import com.macrow.app.data.Food
import com.macrow.app.data.MacrowRepository
import com.macrow.app.data.Recipe
import com.macrow.app.data.RecipeItem
import com.macrow.app.data.Source
import com.macrow.app.data.Water
import com.macrow.app.data.Weight
import com.macrow.app.data.isBackupDue
import com.macrow.app.data.parseBackup
import com.macrow.app.data.parseProduct
import com.macrow.app.domain.Activity
import com.macrow.app.domain.Goal
import com.macrow.app.domain.Meal
import com.macrow.app.domain.Nutrients
import com.macrow.app.domain.Profile
import com.macrow.app.domain.Sex
import com.macrow.app.domain.Status
import com.macrow.app.domain.WeightPoint
import com.macrow.app.domain.bmr
import com.macrow.app.domain.loggingStreak
import com.macrow.app.domain.maintenanceKcal
import com.macrow.app.domain.pretty
import com.macrow.app.domain.servingsText
import com.macrow.app.domain.splitMacros
import com.macrow.app.domain.statusOf
import com.macrow.app.domain.suggestedKcal
import com.macrow.app.domain.targetsFor
import com.macrow.app.domain.weeklyChangeKg
import com.macrow.app.domain.weightTrend
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate

class LogicTest {
    private val man = Profile(Sex.MALE, 30, 175, 80.0, Activity.LIGHT, Goal.LOSE)

    @Test fun bmrMatchesMifflinStJeor() {
        // 10*80 + 6.25*175 - 5*30 + 5 = 1748.75
        assertEquals(1748.75, bmr(man), 0.001)
        assertEquals(1748.75 - 166, bmr(man.copy(sex = Sex.FEMALE)), 0.001)
    }

    @Test fun targetsFollowTheGoalAndNeverDropBelowTheFloor() {
        val maintenance = maintenanceKcal(man)
        assertEquals(2405, maintenance)
        assertEquals(maintenance - 450, suggestedKcal(man))
        assertEquals(maintenance, suggestedKcal(man.copy(goal = Goal.MAINTAIN)))
        val tiny = Profile(Sex.FEMALE, 80, 140, 40.0, Activity.SEDENTARY, Goal.LOSE)
        assertEquals(1200, suggestedKcal(tiny))
    }

    @Test fun macrosAddBackUpToTheCalorieTarget() {
        val t = targetsFor(man)
        val kcal = t.proteinG * 4 + t.carbsG * 4 + t.fatG * 9
        assertEquals(t.kcal.toDouble(), kcal.toDouble(), 15.0)
        assertEquals(144, t.proteinG) // 1.8 g per kg
        // Protein never takes more than 40 percent of a small target.
        assertTrue(splitMacros(1200, 150.0, Goal.GAIN).proteinG * 4 <= 1200 * 0.40 + 4)
    }

    @Test fun weeklyChangeIsAboutFourTenthsOfAKiloFor450Under() {
        assertEquals(-0.409, weeklyChangeKg(1900, 2350), 0.01)
    }

    @Test fun statusUsesAFivePercentBandAbove() {
        assertEquals(Status.ON, statusOf(1900.0, 1900.0))
        assertEquals(Status.ON, statusOf(1990.0, 1900.0))
        assertEquals(Status.OVER, statusOf(2000.0, 1900.0))
        assertEquals(Status.UNDER, statusOf(1600.0, 1900.0))
    }

    @Test fun nutrientsScaleAndAdd() {
        val a = Nutrients(kcal = 100.0, protein = 10.0) * 1.5 + Nutrients(kcal = 50.0, fat = 2.0)
        assertEquals(200.0, a.kcal, 0.0001)
        assertEquals(15.0, a.protein, 0.0001)
        assertEquals(2.0, a.fat, 0.0001)
    }

    @Test fun prettyPrintsAmountsReadably() {
        assertEquals("1", 1.0.pretty())
        assertEquals("1.5", 1.5.pretty())
        assertEquals("340", 340.2.pretty())
        assertEquals("0.25", 0.25.servingsText())
        assertEquals("1.5", 1.5.servingsText())
        assertEquals("2", 2.0.servingsText())
    }

    @Test fun mealGuessFollowsTheClock() {
        assertEquals(Meal.BREAKFAST, Meal.forHour(8))
        assertEquals(Meal.LUNCH, Meal.forHour(12))
        assertEquals(Meal.DINNER, Meal.forHour(19))
        assertEquals(Meal.SNACK, Meal.forHour(2))
    }

    @Test fun trendSmoothsAcrossTheWeek() {
        val pts = listOf(WeightPoint(1, 80.0), WeightPoint(2, 79.0), WeightPoint(3, 81.0), WeightPoint(20, 78.0))
        val t = weightTrend(pts)
        assertEquals(80.0, t[2].kg, 0.0001) // average of the first three
        assertEquals(78.0, t[3].kg, 0.0001) // the older ones are outside the 7 day window
    }

    @Test fun streakCountsBackAndTodayDoesNotBreakIt() {
        assertEquals(3, loggingStreak(setOf(8L, 9L, 10L), 10))
        assertEquals(3, loggingStreak(setOf(7L, 8L, 9L), 10)) // today still empty
        assertEquals(0, loggingStreak(setOf(5L), 10))
        assertEquals(1, loggingStreak(setOf(10L, 8L), 10))
    }

    @Test fun dailyBackupIsDueOncePerDay() {
        val today = LocalDate.of(2026, 11, 17)
        assertTrue(isBackupDue(today, null))
        assertTrue(isBackupDue(today, today.minusDays(1)))
        assertFalse(isBackupDue(today, today))
        assertTrue(isBackupDue(today, today.plusDays(3))) // clock was set back
    }

    @Test fun likeEscapeNeutralisesWildcards() {
        assertEquals("50\\% off\\_x\\\\y", MacrowRepository.likeEscape(" 50% off_x\\y "))
    }

    @Test fun builtInFoodsAreSane() {
        assertTrue(BuiltInFoods.size > 50)
        assertTrue(BuiltInFoods.all { it.source == Source.BUILTIN && it.name.isNotBlank() && it.kcal >= 0 && it.servingGrams > 0 })
        assertEquals(BuiltInFoods.size, BuiltInFoods.map { it.name }.toSet().size)
        // Calories roughly match the macros (4/4/9), within 25 percent, for foods with any calories.
        BuiltInFoods.filter { it.kcal >= 50 }.forEach {
            val est = it.protein * 4 + it.carbs * 4 + it.fat * 9
            assertTrue("${it.name}: ${it.kcal} vs $est", est in it.kcal * 0.75..it.kcal * 1.25)
        }
    }

    @Test fun openFoodFactsProductBecomesAPerServingFood() {
        val json = JSONObject(
            """{"code":"4800016644801","product_name":"Instant noodles","brands":"Acme, Other","serving_size":"60 g","serving_quantity":"60",
               "nutriments":{"energy-kcal_100g":450,"proteins_100g":10,"carbohydrates_100g":60,"fat_100g":18,"sodium_100g":1.5,"iron_100g":0.002}}""",
        )
        val f = parseProduct(json, null)!!
        assertEquals("Instant noodles", f.name)
        assertEquals("Acme", f.brand)
        assertEquals(60.0, f.servingGrams, 0.001)
        assertEquals(270.0, f.kcal, 0.001)
        assertEquals(900.0, f.sodiumMg, 0.001) // 1.5 g per 100 g -> 0.9 g -> 900 mg per 60 g
        assertEquals(1.2, f.ironMg, 0.001)
        assertEquals("4800016644801", f.barcode)
        assertEquals(Source.ONLINE, f.source)
    }

    @Test fun openFoodFactsWithoutServingUsesHundredGramsAndRejectsJunk() {
        val noServing = JSONObject("""{"product_name":"Rice","nutriments":{"energy_100g":1500}}""")
        val f = parseProduct(noServing, "12345678")!!
        assertEquals(100.0, f.servingGrams, 0.001)
        assertEquals("100 g", f.servingLabel)
        assertEquals(358.5, f.kcal, 0.5) // kJ converted to kcal
        assertNull(parseProduct(JSONObject("""{"product_name":"","nutriments":{"energy-kcal_100g":10}}"""), null))
        assertNull(parseProduct(JSONObject("""{"product_name":"No numbers"}"""), null))
        assertNull(parseProduct(JSONObject("""{"product_name":"No energy","nutriments":{"proteins_100g":3}}"""), null))
    }

    private fun sampleBackup(): BackupData {
        val foods = listOf(Food(id = 1, name = "Adobo", kcal = 330.0, source = Source.BUILTIN), Food(id = 1001, name = "My \"stew\"", kcal = 250.0, protein = 20.0, barcode = "12345678", source = Source.CUSTOM))
        return BackupData(
            foods, listOf(Entry(5, 20400, "LUNCH", 1001, 1.5, false, 99), Entry(6, 20400, "DINNER", 1, 1.0, true, 100)),
            listOf(Recipe(1001, 4)), listOf(RecipeItem(1, 1001, 1, 2.0)), listOf(Weight(20400, 78.4)), listOf(Water(1, 20400, 250)),
            Profile(Sex.FEMALE, 28, 160, 60.5, Activity.MODERATE, Goal.MAINTAIN), com.macrow.app.domain.Targets(1900, 120, 200, 60), 2500,
        )
    }

    @Test fun backupRoundTripsEverything() {
        val b = parseBackup(sampleBackup().toJson())
        assertEquals(2, b.foods.size)
        assertEquals("My \"stew\"", b.foods[1].name)
        assertEquals("12345678", b.foods[1].barcode)
        assertNull(b.foods[0].barcode)
        assertEquals(2, b.entries.size)
        assertTrue(b.entries[1].planned)
        assertEquals(1.5, b.entries[0].servings, 0.0001)
        assertEquals(4, b.recipes[0].servings)
        assertEquals(78.4, b.weights[0].kg, 0.0001)
        assertEquals(60.5, b.profile!!.weightKg, 0.0001)
        assertEquals(Sex.FEMALE, b.profile.sex)
        assertEquals(1900, b.targets!!.kcal)
        assertEquals(2500, b.waterGoalMl)
        assertFalse(b.isEmpty())
    }

    @Test fun backupDropsEntriesThatPointAtMissingFoods() {
        val data = sampleBackup().copy(entries = sampleBackup().entries + Entry(7, 20400, "SNACK", 9999, 1.0))
        assertEquals(2, parseBackup(data.toJson()).entries.size)
    }

    @Test fun badBackupsAreRefusedWithAPlainMessage() {
        fun refused(text: String) = try { parseBackup(text); fail("should refuse: $text") } catch (e: BackupException) { assertTrue(e.message!!.isNotBlank()) }
        refused("not json")
        refused("""{"app":"Pantri","format":1}""")
        refused("""{"app":"Macrow","format":99}""")
        refused(sampleBackup().toJson().replace("\"kcal\":330", "\"kcal\":-5"))
        assertNotNull(parseBackup("""{"app":"Macrow","format":1}"""))
    }

    @Test fun emptyBackupIsDetected() {
        assertTrue(BackupData(BuiltInFoods, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), null, null, null).isEmpty())
    }
}
