package com.macrow.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macrow.app.data.MacrowRepository
import com.macrow.app.domain.CoachInput
import com.macrow.app.domain.Eaten
import com.macrow.app.domain.Meal
import com.macrow.app.domain.coach
import com.macrow.app.ui.UiErrors
import com.macrow.app.ui.components.CoachCard
import com.macrow.app.ui.components.CoachSheet
import com.macrow.app.ui.components.LogSheet
import androidx.compose.runtime.remember
import com.macrow.app.domain.kcalText
import com.macrow.app.ui.LocalSnack
import com.macrow.app.ui.components.CappedFont
import com.macrow.app.ui.components.EntrySheet
import com.macrow.app.ui.components.GlassPill
import com.macrow.app.ui.components.HeroCard
import com.macrow.app.ui.components.LinearBar
import com.macrow.app.ui.components.MacroRow
import com.macrow.app.ui.components.MacrowCard
import com.macrow.app.ui.components.MealSection
import com.macrow.app.ui.components.ProgressRing
import com.macrow.app.ui.components.SoftButton
import com.macrow.app.ui.components.animatedKcal
import com.macrow.app.ui.macrowVm
import com.macrow.app.ui.DiaryVm
import com.macrow.app.ui.theme.Tone
import com.macrow.app.ui.theme.macrow
import com.macrow.app.ui.total
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "Today", "Yesterday" or "Tue 17 Nov". */
fun dayLabel(day: Long): String {
    val today = MacrowRepository.today()
    return when (day) {
        today -> "Today"
        today - 1 -> "Yesterday"
        today + 1 -> "Tomorrow"
        else -> LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))
    }
}

@Composable
fun DiaryScreen(onAddFood: (Meal, Long) -> Unit, onSettings: () -> Unit) {
    val vm = macrowVm { DiaryVm(it) }
    val day by vm.day.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()
    val water by vm.water.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()
    val pool by vm.pool.collectAsStateWithLifecycle()
    var coachOpen by rememberSaveable { mutableStateOf(false) }
    var suggestedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val snack = LocalSnack.current
    val scope = rememberCoroutineScope()
    val p = MaterialTheme.macrow
    val t = settings.targets
    val total = entries.total()
    val left = t.kcal - total.kcal
    val over = left < 0
    val isToday = day >= MacrowRepository.today()
    val review = remember(entries, settings, water, day, streak, pool) {
        coach(
            CoachInput(
                eaten = entries.map { Eaten(Meal.of(it.entry.meal), it.food.name, it.nutrients) },
                targets = t, goal = settings.profile.goal, sex = settings.profile.sex, waterMl = water, waterGoalMl = settings.waterGoalMl,
                isToday = isToday, hour = java.time.LocalTime.now().hour, streak = streak, pool = pool,
            ),
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ vm.shift(-1) }) { Icon(Icons.Rounded.ChevronLeft, "Previous day") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(dayLabel(day), style = MaterialTheme.typography.headlineSmall, maxLines = 1)
                if (!isToday) Text("Back to today", Modifier.clickable(role = Role.Button) { vm.goToday() }.padding(vertical = 10.dp), style = MaterialTheme.typography.labelMedium, color = p.green)
            }
            IconButton({ vm.shift(1) }, enabled = !isToday) { Icon(Icons.Rounded.ChevronRight, "Next day") }
            IconButton(onSettings) { Icon(Icons.Rounded.Settings, "Settings") }
        }
        Spacer(Modifier.height(8.dp))

        HeroCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(
                    fraction = if (t.kcal > 0) (total.kcal / t.kcal).toFloat() else 0f,
                    modifier = Modifier.size(112.dp), stroke = 12.dp,
                    color = if (over) Color(0xFFFF9DB8) else Color(0xFFFFD166),
                ) {
                    CappedFont(1.15f) { Column(Modifier.semantics(mergeDescendants = true) { contentDescription = "${kotlin.math.abs(left).toInt()} calories ${if (over) "over" else "left"} of ${t.kcal}" }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(animatedKcal(kotlin.math.abs(left).toInt()), style = MaterialTheme.typography.headlineMedium, color = Color.White, maxLines = 1)
                        Text(if (over) "kcal over" else "kcal left", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.9f))
                    } }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Eaten  ${total.kcal.kcalText()}", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text("Goal  ${t.kcal.toDouble().kcalText()}", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    if (streak > 0) GlassPill("$streak-day streak", icon = Icons.Rounded.LocalFireDepartment)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        CoachCard(review, { coachOpen = true }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))

        MacrowCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MacroRow("Protein", total.protein, t.proteinG.toDouble(), p.protein)
                MacroRow("Carbs", total.carbs, t.carbsG.toDouble(), p.carbs)
                MacroRow("Fat", total.fat, t.fatG.toDouble(), p.fat)
            }
        }
        Spacer(Modifier.height(12.dp))

        MacrowCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WaterDrop, null, Modifier.size(22.dp), tint = p.fat)
                Spacer(Modifier.width(8.dp))
                Text("Water", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text("${"%,d".format(water)} / ${"%,d".format(settings.waterGoalMl)} ml", style = MaterialTheme.typography.labelLarge, color = p.sub)
            }
            Spacer(Modifier.height(8.dp))
            LinearBar(if (settings.waterGoalMl > 0) water.toFloat() / settings.waterGoalMl else 0f, p.fat)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SoftButton("+250 ml", { vm.addWater(250) }, Modifier.weight(1f), tone = Tone.Blue)
                if (water > 0) SoftButton("Undo", { vm.undoWater() }, tone = Tone.Green, icon = Icons.AutoMirrored.Rounded.Undo)
            }
        }
        Spacer(Modifier.height(12.dp))

        Meal.entries.forEach { meal ->
            MealSection(meal, entries.filter { it.entry.meal == meal.name }, onAdd = { onAddFood(meal, day) }, onEdit = { editingId = it.entry.id })
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(112.dp))
    }

    if (coachOpen) CoachSheet(review, onSuggestion = { coachOpen = false; suggestedId = it.id }, onDismiss = { coachOpen = false })
    pool.firstOrNull { it.id == suggestedId }?.let { f ->
        LogSheet(
            f, Meal.forHour(java.time.LocalTime.now().hour), "Add to diary",
            onConfirm = { m, servings -> vm.add(day, m.name, f, servings); suggestedId = null; UiErrors.say("Added ${f.name} to ${m.label.lowercase()}") },
            onDismiss = { suggestedId = null },
        )
    }

    entries.firstOrNull { it.entry.id == editingId }?.let { e ->
        EntrySheet(
            e,
            onSave = { vm.update(it); editingId = null },
            onDelete = { entry ->
                editingId = null
                vm.delete(entry)
                scope.launch {
                    if (snack.showSnackbar("Removed ${e.food.name}", "Undo") == SnackbarResult.ActionPerformed) vm.restore(entry)
                }
            },
            onDismiss = { editingId = null },
        )
    }
}
