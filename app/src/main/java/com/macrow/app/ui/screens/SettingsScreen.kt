package com.macrow.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macrow.app.MacrowApp
import com.macrow.app.Reminders
import com.macrow.app.data.BackupData
import com.macrow.app.domain.Limits
import com.macrow.app.domain.Targets
import com.macrow.app.domain.splitMacros
import com.macrow.app.ui.UiErrors
import com.macrow.app.ui.components.MacrowCard
import com.macrow.app.ui.components.MacrowDialog
import com.macrow.app.ui.components.MacrowField
import com.macrow.app.ui.components.SectionLabel
import com.macrow.app.ui.components.SoftButton
import com.macrow.app.ui.components.Stepper
import com.macrow.app.ui.components.SwitchRow
import com.macrow.app.ui.components.TopBar
import com.macrow.app.ui.theme.Tone
import com.macrow.app.ui.theme.macrow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class Edit { NONE, KCAL, MACROS, WATER, REMINDER_TIME }

@Composable
fun SettingsScreen(onBack: () -> Unit, onEditProfile: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as MacrowApp
    val repo = app.repo
    val scope = rememberCoroutineScope()
    val p = MaterialTheme.macrow
    val s by repo.settingsFlow.collectAsStateWithLifecycle(com.macrow.app.data.Settings())
    var edit by rememberSaveable { mutableStateOf(Edit.NONE) }
    var pendingRestore by remember { mutableStateOf<BackupData?>(null) }
    var reminderMinutes by rememberSaveable { mutableIntStateOf(s.reminderMinutes) }
    var busy by remember { mutableStateOf(false) }

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            // Let go of the old folder so the phone does not keep access we no longer use.
            s.backupFolder?.takeIf { it != uri.toString() }?.let { old -> runCatching { context.contentResolver.releasePersistableUriPermission(old.toUri(), flags) } }
            runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
            scope.launch {
                repo.setBackupFolder(uri.toString())
                app.backup.backupNow().onSuccess { UiErrors.say("Backup folder set. First backup saved.") }.onFailure { UiErrors.report(it) }
            }
        }
    }
    val pickBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch {
            try { pendingRestore = app.backup.read(uri) } catch (e: Exception) { UiErrors.report(e) }
        }
    }
    val saveCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null) scope.launch {
            try { app.backup.writeText(uri, repo.csv()); UiErrors.say("Diary exported") } catch (e: Exception) { UiErrors.say("Couldn't save the file.") }
        }
    }
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) scope.launch { repo.setReminder(true, s.reminderMinutes); Reminders.schedule(context, s.reminderMinutes) }
        else UiErrors.say("Notifications are off for Macrow. Turn them on in Android settings to get reminders.")
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("Settings", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Look and feel", Modifier.padding(start = 4.dp))
            MacrowCard(padding = 10.dp) {
                SwitchRow("Dark mode", s.darkMode, { scope.launch { repo.setDarkMode(it) } })
                SwitchRow("Haptic feedback", s.haptics, { scope.launch { repo.setHaptics(it) } })
            }

            SectionLabel("Your targets", Modifier.padding(start = 4.dp, top = 8.dp))
            MacrowCard(padding = 4.dp) {
                Row2("Daily calories", "%,d kcal".format(s.targets.kcal)) { edit = Edit.KCAL }
                Row2("Protein, carbs, fat", "${s.targets.proteinG} / ${s.targets.carbsG} / ${s.targets.fatG} g") { edit = Edit.MACROS }
                Row2("Water goal", "%,d ml".format(s.waterGoalMl)) { edit = Edit.WATER }
                Row2("About you", "${s.profile.sex.label}, ${s.profile.age}, ${s.profile.heightCm} cm", onEditProfile)
            }

            SectionLabel("Reminder", Modifier.padding(start = 4.dp, top = 8.dp))
            MacrowCard(padding = 10.dp) {
                SwitchRow(
                    "Remind me to log my meals", s.reminderOn,
                    { on ->
                        if (!on) scope.launch { repo.setReminder(false, s.reminderMinutes); Reminders.cancel(context) }
                        else if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else scope.launch { repo.setReminder(true, s.reminderMinutes); Reminders.schedule(context, s.reminderMinutes) }
                    },
                    subtitle = "Skipped on days you already logged dinner",
                )
                if (s.reminderOn) Row2("Time", timeText(s.reminderMinutes)) { reminderMinutes = s.reminderMinutes; edit = Edit.REMINDER_TIME }
            }

            SectionLabel("Backup", Modifier.padding(start = 4.dp, top = 8.dp))
            MacrowCard {
                Text(
                    when {
                        s.backupFolder == null -> "Pick a folder and Macrow keeps backups of your diary there. Nothing leaves your phone."
                        s.backupError != null -> s.backupError!!
                        s.lastBackupAt > 0 -> "Last backup ${lastBackupText(s.lastBackupAt)}"
                        else -> "Folder chosen. No backup yet."
                    },
                    style = MaterialTheme.typography.bodyMedium, color = if (s.backupError != null) p.bad else p.sub,
                )
                Spacer(Modifier.height(10.dp))
                SoftButton(if (s.backupFolder == null) "Choose backup folder" else "Change backup folder", { pickFolder.launch(null) }, Modifier.fillMaxWidth())
                if (s.backupFolder != null) {
                    Spacer(Modifier.height(8.dp))
                    SwitchRow("Back up automatically", s.autoBackup, { scope.launch { repo.setAutoBackup(it) } }, subtitle = "Once a day, when you open the app")
                    SoftButton(
                        if (busy) "Backing up…" else "Back up now",
                        {
                            if (!busy) scope.launch {
                                busy = true
                                app.backup.backupNow().onSuccess { UiErrors.say("Backup saved") }.onFailure { UiErrors.report(it) }
                                busy = false
                            }
                        },
                        Modifier.fillMaxWidth(), tone = Tone.Blue,
                    )
                }
                Spacer(Modifier.height(8.dp))
                SoftButton("Restore from a backup file", { pickBackup.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, Modifier.fillMaxWidth(), tone = Tone.Gold)
            }

            SectionLabel("Your data", Modifier.padding(start = 4.dp, top = 8.dp))
            MacrowCard(padding = 4.dp) {
                Row2("Export diary as CSV", "") { saveCsv.launch("Macrow-diary-${LocalDate.now()}.csv") }
            }

            val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "" }
            Text(
                "Macrow $version. Food data from Open Food Facts (open database) when you look something up online. Built-in foods are typical values, not lab results. Targets and goals are estimates, not medical advice.",
                Modifier.padding(horizontal = 4.dp, vertical = 12.dp), style = MaterialTheme.typography.bodySmall, color = p.muted,
            )
            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }
    }

    when (edit) {
        Edit.KCAL -> {
            var text by rememberSaveable { mutableStateOf(s.targets.kcal.toString()) }
            val v = text.toIntOrNull()
            val err = if (v == null || v !in Limits.MIN_KCAL_TARGET..Limits.MAX_KCAL_TARGET) "${"%,d".format(Limits.MIN_KCAL_TARGET)} to ${"%,d".format(Limits.MAX_KCAL_TARGET)}" else null
            MacrowDialog("Daily calories", { edit = Edit.NONE }, "Save", {
                if (err == null && v != null) scope.launch { repo.setTargets(splitMacros(v, s.profile.weightKg, s.profile.goal)); edit = Edit.NONE }
            }) {
                MacrowField(text, { text = it.filter(Char::isDigit) }, "Calories", Modifier.fillMaxWidth(), KeyboardType.Number, err, suffix = "kcal", maxLength = 4)
                Text("Protein, carbs and fat are worked out again from this.", style = MaterialTheme.typography.bodySmall, color = p.sub)
            }
        }
        Edit.MACROS -> {
            var pr by rememberSaveable { mutableStateOf(s.targets.proteinG.toString()) }
            var c by rememberSaveable { mutableStateOf(s.targets.carbsG.toString()) }
            var f by rememberSaveable { mutableStateOf(s.targets.fatG.toString()) }
            val pv = pr.toIntOrNull(); val cv = c.toIntOrNull(); val fv = f.toIntOrNull()
            val ok = pv != null && cv != null && fv != null && pv in 0..600 && cv in 0..1000 && fv in 0..400
            MacrowDialog("Protein, carbs, fat", { edit = Edit.NONE }, "Save", {
                if (ok) scope.launch { repo.setTargets(Targets(s.targets.kcal, pv, cv, fv)); edit = Edit.NONE } else UiErrors.say("Enter whole grams, up to 600 protein, 1,000 carbs and 400 fat.")
            }) {
                MacrowField(pr, { pr = it.filter(Char::isDigit) }, "Protein", Modifier.fillMaxWidth(), KeyboardType.Number, suffix = "g", maxLength = 3)
                MacrowField(c, { c = it.filter(Char::isDigit) }, "Carbs", Modifier.fillMaxWidth(), KeyboardType.Number, suffix = "g", maxLength = 4)
                MacrowField(f, { f = it.filter(Char::isDigit) }, "Fat", Modifier.fillMaxWidth(), KeyboardType.Number, suffix = "g", maxLength = 3)
                if (ok) Text("That adds up to about ${"%,d".format(pv * 4 + cv * 4 + fv * 9)} kcal. Your calorie goal stays ${"%,d".format(s.targets.kcal)}.", style = MaterialTheme.typography.bodySmall, color = p.sub)
            }
        }
        Edit.WATER -> {
            var text by rememberSaveable { mutableStateOf(s.waterGoalMl.toString()) }
            val v = text.toIntOrNull()
            val err = if (v == null || v !in 500..10000) "500 to 10,000" else null
            MacrowDialog("Water goal", { edit = Edit.NONE }, "Save", {
                if (err == null && v != null) scope.launch { repo.setWaterGoal(v); edit = Edit.NONE }
            }) { MacrowField(text, { text = it.filter(Char::isDigit) }, "Daily water", Modifier.fillMaxWidth(), KeyboardType.Number, err, suffix = "ml", maxLength = 5) }
        }
        Edit.REMINDER_TIME -> MacrowDialog("Reminder time", { edit = Edit.NONE }, "Save", {
            scope.launch { repo.setReminder(true, reminderMinutes); Reminders.schedule(context, reminderMinutes); edit = Edit.NONE }
        }) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Stepper(timeText(reminderMinutes), { reminderMinutes = (reminderMinutes - 30 + 1440) % 1440 }, { reminderMinutes = (reminderMinutes + 30) % 1440 }, minusLabel = "30 minutes earlier", plusLabel = "30 minutes later")
            }
        }
        Edit.NONE -> {}
    }

    pendingRestore?.let { d ->
        MacrowDialog("Restore this backup?", { pendingRestore = null }, "Restore", {
            pendingRestore = null
            scope.launch {
                try { app.backup.restore(d); UiErrors.say("Backup restored") } catch (e: Exception) { UiErrors.report(e) }
            }
        }) {
            Text(
                "This replaces everything on this phone with the backup: ${d.entries.count { !it.planned }} logged foods, ${d.weights.size} weigh-ins and ${d.foods.count { it.source == "CUSTOM" || it.source == "RECIPE" }} of your own foods and recipes. " +
                    if (s.backupFolder != null) "A safety copy of what you have now is saved first." else "Choose a backup folder first if you want a safety copy of what you have now.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun Row2(title: String, value: String, onClick: () -> Unit) {
    val p = MaterialTheme.macrow
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).defaultMinSize(minHeight = 52.dp).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (value.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text(value, style = MaterialTheme.typography.labelLarge, color = p.sub)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = p.muted)
    }
}

private fun timeText(minutes: Int): String =
    LocalTime.of(minutes / 60, minutes % 60).format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))

private fun lastBackupText(at: Long): String {
    val t = java.time.Instant.ofEpochMilli(at).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
    val day = if (t.toLocalDate() == LocalDate.now()) "today" else t.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    return "$day, ${t.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))}"
}
