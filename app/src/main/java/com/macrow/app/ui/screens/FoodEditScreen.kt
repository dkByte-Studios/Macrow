package com.macrow.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.macrow.app.MacrowApp
import com.macrow.app.data.Food
import com.macrow.app.data.OpenFoodFacts
import com.macrow.app.data.Source
import com.macrow.app.domain.Limits
import com.macrow.app.domain.pretty
import com.macrow.app.ui.UiErrors
import com.macrow.app.ui.components.GradientButton
import com.macrow.app.ui.components.MacrowCard
import com.macrow.app.ui.components.MacrowDialog
import com.macrow.app.ui.components.MacrowField
import com.macrow.app.ui.components.SoftButton
import com.macrow.app.ui.components.TopBar
import com.macrow.app.ui.theme.Tone
import kotlinx.coroutines.launch

private fun String.num(): Double? = replace(',', '.').trim().toDoubleOrNull()?.takeIf { it >= 0 && it.isFinite() }

/** Create a food, or edit one (id 0 means new). Numbers are per serving. */
@Composable
fun FoodEditScreen(foodId: Long, barcode: String?, onBack: () -> Unit, onSaved: (Long) -> Unit) {
    val repo = (LocalContext.current.applicationContext as MacrowApp).repo
    val scope = rememberCoroutineScope()
    var loaded by rememberSaveable { mutableStateOf(foodId == 0L) }
    var original by remember { mutableStateOf<Food?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var serving by rememberSaveable { mutableStateOf("1 serving") }
    var grams by rememberSaveable { mutableStateOf("100") }
    var kcal by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carbs by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
    var fiber by rememberSaveable { mutableStateOf("") }
    var sugar by rememberSaveable { mutableStateOf("") }
    var sodium by rememberSaveable { mutableStateOf("") }
    var iron by rememberSaveable { mutableStateOf("") }
    var calcium by rememberSaveable { mutableStateOf("") }
    var vitC by rememberSaveable { mutableStateOf("") }
    var potassium by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf(barcode ?: "") }
    var more by rememberSaveable { mutableStateOf(false) }
    var tried by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(foodId) {
        if (foodId != 0L && !loaded) {
            repo.food(foodId)?.let { f ->
                original = f
                name = f.name; serving = f.servingLabel; grams = f.servingGrams.pretty(); kcal = f.kcal.pretty()
                protein = f.protein.pretty(); carbs = f.carbs.pretty(); fat = f.fat.pretty(); fiber = f.fiber.pretty(); sugar = f.sugar.pretty()
                sodium = f.sodiumMg.pretty(); iron = f.ironMg.pretty(); calcium = f.calciumMg.pretty(); vitC = f.vitCMg.pretty(); potassium = f.potassiumMg.pretty()
                code = f.barcode ?: ""
            }
            loaded = true
        }
    }
    // After a rotation the saved fields are back but the original food is not: fetch it just for its id and counters.
    LaunchedEffect(foodId, loaded) { if (foodId != 0L && original == null && loaded) original = repo.food(foodId) }

    fun err(text: String, max: Double = 1e6) = if (tried && (text.num() == null || text.num()!! > max)) "Enter a number" else null
    val nameErr = if (tried && name.isBlank()) "Give it a name" else null
    val gramsErr = if (tried && (grams.num() == null || grams.num()!! < 1 || grams.num()!! > Limits.MAX_GRAMS * 4)) "1 to 20,000" else null
    val kcalErr = if (tried && (kcal.num() == null || kcal.num()!! > Limits.MAX_KCAL_PER_SERVING * 4)) "0 to 20,000" else null
    val codeErr = if (tried && code.isNotEmpty() && !OpenFoodFacts.isBarcode(code)) "8 to 14 digits" else null

    fun save() {
        tried = true
        val ok = name.isNotBlank() && grams.num() != null && grams.num()!! in 1.0..20000.0 && kcal.num() != null && kcal.num()!! <= 20000 &&
            listOf(protein, carbs, fat, fiber, sugar, sodium, iron, calcium, vitC, potassium).all { it.isBlank() || it.num() != null } &&
            (code.isEmpty() || OpenFoodFacts.isBarcode(code))
        if (!ok) { UiErrors.say("Check the highlighted fields."); return }
        val base = original ?: Food(name = name, source = Source.CUSTOM)
        val food = base.copy(
            id = foodId, name = name, servingLabel = serving, servingGrams = grams.num()!!, kcal = kcal.num()!!,
            protein = protein.num() ?: 0.0, carbs = carbs.num() ?: 0.0, fat = fat.num() ?: 0.0, fiber = fiber.num() ?: 0.0, sugar = sugar.num() ?: 0.0,
            sodiumMg = sodium.num() ?: 0.0, ironMg = iron.num() ?: 0.0, calciumMg = calcium.num() ?: 0.0, vitCMg = vitC.num() ?: 0.0, potassiumMg = potassium.num() ?: 0.0,
            barcode = code.ifEmpty { null },
        )
        scope.launch {
            try { onSaved(repo.saveFood(food)) } catch (e: Exception) { UiErrors.report(e) }
        }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar(if (foodId == 0L) "Create food" else "Edit food", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MacrowField(name, { name = it }, "Name", Modifier.fillMaxWidth(), error = nameErr)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MacrowField(serving, { serving = it }, "Serving", Modifier.weight(1f), maxLength = 30)
                MacrowField(grams, { grams = it }, "Weight", Modifier.weight(1f), keyboard = KeyboardType.Decimal, suffix = "g", error = gramsErr, maxLength = 7)
            }
            Text("Per serving", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MacrowField(kcal, { kcal = it }, "Calories", Modifier.fillMaxWidth(), keyboard = KeyboardType.Decimal, suffix = "kcal", error = kcalErr, maxLength = 7)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MacrowField(protein, { protein = it }, "Protein", Modifier.weight(1f), keyboard = KeyboardType.Decimal, suffix = "g", error = err(protein), maxLength = 6)
                MacrowField(carbs, { carbs = it }, "Carbs", Modifier.weight(1f), keyboard = KeyboardType.Decimal, suffix = "g", error = err(carbs), maxLength = 6)
                MacrowField(fat, { fat = it }, "Fat", Modifier.weight(1f), keyboard = KeyboardType.Decimal, suffix = "g", error = err(fat), maxLength = 6)
            }
            SoftButton(if (more) "Hide vitamins and minerals" else "Add vitamins and minerals", { more = !more }, Modifier.fillMaxWidth(), icon = if (more) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore)
            AnimatedVisibility(more) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MacrowField(fiber, { fiber = it }, "Fiber", Modifier.weight(1f), keyboard = KeyboardType.Decimal, suffix = "g", error = err(fiber), maxLength = 6)
                        MacrowField(sugar, { sugar = it }, "Sugar", Modifier.weight(1f), keyboard = KeyboardType.Decimal, suffix = "g", error = err(sugar), maxLength = 6)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MacrowField(sodium, { sodium = it }, "Sodium", Modifier.weight(1f), keyboard = KeyboardType.Decimal, suffix = "mg", error = err(sodium), maxLength = 7)
                        MacrowField(potassium, { potassium = it }, "Potassium", Modifier.weight(1f), keyboard = KeyboardType.Decimal, suffix = "mg", error = err(potassium), maxLength = 7)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MacrowField(iron, { iron = it }, "Iron", Modifier.weight(1f), keyboard = KeyboardType.Decimal, suffix = "mg", error = err(iron), maxLength = 6)
                        MacrowField(calcium, { calcium = it }, "Calcium", Modifier.weight(1f), keyboard = KeyboardType.Decimal, suffix = "mg", error = err(calcium), maxLength = 7)
                    }
                    MacrowField(vitC, { vitC = it }, "Vitamin C", Modifier.fillMaxWidth(), keyboard = KeyboardType.Decimal, suffix = "mg", error = err(vitC), maxLength = 6)
                }
            }
            MacrowField(code, { code = it.filter(Char::isDigit) }, "Barcode (optional)", Modifier.fillMaxWidth(), keyboard = KeyboardType.Number, error = codeErr, ime = ImeAction.Done, maxLength = 14)
            if (foodId != 0L) SoftButton("Delete this food", { confirmDelete = true }, Modifier.fillMaxWidth(), tone = Tone.Berry)
            Spacer(Modifier.height(8.dp))
        }
        GradientButton("Save food", ::save, Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp))
    }

    if (confirmDelete) {
        MacrowDialog(
            "Delete this food?", { confirmDelete = false }, "Delete",
            onConfirm = {
                confirmDelete = false
                scope.launch { runCatching { repo.archiveFood(foodId) }.onSuccess { UiErrors.say("Food deleted"); onBack() }.onFailure { UiErrors.report(it) } }
            },
        ) {
            Text("It disappears from search. Meals you already logged with it keep their numbers.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
