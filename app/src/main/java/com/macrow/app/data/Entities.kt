package com.macrow.app.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.macrow.app.domain.Nutrients

object Source {
    const val BUILTIN = "BUILTIN"
    const val ONLINE = "ONLINE"
    const val CUSTOM = "CUSTOM"
    const val RECIPE = "RECIPE"
}

/** A food. All nutrient numbers are for one serving of [servingGrams]. */
@Entity(tableName = "foods", indices = [Index("barcode"), Index("name")])
data class Food(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: String = "",
    val servingLabel: String = "1 serving",
    val servingGrams: Double = 100.0,
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
    val barcode: String? = null,
    val source: String = Source.CUSTOM,
    val archived: Boolean = false,
    val lastUsedAt: Long = 0,
    val useCount: Int = 0,
) {
    val nutrients: Nutrients
        get() = Nutrients(kcal, protein, carbs, fat, fiber, sugar, sodiumMg, ironMg, calciumMg, vitCMg, potassiumMg)

    val isRecipe get() = source == Source.RECIPE
}

/** One logged (or planned) portion of a food on a day. [servings] is how many of the food's servings. */
@Entity(tableName = "entries", indices = [Index("day", "planned"), Index("foodId")])
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Long,
    val meal: String,
    val foodId: Long,
    val servings: Double,
    val planned: Boolean = false,
    val createdAt: Long = 0,
)

data class EntryWithFood(
    @Embedded val entry: Entry,
    @Relation(parentColumn = "foodId", entityColumn = "id") val food: Food,
) {
    val nutrients: Nutrients get() = food.nutrients * entry.servings
}

/** A recipe is also a food (same id); this row says how many servings the ingredients make. */
@Entity(tableName = "recipes")
data class Recipe(@PrimaryKey val foodId: Long, val servings: Int)

@Entity(tableName = "recipe_items", indices = [Index("recipeId")])
data class RecipeItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val foodId: Long,
    val servings: Double,
)

data class RecipeItemWithFood(
    @Embedded val item: RecipeItem,
    @Relation(parentColumn = "foodId", entityColumn = "id") val food: Food,
)

@Entity(tableName = "weights")
data class Weight(@PrimaryKey val day: Long, val kg: Double)

@Entity(tableName = "water", indices = [Index("day")])
data class Water(@PrimaryKey(autoGenerate = true) val id: Long = 0, val day: Long, val ml: Int)
