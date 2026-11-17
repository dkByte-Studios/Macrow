package com.macrow.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.DinnerDining
import androidx.compose.material.icons.rounded.RamenDining
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.macrow.app.data.Entry
import com.macrow.app.data.EntryWithFood
import com.macrow.app.data.Food
import com.macrow.app.domain.Limits
import com.macrow.app.domain.Meal
import com.macrow.app.domain.Nutrients
import com.macrow.app.domain.kcalText
import com.macrow.app.domain.pretty
import com.macrow.app.domain.servingsText
import com.macrow.app.ui.theme.Tone
import com.macrow.app.ui.theme.macrow
import kotlin.math.roundToInt

fun Meal.icon(): ImageVector = when (this) {
    Meal.BREAKFAST -> Icons.Rounded.WbSunny
    Meal.LUNCH -> Icons.Rounded.RamenDining
    Meal.DINNER -> Icons.Rounded.DinnerDining
    Meal.SNACK -> Icons.Rounded.Cookie
}

fun Meal.tone(): Tone = when (this) {
    Meal.BREAKFAST -> Tone.Gold
    Meal.LUNCH -> Tone.Green
    Meal.DINNER -> Tone.Blue
    Meal.SNACK -> Tone.Berry
}

/** A number that counts up or down to its new value instead of jumping. */
@Composable
fun animatedKcal(value: Int): String {
    val v by animateIntAsState(value, tween(700), label = "kcal")
    return "%,d".format(v)
}

/** One macro: name, a bar in the macro colour, and "have / goal g". */
@Composable
fun MacroRow(label: String, have: Double, goal: Double, color: Color, modifier: Modifier = Modifier) {
    val p = MaterialTheme.macrow
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text("${have.roundToInt()} / ${goal.roundToInt()} g", style = MaterialTheme.typography.labelMedium, color = p.sub)
        }
        Spacer(Modifier.height(4.dp))
        LinearBar(if (goal > 0) (have / goal).toFloat() else 0f, color)
    }
}

@Composable
fun MacroChips(n: Nutrients, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Pill("P ${n.protein.pretty()} g", Tone.Berry)
        Pill("C ${n.carbs.pretty()} g", Tone.Gold)
        Pill("F ${n.fat.pretty()} g", Tone.Blue)
    }
}

/** A meal's header (icon, name, calories, add button) and its logged foods. */
@Composable
fun MealSection(
    meal: Meal,
    entries: List<EntryWithFood>,
    onAdd: () -> Unit,
    onEdit: (EntryWithFood) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = MaterialTheme.macrow
    val kcal = entries.sumOf { it.nutrients.kcal }
    MacrowCard(modifier, padding = 12.dp) {
      Column(Modifier.animateContentSize(tween(240))) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconTile(meal.icon(), meal.tone(), size = 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(meal.label, style = MaterialTheme.typography.titleMedium)
                Text(if (entries.isEmpty()) "Nothing yet" else "${kcal.kcalText()} kcal", style = MaterialTheme.typography.bodySmall, color = p.sub)
            }
            IconHit(onAdd, "Add food to ${meal.label}") { Icon(Icons.Rounded.Add, null, tint = p.green) }
        }
        entries.forEach { e ->
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.Button) { onEdit(e) }.defaultMinSize(minHeight = 48.dp).padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(e.food.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${e.entry.servings.servingsText()} × ${e.food.servingLabel}", style = MaterialTheme.typography.bodySmall, color = p.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(10.dp))
                Text(e.nutrients.kcal.kcalText(), style = MaterialTheme.typography.titleSmall)
            }
        }
      }
    }
}

