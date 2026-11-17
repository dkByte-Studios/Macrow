package com.macrow.app.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.macrow.app.domain.Activity
import com.macrow.app.domain.Goal
import com.macrow.app.domain.Profile
import com.macrow.app.domain.Sex
import com.macrow.app.domain.Targets
import com.macrow.app.domain.targetsFor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "settings", corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() })

data class Settings(
    val onboarded: Boolean = false,
    val darkMode: Boolean = false,
    val haptics: Boolean = true,
    val profile: Profile = Profile(),
    val targets: Targets = targetsFor(Profile()),
    val waterGoalMl: Int = 2500,
    val reminderOn: Boolean = false,
    /** Minutes after midnight. */
    val reminderMinutes: Int = 20 * 60,
    /** Folder (a content:// tree address) the backups go to; null until chosen. */
    val backupFolder: String? = null,
    val autoBackup: Boolean = true,
    val lastBackupAt: Long = 0,
    val backupError: String? = null,
)

class SettingsStore(private val context: Context) {
    private val onboarded = booleanPreferencesKey("onboarded")
    private val dark = booleanPreferencesKey("dark_mode")
    private val haptics = booleanPreferencesKey("haptics")
    private val sex = stringPreferencesKey("sex")
    private val age = intPreferencesKey("age")
    private val height = intPreferencesKey("height")
    private val weight = stringPreferencesKey("weight")
    private val activity = stringPreferencesKey("activity")
    private val goal = stringPreferencesKey("goal")
    private val kcal = intPreferencesKey("t_kcal")
    private val protein = intPreferencesKey("t_protein")
    private val carbs = intPreferencesKey("t_carbs")
    private val fat = intPreferencesKey("t_fat")
    private val water = intPreferencesKey("water_goal")
    private val reminder = booleanPreferencesKey("reminder_on")
    private val reminderAt = intPreferencesKey("reminder_minutes")
    private val folder = stringPreferencesKey("backup_folder")
    private val auto = booleanPreferencesKey("auto_backup")
    private val last = longPreferencesKey("backup_last")
    private val error = stringPreferencesKey("backup_error")
    private val builtinVersion = intPreferencesKey("builtin_version")

    val settings: Flow<Settings> = context.dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { read(it) }

    private fun read(p: Preferences): Settings {
        val d = Profile()
        val profile = Profile(
            sex = p[sex]?.let { s -> Sex.entries.firstOrNull { it.name == s } } ?: d.sex,
            age = p[age] ?: d.age,
            heightCm = p[height] ?: d.heightCm,
            weightKg = p[weight]?.toDoubleOrNull() ?: d.weightKg,
            activity = p[activity]?.let { s -> Activity.entries.firstOrNull { it.name == s } } ?: d.activity,
            goal = p[goal]?.let { s -> Goal.entries.firstOrNull { it.name == s } } ?: d.goal,
        )
        val fallback = targetsFor(profile)
        return Settings(
            onboarded = p[onboarded] ?: false,
            darkMode = p[dark] ?: false,
            haptics = p[haptics] ?: true,
            profile = profile,
            targets = Targets(p[kcal] ?: fallback.kcal, p[protein] ?: fallback.proteinG, p[carbs] ?: fallback.carbsG, p[fat] ?: fallback.fatG),
            waterGoalMl = p[water] ?: 2500,
            reminderOn = p[reminder] ?: false,
            reminderMinutes = p[reminderAt] ?: (20 * 60),
            backupFolder = p[folder],
            autoBackup = p[auto] ?: true,
            lastBackupAt = p[last] ?: 0,
            backupError = p[error],
        )
    }

    suspend fun builtinVersion(): Int = context.dataStore.data.first()[builtinVersion] ?: 0
    suspend fun setBuiltinVersion(v: Int) { context.dataStore.edit { it[builtinVersion] = v } }

    suspend fun setDarkMode(on: Boolean) { context.dataStore.edit { it[dark] = on } }
    suspend fun setHaptics(on: Boolean) { context.dataStore.edit { it[haptics] = on } }
    suspend fun setWaterGoal(ml: Int) { context.dataStore.edit { it[water] = ml } }
    suspend fun setReminder(on: Boolean, minutes: Int) { context.dataStore.edit { it[reminder] = on; it[reminderAt] = minutes } }

    /** Saves who you are and the targets worked out from it, and marks onboarding done. */
    suspend fun saveProfile(profile: Profile, targets: Targets) {
        context.dataStore.edit {
            it[onboarded] = true
            it[sex] = profile.sex.name
            it[age] = profile.age
            it[height] = profile.heightCm
            it[weight] = profile.weightKg.toString()
            it[activity] = profile.activity.name
            it[goal] = profile.goal.name
            writeTargets(it, targets)
        }
    }

    suspend fun setTargets(t: Targets) { context.dataStore.edit { writeTargets(it, t) } }

    private fun writeTargets(it: androidx.datastore.preferences.core.MutablePreferences, t: Targets) {
        it[kcal] = t.kcal
        it[protein] = t.proteinG
        it[carbs] = t.carbsG
        it[fat] = t.fatG
    }

    suspend fun setBackupFolder(uri: String) { context.dataStore.edit { it[folder] = uri; it.remove(error) } }
    suspend fun setAutoBackup(on: Boolean) { context.dataStore.edit { it[auto] = on } }

    /** Records how the last backup went: [error] null means it worked at [at]. */
    suspend fun setBackupResult(at: Long, error: String?) {
        context.dataStore.edit {
            if (error == null) { it[last] = at; it.remove(this.error) } else it[this.error] = error
        }
    }
}
