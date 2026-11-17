package com.macrow.app.domain

import kotlin.math.roundToInt

enum class Sex(val label: String) { MALE("Male"), FEMALE("Female") }

enum class Activity(val label: String, val hint: String, val factor: Double) {
    SEDENTARY("Mostly sitting", "Desk job, little exercise", 1.2),
    LIGHT("Light activity", "Walks or exercise 1 to 3 days a week", 1.375),
    MODERATE("Active", "Exercise 3 to 5 days a week", 1.55),
    VERY("Very active", "Hard exercise most days or a physical job", 1.725),
}

enum class Goal(val label: String, val hint: String, val delta: Int) {
    LOSE("Lose fat", "About 0.4 kg a week", -450),
    MAINTAIN("Stay the same", "Keep your weight steady", 0),
    GAIN("Build muscle", "A small, steady surplus", 300),
}

data class Profile(
    val sex: Sex = Sex.MALE,
    val age: Int = 25,
    val heightCm: Int = 170,
    val weightKg: Double = 70.0,
    val activity: Activity = Activity.LIGHT,
    val goal: Goal = Goal.LOSE,
)

data class Targets(val kcal: Int, val proteinG: Int, val carbsG: Int, val fatG: Int)

object Limits {
    const val NAME = 60
    const val MAX_KCAL_PER_SERVING = 5000.0
    const val MAX_GRAMS = 5000.0
    const val MAX_SERVINGS = 99.0
    const val MIN_KCAL_TARGET = 1200
    const val MAX_KCAL_TARGET = 6000
    const val WATER_STEP_ML = 250
}

/** Mifflin-St Jeor resting energy, in kcal a day. */
fun bmr(p: Profile): Double {
    val base = 10 * p.weightKg + 6.25 * p.heightCm - 5 * p.age
    return if (p.sex == Sex.MALE) base + 5 else base - 161
}

/** What you burn in a normal day at your activity level. */
fun maintenanceKcal(p: Profile): Int = (bmr(p) * p.activity.factor).roundToInt()

/** The calorie target for the goal, never below a safe floor. */
fun suggestedKcal(p: Profile): Int = (maintenanceKcal(p) + p.goal.delta).coerceIn(Limits.MIN_KCAL_TARGET, Limits.MAX_KCAL_TARGET)

/** Splits [kcal] into macros: protein by body weight, fat 25 percent of calories, carbs fill the rest. */
fun splitMacros(kcal: Int, weightKg: Double, goal: Goal): Targets {
    val perKg = if (goal == Goal.MAINTAIN) 1.6 else 1.8
    val protein = minOf(weightKg * perKg, kcal * 0.40 / 4).roundToInt().coerceAtLeast(0)
    val fat = (kcal * 0.25 / 9).roundToInt()
    val carbs = ((kcal - protein * 4 - fat * 9) / 4.0).roundToInt().coerceAtLeast(0)
    return Targets(kcal, protein, carbs, fat)
}

fun targetsFor(p: Profile): Targets = splitMacros(suggestedKcal(p), p.weightKg, p.goal)

/** Expected weight change in kg per week at [kcal] a day. 7,700 kcal is about 1 kg of body weight. */
fun weeklyChangeKg(kcal: Int, maintenance: Int): Double = (kcal - maintenance) * 7 / 7700.0
