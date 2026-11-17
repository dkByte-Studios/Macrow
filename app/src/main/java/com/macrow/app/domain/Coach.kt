package com.macrow.app.domain

import com.macrow.app.data.Food
import kotlin.math.abs
import kotlin.math.roundToInt

/** How an observation reads: fine, worth a look, or worth acting on. */
enum class Mood { GOOD, OKAY, WATCH }

data class Insight(val title: String, val body: String, val mood: Mood)

/** A food that would help close today's biggest gap, and why. */
data class Suggestion(val food: Food, val reason: String)

/** Share of calories from each macro, in whole percent. */
data class MacroSplit(val protein: Int, val carbs: Int, val fat: Int)

/** One logged portion: which meal, the food's name, and what that portion adds up to. */
data class Eaten(val meal: Meal, val name: String, val n: Nutrients)

data class CoachInput(
    val eaten: List<Eaten>,
    val targets: Targets,
    val goal: Goal,
    val sex: Sex,
    val waterMl: Int,
    val waterGoalMl: Int,
    val isToday: Boolean,
    /** Hour of the day, 0 to 23. Ignored for a past day, which is judged as finished. */
    val hour: Int,
    val streak: Int,
    /** Foods suggestions may come from. */
    val pool: List<Food>,
)

data class CoachReview(
    val headline: String,
    val mood: Mood,
    /** One short paragraph: what you ate and how much. */
    val summary: String,
    val split: MacroSplit?,
    val targetSplit: MacroSplit,
    val insights: List<Insight>,
    val suggestions: List<Suggestion>,
    val empty: Boolean,
)

fun macroSplit(n: Nutrients): MacroSplit? {
    val total = n.protein * 4 + n.carbs * 4 + n.fat * 9
    if (total <= 0) return null
    val p = (n.protein * 4 / total * 100).roundToInt()
    val f = (n.fat * 9 / total * 100).roundToInt()
    return MacroSplit(p, (100 - p - f).coerceAtLeast(0), f)
}

/** How much of the day's calories you would normally have eaten by this hour. */
internal fun expectedShare(hour: Int): Double = when {
    hour < 6 -> 0.0
    hour < 9 -> 0.12
    hour < 12 -> 0.30
    hour < 15 -> 0.55
    hour < 18 -> 0.72
    hour < 21 -> 0.92
    else -> 1.0
}

private fun Double.g() = roundToInt().toString()
private fun Int.n() = "%,d".format(this)

/**
 * Reads one day of eating against your targets and says, in plain words, what went well and what to adjust.
 * Everything here is simple rules over your own numbers: nothing is sent anywhere, and it is not medical advice.
 */
