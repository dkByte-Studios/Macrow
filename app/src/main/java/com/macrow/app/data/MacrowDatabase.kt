package com.macrow.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Food::class, Entry::class, Recipe::class, RecipeItem::class, Weight::class, Water::class],
    version = 1,
    exportSchema = true,
)
abstract class MacrowDatabase : RoomDatabase() {
    abstract fun dao(): MacrowDao

    companion object {
        fun create(context: Context): MacrowDatabase =
            Room.databaseBuilder(context.applicationContext, MacrowDatabase::class.java, "macrow.db").build()
    }
}
