package com.macrow.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macrow.app.data.MacrowRepository
import com.macrow.app.domain.MicroGoals
import com.macrow.app.domain.Meal
import com.macrow.app.domain.Status
import com.macrow.app.domain.kcalText
import com.macrow.app.domain.statusOf
import com.macrow.app.ui.LocalSnack
import com.macrow.app.ui.PlanVm
import com.macrow.app.ui.UiErrors
import com.macrow.app.ui.components.EntrySheet
import com.macrow.app.ui.components.GradientButton
import com.macrow.app.ui.components.LinearBar
import com.macrow.app.ui.components.MacrowCard
import com.macrow.app.ui.components.MealSection
import com.macrow.app.ui.components.Pill
import com.macrow.app.ui.macrowVm
import com.macrow.app.ui.theme.Tone
import com.macrow.app.ui.theme.macrow
import com.macrow.app.ui.total
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun PlanScreen(onAddFood: (Meal, Long) -> Unit) {
    val vm = macrowVm { PlanVm(it) }
    val p = MaterialTheme.macrow
    val day by vm.day.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val planned by vm.planned.collectAsStateWithLifecycle()
    val week by vm.week.collectAsStateWithLifecycle()
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val snack = LocalSnack.current
    val scope = rememberCoroutineScope()
    val start = PlanVm.weekStart(day)
    val t = settings.targets
    val total = planned.total()
    val goals = MicroGoals.forSex(settings.profile.sex)
    val locale = LocalLocale.current.platformLocale

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ vm.shiftWeek(-1) }) { Icon(Icons.Rounded.ChevronLeft, "Previous week") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Plan", style = MaterialTheme.typography.headlineSmall)
                val a = LocalDate.ofEpochDay(start); val b = a.plusDays(6)
                Text("${a.dayOfMonth} ${a.month.getDisplayName(TextStyle.SHORT, locale)} to ${b.dayOfMonth} ${b.month.getDisplayName(TextStyle.SHORT, locale)}", style = MaterialTheme.typography.labelMedium, color = p.sub)
            }
            IconButton({ vm.shiftWeek(1) }) { Icon(Icons.Rounded.ChevronRight, "Next week") }
        }
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            for (i in 0..6) {
                val d = start + i
                val sel = d == day
                val has = week.any { it.entry.day == d }
                val bg by animateColorAsState(if (sel) p.green else p.card, tween(200), label = "dayBg")
                val fg by animateColorAsState(if (sel) Color.White else p.ink, tween(200), label = "dayFg")
                val date = LocalDate.ofEpochDay(d)
                Column(
                    Modifier.weight(1f).heightIn(min = 56.dp).clip(RoundedCornerShape(16.dp)).background(bg).selectable(sel, role = Role.Tab) { vm.select(d) }.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale), style = MaterialTheme.typography.labelMedium, color = fg)
                    Text("${date.dayOfMonth}", style = MaterialTheme.typography.titleSmall, color = fg)
                    Box(Modifier.padding(top = 3.dp).size(5.dp).clip(CircleShape).background(if (has) (if (sel) Color.White else p.green) else Color.Transparent))
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        val kStatus = statusOf(total.kcal, t.kcal.toDouble())
        val empty = planned.isEmpty()
        MacrowCard {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                Text("Plan vs target", style = MaterialTheme.typography.titleMedium)
                if (!empty) when (kStatus) {
                    Status.ON -> Pill("On target", Tone.Green, icon = Icons.Rounded.CheckCircle)
                    Status.UNDER -> Pill("${abs(t.kcal - total.kcal).roundToInt()} kcal under", Tone.Gold)
                    Status.OVER -> Pill("${abs(total.kcal - t.kcal).roundToInt()} kcal over", Tone.Berry)
                }
            }
            Spacer(Modifier.height(10.dp))
            PlanBar("Calories", "${total.kcal.kcalText()} / ${t.kcal.toDouble().kcalText()}", total.kcal / t.kcal, if (kStatus == Status.OVER) p.bad else p.good)
            Spacer(Modifier.height(8.dp))
            PlanBar("Protein", "${total.protein.roundToInt()} / ${t.proteinG} g", total.protein / t.proteinG.coerceAtLeast(1), p.protein)
            if (!empty && total.fiber < goals.fiber * 0.8) {
                Spacer(Modifier.height(10.dp))
                Pill("Fiber is low: ${total.fiber.roundToInt()} of ${goals.fiber.roundToInt()} g", Tone.Gold, icon = Icons.Rounded.Warning)
            }
            if (empty) {
                Spacer(Modifier.height(8.dp))
                Text("Nothing planned for this day yet. Add meals below and the app checks them against your targets.", style = MaterialTheme.typography.bodyMedium, color = p.sub)
            }
        }
        Spacer(Modifier.height(12.dp))

        Meal.entries.forEach { meal ->
            MealSection(meal, planned.filter { it.entry.meal == meal.name }, onAdd = { onAddFood(meal, day) }, onEdit = { editingId = it.entry.id })
            Spacer(Modifier.height(10.dp))
        }
        if (!empty) {
            GradientButton(
                "Log this day in my diary",
                { vm.logDay { UiErrors.say("Added ${planned.size} foods to your diary for ${dayLabel(day).lowercase()}") } },
                Modifier.fillMaxWidth(), icon = Icons.Rounded.EventAvailable,
            )
        }
        Spacer(Modifier.height(112.dp))
    }

    planned.firstOrNull { it.entry.id == editingId }?.let { e ->
        EntrySheet(
            e,
            onSave = { vm.update(it); editingId = null },
            onDelete = { entry ->
                editingId = null
                vm.delete(entry)
                scope.launch { if (snack.showSnackbar("Removed ${e.food.name}", "Undo") == SnackbarResult.ActionPerformed) vm.restore(entry) }
            },
            onDismiss = { editingId = null },
        )
    }
}

@Composable
private fun PlanBar(label: String, value: String, fraction: Double, color: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.macrow.sub)
    }
    Spacer(Modifier.height(4.dp))
    LinearBar(fraction.toFloat(), color)
}
