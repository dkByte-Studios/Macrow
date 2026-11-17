package com.macrow.app.data

import androidx.room.withTransaction
import com.macrow.app.domain.Nutrients
import com.macrow.app.domain.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class MacrowRepository(private val db: MacrowDatabase, private val store: SettingsStore) {
    private val dao = db.dao()
    val settingsFlow: Flow<Settings> = store.settings

    companion object {
        fun today(): Long = LocalDate.now().toEpochDay()

        /** Escapes the characters LIKE treats as wildcards, matching `ESCAPE '\'` in the query. */
        fun likeEscape(q: String) = q.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    }

    // Foods
    suspend fun ensureSeeded() {
        if (dao.foodCount() == 0) dao.insertFoods(BuiltInFoods)
    }

    fun foods(query: String, filter: String): Flow<List<Food>> = dao.searchFoods(likeEscape(query), filter)
    suspend fun food(id: Long): Food? = dao.food(id)
    suspend fun foodByBarcode(code: String): Food? = dao.foodByBarcode(code)

    /** Saves a new or edited food and returns its id. Numbers are clamped so a typo cannot ruin the totals. */
    suspend fun saveFood(f: Food): Long {
        val clean = f.copy(
            name = f.name.trim().take(60), brand = f.brand.trim().take(40), servingLabel = f.servingLabel.trim().take(30).ifEmpty { "1 serving" },
            servingGrams = f.servingGrams.coerceIn(1.0, 20000.0), kcal = f.kcal.coerceIn(0.0, 20000.0),
            protein = f.protein.coerceIn(0.0, 5000.0), carbs = f.carbs.coerceIn(0.0, 5000.0), fat = f.fat.coerceIn(0.0, 5000.0),
            fiber = f.fiber.coerceIn(0.0, 5000.0), sugar = f.sugar.coerceIn(0.0, 5000.0), sodiumMg = f.sodiumMg.coerceIn(0.0, 1e6),
            ironMg = f.ironMg.coerceIn(0.0, 1e6), calciumMg = f.calciumMg.coerceIn(0.0, 1e6), vitCMg = f.vitCMg.coerceIn(0.0, 1e6),
            potassiumMg = f.potassiumMg.coerceIn(0.0, 1e6), barcode = f.barcode?.takeIf { it.length in 8..14 && it.all { c -> c in '0'..'9' } },
        )
        return if (clean.id == 0L) dao.insertFood(clean) else { dao.updateFood(clean); clean.id }
    }

    suspend fun archiveFood(id: Long) = dao.archiveFood(id)

    /** Keeps an online food on the phone the first time you use it, so it works offline after that. */
    suspend fun cacheOnline(f: Food): Food {
        val existing = (f.barcode?.let { dao.foodByBarcode(it) }) ?: dao.onlineByName(f.name, f.brand)
        if (existing != null) return existing
        return f.copy(id = dao.insertFood(f.copy(id = 0)))
    }

    // Diary and plan
    fun dayEntries(day: Long, planned: Boolean) = dao.entriesOn(day, planned)
    fun entriesBetween(from: Long, to: Long, planned: Boolean = false) = dao.entriesBetween(from, to, planned)
    fun loggedDays() = dao.loggedDays()

    suspend fun addEntry(day: Long, meal: String, food: Food, servings: Double, planned: Boolean) {
        db.withTransaction {
            dao.insertEntry(Entry(day = day, meal = meal, foodId = food.id, servings = servings.coerceIn(0.05, 99.0), planned = planned, createdAt = System.currentTimeMillis()))
            if (!planned) dao.touchFood(food.id, System.currentTimeMillis())
        }
    }

    suspend fun updateEntry(e: Entry) = dao.updateEntry(e.copy(servings = e.servings.coerceIn(0.05, 99.0)))
    suspend fun deleteEntry(e: Entry) = dao.deleteEntry(e.id)
    suspend fun restoreEntry(e: Entry) { dao.insertEntry(e) }
    suspend fun dinnerLogged(day: Long) = dao.countMeal(day, "DINNER") > 0

    /** Copies everything planned for [day] into the diary. */
    suspend fun logPlan(day: Long) = db.withTransaction {
        val now = System.currentTimeMillis()
        val planned = dao.plannedOn(day)
        dao.insertEntries(planned.map { it.entry.copy(id = 0, planned = false, createdAt = now) })
        planned.forEach { dao.touchFood(it.food.id, now) }
    }

    // Recipes
    suspend fun recipe(foodId: Long): Recipe? = dao.recipe(foodId)
    suspend fun recipeItems(foodId: Long) = dao.recipeItems(foodId)

    /**
     * Saves a recipe as a food whose numbers are per serving. The ingredient list is kept so you can edit it,
     * but later changes to an ingredient food do not rewrite recipes you already made.
     */
    suspend fun saveRecipe(foodId: Long, name: String, servings: Int, items: List<Pair<Food, Double>>): Long = db.withTransaction {
        val n = servings.coerceIn(1, 99)
        val total = items.fold(Nutrients.ZERO) { a, (food, q) -> a + food.nutrients * q }
        val per = total * (1.0 / n)
        val grams = items.sumOf { (food, q) -> food.servingGrams * q } / n
        val base = Food(
            id = foodId, name = name.trim().take(60), servingLabel = "1 serving", servingGrams = grams.coerceIn(1.0, 20000.0), kcal = per.kcal, protein = per.protein,
            carbs = per.carbs, fat = per.fat, fiber = per.fiber, sugar = per.sugar, sodiumMg = per.sodiumMg, ironMg = per.ironMg,
            calciumMg = per.calciumMg, vitCMg = per.vitCMg, potassiumMg = per.potassiumMg, source = Source.RECIPE,
        )
        val id = if (foodId == 0L) dao.insertFood(base) else { dao.updateFood(dao.food(foodId)!!.let { old -> base.copy(lastUsedAt = old.lastUsedAt, useCount = old.useCount) }); foodId }
        dao.insertRecipe(Recipe(id, n))
        dao.deleteRecipeItems(id)
        dao.insertRecipeItems(items.map { (food, q) -> RecipeItem(recipeId = id, foodId = food.id, servings = q.coerceIn(0.05, 999.0)) })
        id
    }

    // Weight and water
    fun weights() = dao.weights()
    suspend fun setWeight(day: Long, kg: Double) = dao.upsertWeight(Weight(day, kg.coerceIn(20.0, 500.0)))
    suspend fun deleteWeight(day: Long) = dao.deleteWeight(day)
    fun water(day: Long) = dao.waterOn(day)
    suspend fun addWater(day: Long, ml: Int) = dao.insertWater(Water(day = day, ml = ml))
    suspend fun undoWater(day: Long) = dao.undoWater(day)

    // Settings
    suspend fun saveProfile(profile: Profile, targets: com.macrow.app.domain.Targets, logWeightOn: Long?) {
        store.saveProfile(profile, targets)
        if (logWeightOn != null) setWeight(logWeightOn, profile.weightKg)
    }

    suspend fun setTargets(t: com.macrow.app.domain.Targets) = store.setTargets(t)
    suspend fun setWaterGoal(ml: Int) = store.setWaterGoal(ml)
    suspend fun setDarkMode(on: Boolean) = store.setDarkMode(on)
    suspend fun setHaptics(on: Boolean) = store.setHaptics(on)
    suspend fun setReminder(on: Boolean, minutes: Int) = store.setReminder(on, minutes)
    suspend fun setBackupFolder(uri: String) = store.setBackupFolder(uri)
    suspend fun setAutoBackup(on: Boolean) = store.setAutoBackup(on)
    suspend fun setBackupResult(at: Long, error: String?) = store.setBackupResult(at, error)

    // Backup
    suspend fun exportBackup(): BackupData {
        val s = settingsFlow.first()
        return db.withTransaction {
            BackupData(dao.allFoods(), dao.allEntries(), dao.allRecipes(), dao.allRecipeItems(), dao.allWeights(), dao.allWater(), s.profile, s.targets, s.waterGoalMl)
        }
    }

    /** Replaces everything with [d] in one transaction: either all of it is restored or none. */
    suspend fun restoreBackup(d: BackupData) {
        db.withTransaction {
            dao.clearEntries(); dao.clearRecipeItems(); dao.clearRecipes(); dao.clearWeights(); dao.clearWater(); dao.clearFoods()
            dao.insertFoods(d.foods)
            dao.putRecipes(d.recipes); dao.putRecipeItems(d.recipeItems); dao.putEntries(d.entries); dao.putWeights(d.weights); dao.putWater(d.water)
        }
        if (d.profile != null && d.targets != null) store.saveProfile(d.profile, d.targets)
        d.waterGoalMl?.let { store.setWaterGoal(it) }
    }

    suspend fun csv(): String {
        val rows = dao.allEntries().filter { !it.planned }.sortedWith(compareBy({ it.day }, { it.id }))
        val foods = dao.allFoods().associateBy { it.id }
        val sb = StringBuilder("date,meal,food,servings,kcal,protein_g,carbs_g,fat_g\n")
        for (e in rows) {
            val f = foods[e.foodId] ?: continue
            val n = f.nutrients * e.servings
            sb.append(LocalDate.ofEpochDay(e.day)).append(',').append(e.meal.lowercase()).append(',').append(csvCell(f.name)).append(',')
                .append(e.servings).append(',').append(Math.round(n.kcal)).append(',').append("%.1f".format(java.util.Locale.US, n.protein)).append(',')
                .append("%.1f".format(java.util.Locale.US, n.carbs)).append(',').append("%.1f".format(java.util.Locale.US, n.fat)).append('\n')
        }
        return sb.toString()
    }

    /** Quotes a CSV cell, and defuses a leading = + - @ so a spreadsheet never runs a food name as a formula. */
    private fun csvCell(s: String): String {
        val safe = if (s.firstOrNull() in setOf('=', '+', '-', '@')) "'$s" else s
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }
}
