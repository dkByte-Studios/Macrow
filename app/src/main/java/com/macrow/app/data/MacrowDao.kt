package com.macrow.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MacrowDao {
    // Foods. [filter] is ALL, RECENT, MINE or RECIPES. [q] is already escaped for LIKE.
    @Query(
        "SELECT * FROM foods WHERE archived = 0 AND (name LIKE '%' || :q || '%' ESCAPE '\\' OR brand LIKE '%' || :q || '%' ESCAPE '\\') AND (" +
            ":filter = 'ALL' OR (:filter = 'RECENT' AND lastUsedAt > 0) OR (:filter = 'MINE' AND source IN ('CUSTOM','RECIPE')) OR (:filter = 'RECIPES' AND source = 'RECIPE')) " +
            "ORDER BY CASE WHEN :filter = 'RECENT' THEN lastUsedAt END DESC, useCount DESC, name COLLATE NOCASE LIMIT 80",
    )
    fun searchFoods(q: String, filter: String): Flow<List<Food>>

    @Query("SELECT * FROM foods WHERE id = :id") suspend fun food(id: Long): Food?
    @Query("SELECT * FROM foods WHERE barcode = :code AND archived = 0 LIMIT 1") suspend fun foodByBarcode(code: String): Food?
    @Query("SELECT * FROM foods WHERE source = 'ONLINE' AND name = :name AND brand = :brand AND archived = 0 LIMIT 1") suspend fun onlineByName(name: String, brand: String): Food?
    @Query("SELECT COUNT(*) FROM foods") suspend fun foodCount(): Int
    @Query("SELECT * FROM foods WHERE archived = 0 AND kcal > 0") suspend fun allActive(): List<Food>
    @Query("SELECT name FROM foods WHERE source = 'BUILTIN'") suspend fun builtinNames(): List<String>
    @Insert suspend fun insertFood(food: Food): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertFoods(foods: List<Food>)
    @Update suspend fun updateFood(food: Food)
    @Query("UPDATE foods SET archived = 1 WHERE id = :id") suspend fun archiveFood(id: Long)
    @Query("UPDATE foods SET lastUsedAt = :at, useCount = useCount + 1 WHERE id = :id") suspend fun touchFood(id: Long, at: Long)

    // Diary and plan entries.
    @Transaction
    @Query("SELECT * FROM entries WHERE day = :day AND planned = :planned ORDER BY id")
    fun entriesOn(day: Long, planned: Boolean): Flow<List<EntryWithFood>>

    @Transaction
    @Query("SELECT * FROM entries WHERE day BETWEEN :from AND :to AND planned = :planned ORDER BY day, id")
    fun entriesBetween(from: Long, to: Long, planned: Boolean): Flow<List<EntryWithFood>>

    @Transaction
    @Query("SELECT * FROM entries WHERE day = :day AND planned = 1 ORDER BY id")
    suspend fun plannedOn(day: Long): List<EntryWithFood>

    @Query("SELECT DISTINCT day FROM entries WHERE planned = 0 ORDER BY day DESC LIMIT 400") fun loggedDays(): Flow<List<Long>>
    @Query("SELECT COUNT(*) FROM entries WHERE day = :day AND planned = 0 AND meal = :meal") suspend fun countMeal(day: Long, meal: String): Int
    @Insert suspend fun insertEntry(entry: Entry): Long
    @Insert suspend fun insertEntries(entries: List<Entry>)
    @Update suspend fun updateEntry(entry: Entry)
    @Query("DELETE FROM entries WHERE id = :id") suspend fun deleteEntry(id: Long)

    // Recipes.
    @Query("SELECT * FROM recipes WHERE foodId = :foodId") suspend fun recipe(foodId: Long): Recipe?
    @Transaction @Query("SELECT * FROM recipe_items WHERE recipeId = :foodId ORDER BY id") suspend fun recipeItems(foodId: Long): List<RecipeItemWithFood>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRecipe(recipe: Recipe)
    @Insert suspend fun insertRecipeItems(items: List<RecipeItem>)
    @Query("DELETE FROM recipe_items WHERE recipeId = :foodId") suspend fun deleteRecipeItems(foodId: Long)

    // Weight and water.
    @Query("SELECT * FROM weights ORDER BY day") fun weights(): Flow<List<Weight>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertWeight(weight: Weight)
    @Query("DELETE FROM weights WHERE day = :day") suspend fun deleteWeight(day: Long)
    @Query("SELECT COALESCE(SUM(ml), 0) FROM water WHERE day = :day") fun waterOn(day: Long): Flow<Int>
    @Insert suspend fun insertWater(water: Water)
    @Query("DELETE FROM water WHERE id = (SELECT MAX(id) FROM water WHERE day = :day)") suspend fun undoWater(day: Long)

    // Backup: whole tables.
    @Query("SELECT * FROM foods") suspend fun allFoods(): List<Food>
    @Query("SELECT * FROM entries") suspend fun allEntries(): List<Entry>
    @Query("SELECT * FROM recipes") suspend fun allRecipes(): List<Recipe>
    @Query("SELECT * FROM recipe_items") suspend fun allRecipeItems(): List<RecipeItem>
    @Query("SELECT * FROM weights") suspend fun allWeights(): List<Weight>
    @Query("SELECT * FROM water") suspend fun allWater(): List<Water>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putRecipes(r: List<Recipe>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putRecipeItems(r: List<RecipeItem>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putEntries(r: List<Entry>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putWeights(r: List<Weight>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putWater(r: List<Water>)
    @Query("DELETE FROM foods") suspend fun clearFoods()
    @Query("DELETE FROM entries") suspend fun clearEntries()
    @Query("DELETE FROM recipes") suspend fun clearRecipes()
    @Query("DELETE FROM recipe_items") suspend fun clearRecipeItems()
    @Query("DELETE FROM weights") suspend fun clearWeights()
    @Query("DELETE FROM water") suspend fun clearWater()
}