@Composable
private fun IconHit(onClick: () -> Unit, label: String, content: @Composable () -> Unit) {
    Box(
        Modifier.size(48.dp).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** How to type the amount: in servings of the food, or in grams. */
private enum class AmountUnit { SERVINGS, GRAMS }

/**
 * Sheet to log or plan one portion of [food]: amount, meal, and the numbers it adds up to before you save.
 * [confirmLabel] reads "Add to diary" or "Add to plan".
 */
@Composable
fun LogSheet(food: Food, initialMeal: Meal, confirmLabel: String, onConfirm: (Meal, Double) -> Unit, onDismiss: () -> Unit) {
    val p = MaterialTheme.macrow
    var meal by rememberSaveable { mutableStateOf(initialMeal) }
    var unit by rememberSaveable { mutableStateOf(AmountUnit.SERVINGS) }
    var text by rememberSaveable { mutableStateOf("1") }
    val amount = text.replace(',', '.').toDoubleOrNull()
    val servings = amount?.let { if (unit == AmountUnit.GRAMS) it / food.servingGrams else it }
    val error = when {
        amount == null || amount <= 0 -> "Enter an amount above 0"
        unit == AmountUnit.GRAMS && amount > Limits.MAX_GRAMS -> "That's a lot. Enter ${Limits.MAX_GRAMS.toInt()} g or less"
        unit == AmountUnit.SERVINGS && amount > Limits.MAX_SERVINGS -> "Enter ${Limits.MAX_SERVINGS.toInt()} servings or fewer"
        else -> null
    }
    val shown = (food.nutrients * (servings?.takeIf { error == null } ?: 0.0))

    MacrowSheet(onDismiss) {
        Text(food.name, style = MaterialTheme.typography.headlineSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
        val sub = listOfNotNull(food.brand.ifBlank { null }, if (food.servingLabel.endsWith(" g")) food.servingLabel else "${food.servingLabel} = ${food.servingGrams.pretty()} g").joinToString(" · ")
        Text(sub, style = MaterialTheme.typography.bodyMedium, color = p.sub)
        Spacer(Modifier.height(12.dp))
        Segmented(listOf(AmountUnit.SERVINGS to "Servings", AmountUnit.GRAMS to "Grams"), unit, onSelect = {
            if (it != unit) {
                // Keep the same portion when switching, so 1.5 servings becomes the matching grams.
                val s = servings
                if (s != null && error == null) text = if (it == AmountUnit.GRAMS) (s * food.servingGrams).pretty() else s.servingsText()
                unit = it
            }
        })
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MacrowField(
                text, { text = it }, if (unit == AmountUnit.GRAMS) "Amount in grams" else "Number of servings", Modifier.weight(1f),
                keyboard = KeyboardType.Decimal, error = error, maxLength = 7,
            )
        }
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Meal.entries.forEach { m -> ChoiceChip(m.label, meal == m, { meal = m }) }
        }
        Spacer(Modifier.height(14.dp))
        MacrowCard(Modifier.fillMaxWidth()) {
            Text("${shown.kcal.kcalText()} kcal", style = MaterialTheme.typography.headlineMedium, color = p.green)
            Spacer(Modifier.height(6.dp))
            MacroChips(shown)
        }
        Spacer(Modifier.height(14.dp))
        GradientButton(confirmLabel, { if (error == null && servings != null) onConfirm(meal, servings) }, Modifier.fillMaxWidth())
    }
}

/** Sheet to change or delete something you already logged or planned. */
@Composable
fun EntrySheet(e: EntryWithFood, onSave: (Entry) -> Unit, onDelete: (Entry) -> Unit, onDismiss: () -> Unit) {
    val p = MaterialTheme.macrow
    var servings by rememberSaveable { mutableDoubleStateOf(e.entry.servings) }
    var meal by rememberSaveable { mutableStateOf(Meal.of(e.entry.meal)) }
    val shown = e.food.nutrients * servings
    MacrowSheet(onDismiss) {
        Text(e.food.name, style = MaterialTheme.typography.headlineSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Text(e.food.servingLabel, style = MaterialTheme.typography.bodyMedium, color = p.sub)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Servings", style = MaterialTheme.typography.titleMedium)
            val step = if (servings <= 1.0) 0.25 else 0.5
            Stepper(servings.servingsText(), { servings = (servings - step).coerceAtLeast(0.25) }, { servings = (servings + step).coerceAtMost(Limits.MAX_SERVINGS) }, minusLabel = "Fewer servings", plusLabel = "More servings")
        }
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Meal.entries.forEach { m -> ChoiceChip(m.label, meal == m, { meal = m }) }
        }
        Spacer(Modifier.height(14.dp))
        MacrowCard(Modifier.fillMaxWidth()) {
            Text("${shown.kcal.kcalText()} kcal", style = MaterialTheme.typography.headlineMedium, color = p.green)
            Spacer(Modifier.height(6.dp))
            MacroChips(shown)
        }
        Spacer(Modifier.height(14.dp))
        GradientButton("Save changes", { onSave(e.entry.copy(servings = servings, meal = meal.name)) }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        SoftButton("Delete", { onDelete(e.entry) }, Modifier.fillMaxWidth(), tone = Tone.Berry)
    }
}
