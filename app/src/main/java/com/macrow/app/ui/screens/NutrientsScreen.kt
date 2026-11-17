package com.macrow.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macrow.app.data.MacrowRepository
import com.macrow.app.domain.MicroGoals
import com.macrow.app.domain.Nutrients
import com.macrow.app.domain.pretty
import com.macrow.app.ui.NutrientsVm
import com.macrow.app.ui.components.EmptyState
import com.macrow.app.ui.components.LinearBar
import com.macrow.app.ui.components.MacrowCard
import com.macrow.app.ui.components.Segmented
import com.macrow.app.ui.macrowVm
import com.macrow.app.ui.theme.macrow
import com.macrow.app.ui.total

private enum class Span { DAY, WEEK }

@Composable
fun NutrientsScreen() {
    val vm = macrowVm { NutrientsVm(it) }
    val p = MaterialTheme.macrow
    val settings by vm.settings.collectAsStateWithLifecycle()
    val todayEntries by vm.today.collectAsStateWithLifecycle()
    val weekEntries by vm.week.collectAsStateWithLifecycle()
    var span by rememberSaveable { mutableStateOf(Span.DAY) }
    val entries = if (span == Span.DAY) todayEntries else weekEntries
    // The week shows the average on the days you actually logged, so a missed day doesn't look like fasting.
    val days = if (span == Span.DAY) 1 else weekEntries.map { it.entry.day }.distinct().size.coerceAtLeast(1)
    val n = entries.total() * (1.0 / days)
    val goals = MicroGoals.forSex(settings.profile.sex)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp)) {
        Text("Nutrients", Modifier.padding(top = 16.dp, start = 8.dp), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Segmented(listOf(Span.DAY to "Today", Span.WEEK to "Week average"), span, { span = it })
        Spacer(Modifier.height(12.dp))

        if (entries.isEmpty()) {
            EmptyState(Icons.Rounded.Eco, "Nothing to add up yet", "Log a meal in your diary and the vitamins and minerals show up here.")
        } else {
            MacrowCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Goal("Fiber", n.fiber, goals.fiber, "g")
                    Goal("Iron", n.ironMg, goals.iron, "mg")
                    Goal("Calcium", n.calciumMg, goals.calcium, "mg")
                    Goal("Vitamin C", n.vitCMg, goals.vitC, "mg")
                    Goal("Potassium", n.potassiumMg, goals.potassium, "mg")
                    Limit("Sodium", n.sodiumMg, goals.sodiumMax, "mg")
                    Limit("Sugar", n.sugar, goals.sugarMax, "g")
                }
            }
            Spacer(Modifier.height(12.dp))
            val top = entries.groupBy { it.food.name }.mapValues { (_, v) -> v.sumOf { it.nutrients.kcal } }.entries.sortedByDescending { it.value }.take(3).map { it.key }
            Text("Biggest sources: ${top.joinToString(", ")}.", style = MaterialTheme.typography.bodyMedium, color = p.sub)
        }
        Spacer(Modifier.height(10.dp))
        Text("Goals are general guides for your sex, not medical advice. Your own needs may differ.", style = MaterialTheme.typography.bodySmall, color = p.muted)
        Spacer(Modifier.height(112.dp))
    }
}

/** A nutrient you want to reach: green once you have all of it, gold from halfway, berry below that. */
@Composable
private fun Goal(label: String, have: Double, goal: Double, unit: String) {
    val p = MaterialTheme.macrow
    val r = if (goal > 0) have / goal else 0.0
    Row2(label, "${have.pretty()} / ${goal.pretty()} $unit", r.toFloat(), if (r >= 1.0) p.good else if (r >= 0.5) p.carbs else p.protein)
}

/** A nutrient you want to stay under: green while under 90 percent, gold near the limit, berry over it. */
@Composable
private fun Limit(label: String, have: Double, max: Double, unit: String) {
    val p = MaterialTheme.macrow
    val r = if (max > 0) have / max else 0.0
    Row2("$label (limit)", "${have.pretty()} / ${max.pretty()} $unit", r.toFloat(), if (r > 1.0) p.bad else if (r > 0.9) p.carbs else p.good)
}

@Composable
private fun Row2(label: String, value: String, fraction: Float, color: Color) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.macrow.sub)
        }
        Spacer(Modifier.height(4.dp))
        LinearBar(fraction, color)
    }
}
