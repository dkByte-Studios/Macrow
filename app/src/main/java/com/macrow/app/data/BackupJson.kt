package com.macrow.app.data

import com.macrow.app.domain.Activity
import com.macrow.app.domain.Goal
import com.macrow.app.domain.Profile
import com.macrow.app.domain.Sex
import com.macrow.app.domain.Targets
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class BackupException(message: String) : Exception(message)

private const val FORMAT = 1

/** Everything the app stores, plus who you are and your targets (not the backup folder or reminders). */
data class BackupData(
    val foods: List<Food>,
    val entries: List<Entry>,
    val recipes: List<Recipe>,
    val recipeItems: List<RecipeItem>,
    val weights: List<Weight>,
    val water: List<Water>,
    val profile: Profile?,
    val targets: Targets?,
    val waterGoalMl: Int?,
) {
    fun isEmpty() = entries.isEmpty() && weights.isEmpty() && water.isEmpty() && foods.none { it.source == Source.CUSTOM || it.source == Source.RECIPE }

    fun toJson(): String = JSONObject().apply {
        put("app", "Macrow")
        put("format", FORMAT)
        put("foods", JSONArray().also { a -> foods.forEach { a.put(it.toJson()) } })
        put("entries", JSONArray().also { a -> entries.forEach { a.put(JSONObject().put("id", it.id).put("day", it.day).put("meal", it.meal).put("foodId", it.foodId).put("servings", it.servings).put("planned", it.planned).put("createdAt", it.createdAt)) } })
        put("recipes", JSONArray().also { a -> recipes.forEach { a.put(JSONObject().put("foodId", it.foodId).put("servings", it.servings)) } })
        put("recipeItems", JSONArray().also { a -> recipeItems.forEach { a.put(JSONObject().put("id", it.id).put("recipeId", it.recipeId).put("foodId", it.foodId).put("servings", it.servings)) } })
        put("weights", JSONArray().also { a -> weights.forEach { a.put(JSONObject().put("day", it.day).put("kg", it.kg)) } })
        put("water", JSONArray().also { a -> water.forEach { a.put(JSONObject().put("id", it.id).put("day", it.day).put("ml", it.ml)) } })
        if (profile != null) put("profile", JSONObject().put("sex", profile.sex.name).put("age", profile.age).put("heightCm", profile.heightCm).put("weightKg", profile.weightKg).put("activity", profile.activity.name).put("goal", profile.goal.name))
        if (targets != null) put("targets", JSONObject().put("kcal", targets.kcal).put("protein", targets.proteinG).put("carbs", targets.carbsG).put("fat", targets.fatG))
        if (waterGoalMl != null) put("waterGoalMl", waterGoalMl)
    }.toString()
}

private fun Food.toJson() = JSONObject()
    .put("id", id).put("name", name).put("brand", brand).put("servingLabel", servingLabel).put("servingGrams", servingGrams)
    .put("kcal", kcal).put("protein", protein).put("carbs", carbs).put("fat", fat).put("fiber", fiber).put("sugar", sugar)
    .put("sodiumMg", sodiumMg).put("ironMg", ironMg).put("calciumMg", calciumMg).put("vitCMg", vitCMg).put("potassiumMg", potassiumMg)
    .put("barcode", barcode ?: JSONObject.NULL).put("source", source).put("archived", archived).put("lastUsedAt", lastUsedAt).put("useCount", useCount)

private fun <T> JSONArray?.mapObjects(f: (JSONObject) -> T): List<T> =
    if (this == null) emptyList() else (0 until length()).map { f(getJSONObject(it)) }

private fun JSONObject.num(key: String, max: Double = 1e7): Double {
    val v = getDouble(key)
    if (v.isNaN() || v < 0 || v > max) throw BackupException("The backup has a number out of range ($key).")
    return v
}

private fun JSONObject.text(key: String, max: Int): String = optString(key, "").take(max)

