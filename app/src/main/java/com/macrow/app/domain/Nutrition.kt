package com.macrow.app.domain

import kotlin.math.roundToInt

/** Every tracked nutrient in one place. Grams for macros, milligrams for minerals and vitamin C. */
data class Nutrients(
    val kcal: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0,
    val sugar: Double = 0.0,
    val sodiumMg: Double = 0.0,
    val ironMg: Double = 0.0,
    val calciumMg: Double = 0.0,
    val vitCMg: Double = 0.0,
    val potassiumMg: Double = 0.0,
) {
    operator fun plus(o: Nutrients) = Nutrients(
        kcal + o.kcal, protein + o.protein, carbs + o.carbs, fat + o.fat, fiber + o.fiber, sugar + o.sugar,
        sodiumMg + o.sodiumMg, ironMg + o.ironMg, calciumMg + o.calciumMg, vitCMg + o.vitCMg, potassiumMg + o.potassiumMg,
    )

    operator fun times(f: Double) = Nutrients(
        kcal * f, protein * f, carbs * f, fat * f, fiber * f, sugar * f,
        sodiumMg * f, ironMg * f, calciumMg * f, vitCMg * f, potassiumMg * f,
    )

    companion object {
        val ZERO = Nutrients()
    }
}

enum class Meal(val label: String) {
    BREAKFAST("Breakfast"), LUNCH("Lunch"), DINNER("Dinner"), SNACK("Snacks");

    companion object {
        fun of(name: String): Meal = entries.firstOrNull { it.name == name } ?: SNACK

        /** The meal you are most likely logging at this hour. */
        fun forHour(hour: Int): Meal = when (hour) {
            in 4..10 -> BREAKFAST
            in 11..14 -> LUNCH
            in 15..16 -> SNACK
            in 17..21 -> DINNER
            else -> SNACK
        }
    }
}

/** One decimal for small amounts, none for big ones: 1.5, 12, 340. */
fun Double.pretty(): String {
    val r = if (this >= 100) roundToInt().toDouble() else (this * 10).roundToInt() / 10.0
    return if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString()
}

/** Servings text such as "1", "1.5" or "0.25". Quarters are kept exact. */
fun Double.servingsText(): String {
    val q = (this * 100).roundToInt() / 100.0
    return if (q == q.toLong().toDouble()) q.toLong().toString() else q.toString().trimEnd('0')
}

fun Double.kcalText(): String = "%,d".format(roundToInt())

/** How a day's total sits next to its target. */
enum class Status { UNDER, ON, OVER }

fun statusOf(total: Double, target: Double): Status = when {
    target <= 0 -> Status.ON
    total < target * 0.9 -> Status.UNDER
    total > target * 1.05 -> Status.OVER
    else -> Status.ON
}

/** Rough daily needs for the micronutrients. Not medical advice; the app says so in Settings. */
data class MicroGoals(val fiber: Double, val sugarMax: Double, val sodiumMax: Double, val iron: Double, val calcium: Double, val vitC: Double, val potassium: Double) {
    companion object {
        fun forSex(sex: Sex) = if (sex == Sex.MALE) {
            MicroGoals(30.0, 50.0, 2300.0, 8.0, 1000.0, 90.0, 3400.0)
        } else {
            MicroGoals(25.0, 50.0, 2300.0, 18.0, 1000.0, 75.0, 2600.0)
        }
    }
}