fun coach(i: CoachInput): CoachReview {
    val t = i.targets
    val total = i.eaten.fold(Nutrients.ZERO) { a, e -> a + e.n }
    val targetSplit = macroSplit(Nutrients(protein = t.proteinG.toDouble(), carbs = t.carbsG.toDouble(), fat = t.fatG.toDouble())) ?: MacroSplit(30, 45, 25)
    if (i.eaten.isEmpty()) {
        val early = i.isToday && i.hour < 11
        return CoachReview(
            headline = if (early) "A fresh start" else "Nothing logged yet",
            mood = Mood.OKAY,
            summary = if (i.isToday) "Add what you eat and the coach reads your day: calories, protein, balance and the vitamins you are missing." else "Nothing was logged on this day.",
            split = null, targetSplit = targetSplit, suggestions = emptyList(), empty = true,
            insights = if (i.isToday) listOf(
                Insight(if (early) "Start with breakfast" else "Log your meals", if (early) "A protein-rich breakfast, like eggs or Greek yogurt, makes the rest of the day easier." else "Even a rough log is useful. Tap the plus next to a meal to add what you ate.", Mood.OKAY),
            ) else emptyList(),
        )
    }

    val done = !i.isToday || i.hour >= 20
    val share = if (i.isToday) expectedShare(i.hour) else 1.0
    val kcal = total.kcal
    val ratio = if (t.kcal > 0) kcal / t.kcal else 0.0
    val left = t.kcal - kcal
    val protRatio = if (t.proteinG > 0) total.protein / t.proteinG else 1.0
    val goals = MicroGoals.forSex(i.sex)
    val insights = mutableListOf<Insight>()

    val byMeal = i.eaten.groupBy { it.meal }.mapValues { (_, v) -> v.sumOf { it.n.kcal } }
    val biggestMeal = byMeal.maxByOrNull { it.value }
    val biggestFood = i.eaten.groupBy { it.name }.mapValues { (_, v) -> v.sumOf { it.n.kcal } }.maxByOrNull { it.value }

    // Calories
    insights += when {
        done && ratio > 1.05 -> when (i.goal) {
            Goal.GAIN -> Insight("Calories", "${kcal.kcalText()} kcal, ${abs(left).roundToInt().n()} over your goal. That fits a day of building, as long as protein is covered.", if (ratio <= 1.2) Mood.GOOD else Mood.OKAY)
            else -> Insight("Calories ran over", "You are ${abs(left).roundToInt().n()} kcal over ${t.kcal.n()}.${biggestMeal?.let { " ${it.key.label} was the biggest meal at ${it.value.kcalText()} kcal." } ?: ""} One day will not undo your week.", if (ratio > 1.2) Mood.WATCH else Mood.OKAY)
        }
        done && ratio >= 0.9 -> Insight("Right on target", "${kcal.kcalText()} of ${t.kcal.n()} kcal. That is a well-judged day.", Mood.GOOD)
        done && ratio >= 0.8 -> Insight("A little under", "${kcal.kcalText()} of ${t.kcal.n()} kcal. Close enough, but there was room for a small snack.", Mood.OKAY)
        done -> Insight("Quite low on calories", "Only ${kcal.kcalText()} of ${t.kcal.n()} kcal. Eating much less than your target can leave you tired and hungry later, and can cost muscle. Add a proper snack next time.", Mood.WATCH)
        ratio > share + 0.25 -> Insight("Ahead of pace", "You have used ${(ratio * 100).roundToInt()}% of your calories and the day is not over. Lighter meals later will keep you inside ${t.kcal.n()}.", Mood.WATCH)
        ratio < share - 0.25 && i.hour >= 12 -> Insight("Plenty of room left", "${left.roundToInt().n()} kcal still to use. Eat a real meal rather than waiting until you are starving.", Mood.OKAY)
        else -> Insight("On pace", "${left.roundToInt().n()} kcal left for the rest of the day.", Mood.GOOD)
    }

    // Protein
    insights += when {
        done && protRatio >= 0.95 -> Insight("Protein goal reached", "${total.protein.g()} g of ${t.proteinG} g. That helps you stay full and keep muscle${if (i.goal == Goal.LOSE) " while you lose fat" else ""}.", Mood.GOOD)
        done && protRatio >= 0.75 -> Insight("Protein is close", "${total.protein.g()} of ${t.proteinG} g. One more serving of chicken, fish, eggs or yogurt would get you there.", Mood.OKAY)
        done -> Insight("Protein is low", "${total.protein.g()} of ${t.proteinG} g. Protein keeps you full and protects muscle, especially when you are eating less. Build the next meal around it.", Mood.WATCH)
        i.hour >= 15 && protRatio < share * 0.7 -> Insight("Behind on protein", "${total.protein.g()} of ${t.proteinG} g so far. Make dinner protein-first.", Mood.WATCH)
        protRatio >= share -> Insight("Protein is on track", "${total.protein.g()} of ${t.proteinG} g so far.", Mood.GOOD)
        else -> Insight("Protein so far", "${total.protein.g()} of ${t.proteinG} g. Add some at your next meal.", Mood.OKAY)
    }

    // Balance of macros
    val split = macroSplit(total)
    if (split != null && kcal >= 600) {
        when {
            split.carbs > targetSplit.carbs + 12 -> insights += Insight("Carb-heavy day", "${split.carbs}% of your calories came from carbs; your plan is about ${targetSplit.carbs}%. Swap a starchy side for vegetables or protein.", Mood.OKAY)
            split.fat > targetSplit.fat + 10 -> insights += Insight("Fat ran high", "${split.fat}% of your calories came from fat; your plan is about ${targetSplit.fat}%. Fried foods, oils and cheese add up fast.", Mood.OKAY)
            kotlin.math.abs(split.protein - targetSplit.protein) <= 5 && abs(split.carbs - targetSplit.carbs) <= 8 -> insights += Insight("Balanced macros", "Your protein, carbs and fat sit close to your plan.", Mood.GOOD)
        }
    }

    // How the day was spread out
    if (biggestMeal != null && kcal >= 800 && (done || i.hour >= 15) && biggestMeal.value / kcal > 0.55) {
        insights += Insight("Most of it came at ${biggestMeal.key.label.lowercase()}", "${(biggestMeal.value / kcal * 100).roundToInt()}% of your calories were in one meal. Spreading them out keeps hunger steadier.", Mood.OKAY)
    }
    if (Meal.BREAKFAST !in byMeal && i.hour >= 11 && kcal > 0) {
        insights += Insight("No breakfast logged", "If you skipped it on purpose, that is fine. If you forgot, add it so your totals are right.", Mood.OKAY)
    }

    // Micronutrients and limits (judged once the day is mostly done)
    if (done) {
        when {
            total.fiber >= goals.fiber -> insights += Insight("Fiber goal reached", "${total.fiber.g()} g of ${goals.fiber.g()} g. Good for digestion and fullness.", Mood.GOOD)
            total.fiber < goals.fiber * 0.6 -> insights += Insight("Fiber is low", "${total.fiber.g()} of ${goals.fiber.g()} g. Vegetables, beans, oats and fruit with the skin on will lift it.", Mood.OKAY)
        }
        if (total.sodiumMg > goals.sodiumMax) {
            val top = i.eaten.maxByOrNull { it.n.sodiumMg }
            insights += Insight("Sodium is high", "${total.sodiumMg.g()} mg against a limit of ${goals.sodiumMax.g()} mg.${top?.let { " ${it.name} added the most." } ?: ""} Instant, canned and cured foods are the usual culprits.", Mood.WATCH)
        }
        if (total.sugar > goals.sugarMax) {
            val top = i.eaten.maxByOrNull { it.n.sugar }
            insights += Insight("Sugar is high", "${total.sugar.g()} g of sugar.${top?.let { " ${it.name} had the most." } ?: ""} Sweet drinks are the easiest place to cut.", Mood.OKAY)
        }
    }

    // Water
    if (i.waterGoalMl > 0 && (done || i.hour >= 17)) {
        if (i.waterMl >= i.waterGoalMl) insights += Insight("Water goal reached", "${i.waterMl.n()} ml. Well done.", Mood.GOOD)
        else if (i.waterMl < i.waterGoalMl * 0.5) insights += Insight("Drink more water", "${i.waterMl.n()} of ${i.waterGoalMl.n()} ml. Water also helps with hunger that is really thirst.", Mood.OKAY)
    }

    // Variety
    if (done && i.eaten.map { it.name }.distinct().size <= 2) {
        insights += Insight("Very few foods", "Only ${i.eaten.map { it.name }.distinct().size} different foods today. More variety covers more vitamins and minerals.", Mood.OKAY)
    }
    if (i.isToday && i.streak >= 3) insights += Insight("${i.streak}-day streak", "You have logged ${i.streak} days in a row. Logging is the habit that makes everything else work.", Mood.GOOD)

    val sorted = insights.sortedBy { it.mood.ordinal.let { o -> 2 - o } }.take(6)

    // Suggestions for the biggest gap
    val remaining = t.kcal - kcal
    val have = i.eaten.map { it.name }.toSet()
    val gap = when {
        protRatio < 0.9 && (done || i.hour >= 12) -> "protein"
        done && total.fiber < goals.fiber * 0.6 -> "fiber"
        else -> null
    }
    val suggestions = if (gap == null || remaining < 80) emptyList() else {
        val room = maxOf(remaining, 150.0).coerceAtMost(500.0)
        i.pool.asSequence()
            .filter { it.name !in have && !it.isRecipe && it.kcal in 60.0..room && it.sodiumMg < 900 && it.sugar < 20 }
            .filter { if (gap == "protein") it.protein >= 12 else it.fiber >= 3 }
            .sortedByDescending { if (gap == "protein") it.protein / it.kcal else it.fiber / it.kcal }
            .take(3)
            .map { f ->
                Suggestion(f, if (gap == "protein") "+${f.protein.g()} g protein, ${f.kcal.g()} kcal" else "+${f.fiber.g()} g fiber, ${f.kcal.g()} kcal")
            }.toList()
    }

    val watches = sorted.count { it.mood == Mood.WATCH }
    val goods = sorted.count { it.mood == Mood.GOOD }
    val mood = when {
        watches >= 2 -> Mood.WATCH
        watches == 1 -> Mood.OKAY
        goods >= 2 -> Mood.GOOD
        else -> Mood.OKAY
    }
    val headline = when {
        mood == Mood.GOOD -> if (done) "A solid day" else "Looking good so far"
        mood == Mood.WATCH -> "A few things to adjust"
        watches == 1 -> sorted.first { it.mood == Mood.WATCH }.title.let { if (done) "Mostly good. Watch this: ${it.lowercase()}" else "On track, with one thing to watch" }
        else -> if (done) "An okay day" else "Off to a steady start"
    }

    val names = i.eaten.groupBy { it.name }.mapValues { (_, v) -> v.sumOf { it.n.kcal } }.entries.sortedByDescending { it.value }.take(3).map { it.key }
    val mealsCount = byMeal.size
    val summary = buildString {
        append(if (i.isToday && !done) "So far you have eaten " else "You ate ")
        append(kcal.kcalText()).append(" kcal (").append((ratio * 100).roundToInt()).append("% of ").append(t.kcal.n()).append(") across ")
        append(mealsCount).append(if (mealsCount == 1) " meal" else " meals")
        append(". Biggest: ").append(biggestFood?.key ?: names.first()).append(". ")
        append("Protein ").append(total.protein.g()).append(" g, carbs ").append(total.carbs.g()).append(" g, fat ").append(total.fat.g()).append(" g.")
    }

    return CoachReview(headline, mood, summary, split, targetSplit, sorted, suggestions, empty = false)
}