/** Reads and checks a backup. Throws [BackupException] with a plain message when it is not a good Macrow backup. */
fun parseBackup(text: String): BackupData {
    try {
        val o = JSONObject(text)
        if (o.optString("app") != "Macrow") throw BackupException("That file is not a Macrow backup.")
        if (o.optInt("format", 0) != FORMAT) throw BackupException("That backup is from a newer version of Macrow.")
        val foods = o.optJSONArray("foods").mapObjects {
            Food(
                id = it.getLong("id"), name = it.text("name", 60).ifBlank { throw BackupException("A food in the backup has no name.") },
                brand = it.text("brand", 40), servingLabel = it.text("servingLabel", 30).ifBlank { "1 serving" },
                servingGrams = it.num("servingGrams", 20000.0), kcal = it.num("kcal"), protein = it.num("protein"), carbs = it.num("carbs"), fat = it.num("fat"),
                fiber = it.num("fiber"), sugar = it.num("sugar"), sodiumMg = it.num("sodiumMg"), ironMg = it.num("ironMg"), calciumMg = it.num("calciumMg"),
                vitCMg = it.num("vitCMg"), potassiumMg = it.num("potassiumMg"),
                barcode = if (it.isNull("barcode")) null else it.optString("barcode").take(14),
                source = it.optString("source", Source.CUSTOM).takeIf { s -> s in setOf(Source.BUILTIN, Source.ONLINE, Source.CUSTOM, Source.RECIPE) } ?: Source.CUSTOM,
                archived = it.optBoolean("archived"), lastUsedAt = it.optLong("lastUsedAt"), useCount = it.optInt("useCount"),
            )
        }
        val ids = foods.map { it.id }.toSet()
        if (ids.size != foods.size) throw BackupException("The backup lists the same food twice.")
        val entries = o.optJSONArray("entries").mapObjects {
            Entry(it.getLong("id"), it.getLong("day"), it.getString("meal"), it.getLong("foodId"), it.num("servings", 1000.0), it.optBoolean("planned"), it.optLong("createdAt"))
        }.filter { it.foodId in ids }
        val recipes = o.optJSONArray("recipes").mapObjects { Recipe(it.getLong("foodId"), it.getInt("servings")) }.filter { it.foodId in ids }
        val items = o.optJSONArray("recipeItems").mapObjects { RecipeItem(it.getLong("id"), it.getLong("recipeId"), it.getLong("foodId"), it.num("servings", 1000.0)) }
            .filter { it.recipeId in ids && it.foodId in ids }
        val weights = o.optJSONArray("weights").mapObjects { Weight(it.getLong("day"), it.num("kg", 1000.0)) }
        val water = o.optJSONArray("water").mapObjects { Water(it.getLong("id"), it.getLong("day"), it.getInt("ml").also { ml -> if (ml < 0 || ml > 20000) throw BackupException("The backup has a water amount out of range.") }) }
        val profile = o.optJSONObject("profile")?.let {
            Profile(
                sex = Sex.entries.firstOrNull { s -> s.name == it.optString("sex") } ?: Sex.MALE,
                age = it.optInt("age", 25).coerceIn(10, 110),
                heightCm = it.optInt("heightCm", 170).coerceIn(100, 250),
                weightKg = it.optDouble("weightKg", 70.0).coerceIn(30.0, 400.0),
                activity = Activity.entries.firstOrNull { s -> s.name == it.optString("activity") } ?: Activity.LIGHT,
                goal = Goal.entries.firstOrNull { s -> s.name == it.optString("goal") } ?: Goal.MAINTAIN,
            )
        }
        val targets = o.optJSONObject("targets")?.let {
            Targets(it.getInt("kcal").coerceIn(500, 10000), it.getInt("protein").coerceIn(0, 1000), it.getInt("carbs").coerceIn(0, 2000), it.getInt("fat").coerceIn(0, 1000))
        }
        return BackupData(foods, entries, recipes, items, weights, water, profile, targets, if (o.has("waterGoalMl")) o.getInt("waterGoalMl").coerceIn(500, 10000) else null)
    } catch (e: BackupException) {
        throw e
    } catch (e: JSONException) {
        throw BackupException("That file isn't a readable Macrow backup.")
    }
}
