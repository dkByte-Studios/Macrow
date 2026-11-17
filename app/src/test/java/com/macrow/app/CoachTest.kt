package com.macrow.app

import com.macrow.app.data.Food
import com.macrow.app.domain.Activity
import com.macrow.app.domain.CoachInput
import com.macrow.app.domain.Eaten
import com.macrow.app.domain.Goal
import com.macrow.app.domain.Meal
import com.macrow.app.domain.Mood
import com.macrow.app.domain.Nutrients
import com.macrow.app.domain.Sex
import com.macrow.app.domain.Targets
import com.macrow.app.domain.coach
import com.macrow.app.domain.macroSplit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachTest {
    private val targets = Targets(kcal = 2000, proteinG = 140, carbsG = 220, fatG = 60)
    private val pool = listOf(
        Food(id = 1, name = "Chicken breast", servingLabel = "100 g", kcal = 165.0, protein = 31.0, fat = 3.6),
        Food(id = 2, name = "Greek yogurt", servingLabel = "170 g", kcal = 100.0, protein = 17.0, carbs = 6.0, sugar = 6.0),
        Food(id = 3, name = "Salty ham", servingLabel = "2 slices", kcal = 70.0, protein = 12.0, sodiumMg = 1100.0),
        Food(id = 4, name = "Candy", servingLabel = "1 bar", kcal = 200.0, protein = 15.0, carbs = 30.0, sugar = 28.0),
        Food(id = 5, name = "Lentils", servingLabel = "1 cup", kcal = 230.0, protein = 18.0, carbs = 40.0, fiber = 15.6),
    )

    private fun eat(meal: Meal, name: String, kcal: Double, p: Double = 0.0, c: Double = 0.0, f: Double = 0.0, fiber: Double = 0.0, na: Double = 0.0) =
        Eaten(meal, name, Nutrients(kcal = kcal, protein = p, carbs = c, fat = f, fiber = fiber, sodiumMg = na))

    private fun input(eaten: List<Eaten>, isToday: Boolean = false, hour: Int = 22, water: Int = 2500, goal: Goal = Goal.LOSE) =
        CoachInput(eaten, targets, goal, Sex.MALE, water, 2500, isToday, hour, streak = 0, pool = pool)

    @Test fun emptyMorningIsAFreshStart() {
        val r = coach(input(emptyList(), isToday = true, hour = 8))
        assertTrue(r.empty)
        assertEquals("A fresh start", r.headline)
        assertTrue(r.suggestions.isEmpty())
    }

    @Test fun pastDayWithNothingSaysSo() {
        val r = coach(input(emptyList(), isToday = false))
        assertTrue(r.empty)
        assertTrue(r.insights.isEmpty())
    }

    @Test fun onTargetDayIsGoodAndNamesTheBiggestFood() {
        val eaten = listOf(
            eat(Meal.BREAKFAST, "Oatmeal", 400.0, 15.0, 70.0, 8.0, fiber = 10.0),
            eat(Meal.LUNCH, "Chicken adobo", 700.0, 70.0, 60.0, 25.0, fiber = 10.0),
            eat(Meal.DINNER, "Salmon", 600.0, 50.0, 50.0, 20.0, fiber = 8.0),
            eat(Meal.SNACK, "Yogurt", 300.0, 20.0, 30.0, 8.0),
        )
        val r = coach(input(eaten))
        assertEquals(Mood.GOOD, r.mood)
        assertTrue(r.insights.any { it.title == "Right on target" && it.mood == Mood.GOOD })
        assertTrue(r.insights.any { it.title == "Protein goal reached" })
        assertTrue(r.summary.contains("Chicken adobo"))
        assertTrue(r.summary.contains("2,000"))
    }

    @Test fun bigOvershootOnAFinishedDayIsAWarning() {
        val r = coach(input(listOf(eat(Meal.DINNER, "Feast", 2800.0, 100.0, 300.0, 120.0))))
        val cal = r.insights.first { it.title == "Calories ran over" }
        assertEquals(Mood.WATCH, cal.mood)
        assertTrue(cal.body.contains("800"))
        assertTrue(cal.body.contains("Dinner"))
    }

    @Test fun overshootIsFineWhenBuildingMuscle() {
        val r = coach(input(listOf(eat(Meal.DINNER, "Feast", 2200.0, 150.0, 250.0, 80.0)), goal = Goal.GAIN))
        assertEquals(Mood.GOOD, r.insights.first { it.title == "Calories" }.mood)
    }

    @Test fun lowProteinSuggestsGoodFoodsAndSkipsSaltyOrSugaryOnes() {
        val eaten = listOf(eat(Meal.LUNCH, "Rice and veg", 1300.0, 30.0, 250.0, 15.0))
        val r = coach(input(eaten))
        assertTrue(r.insights.any { it.title == "Protein is low" && it.mood == Mood.WATCH })
        val names = r.suggestions.map { it.food.name }
        assertTrue("Chicken breast" in names)
        assertFalse("Salty ham" in names)
        assertFalse("Candy" in names)
        assertEquals("Chicken breast", names.first()) // best protein for the calories
        assertTrue(r.suggestions.first().reason.contains("31 g protein"))
    }

    @Test fun suggestionsNeverRepeatAFoodYouAlreadyAte() {
        val eaten = listOf(eat(Meal.LUNCH, "Chicken breast", 900.0, 30.0, 150.0, 10.0))
        val r = coach(input(eaten))
        assertFalse(r.suggestions.any { it.food.name == "Chicken breast" })
    }

    @Test fun noSuggestionsWhenThereIsNoCalorieRoomLeft() {
        val eaten = listOf(eat(Meal.DINNER, "Big plate", 1980.0, 40.0, 300.0, 60.0))
        assertTrue(coach(input(eaten)).suggestions.isEmpty())
    }

    @Test fun aheadOfPaceInTheMorningIsFlagged() {
        val eaten = listOf(eat(Meal.BREAKFAST, "Huge breakfast", 1300.0, 40.0, 150.0, 50.0))
        val r = coach(input(eaten, isToday = true, hour = 10))
        assertTrue(r.insights.any { it.title == "Ahead of pace" && it.mood == Mood.WATCH })
        assertTrue(r.summary.startsWith("So far"))
    }

    @Test fun carbHeavyAndSaltyDaysGetNotes() {
        val eaten = listOf(eat(Meal.LUNCH, "Noodles", 1900.0, 40.0, 380.0, 20.0, na = 3500.0))
        val r = coach(input(eaten))
        assertTrue(r.insights.any { it.title == "Carb-heavy day" })
        val na = r.insights.first { it.title == "Sodium is high" }
        assertTrue(na.body.contains("Noodles"))
    }

    @Test fun skippingWaterAndBreakfastAreMentioned() {
        val eaten = listOf(eat(Meal.LUNCH, "Lunch", 700.0, 50.0, 60.0, 20.0))
        val r = coach(input(eaten, isToday = true, hour = 19, water = 400))
        assertTrue(r.insights.any { it.title == "No breakfast logged" })
        assertTrue(r.insights.any { it.title == "Drink more water" })
    }

    @Test fun insightsAreSortedWithWarningsFirst() {
        val eaten = listOf(eat(Meal.DINNER, "Feast", 2800.0, 140.0, 300.0, 120.0))
        val moods = coach(input(eaten)).insights.map { it.mood.ordinal }
        assertEquals(moods.sortedDescending(), moods)
    }

    @Test fun macroSplitAddsUpToAboutAHundred() {
        val s = macroSplit(Nutrients(protein = 100.0, carbs = 250.0, fat = 70.0))!!
        assertEquals(100, s.protein + s.carbs + s.fat)
        assertEquals(null, macroSplit(Nutrients.ZERO))
        assertNotNull(coach(input(listOf(eat(Meal.LUNCH, "x", 500.0, 20.0, 50.0, 10.0)))).split)
    }
}
