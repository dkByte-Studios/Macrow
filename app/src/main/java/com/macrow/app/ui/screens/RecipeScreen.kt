package com.macrow.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.macrow.app.data.Food
import com.macrow.app.data.MacrowRepository
import com.macrow.app.domain.Nutrients
import com.macrow.app.domain.kcalText
import com.macrow.app.domain.pretty
import com.macrow.app.domain.servingsText
import com.macrow.app.ui.UiErrors
import com.macrow.app.ui.components.GradientButton
import com.macrow.app.ui.components.HeroCard
import com.macrow.app.ui.components.MacroChips
import com.macrow.app.ui.components.MacrowCard
import com.macrow.app.ui.components.MacrowField
import com.macrow.app.ui.components.MacrowSheet
import com.macrow.app.ui.components.SoftButton
import com.macrow.app.ui.components.Stepper
import com.macrow.app.ui.components.TopBar
import com.macrow.app.ui.macrowVm
import com.macrow.app.ui.safeLaunch
import com.macrow.app.ui.theme.Tone
import com.macrow.app.ui.theme.macrow
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The recipe being edited. Lives in a ViewModel so a rotation keeps the half-built list. */
class RecipeVm(private val repo: MacrowRepository, private val foodId: Long) : ViewModel() {
    val name = MutableStateFlow("")
    val servings = MutableStateFlow(1)
    val items = MutableStateFlow<List<Pair<Food, Double>>>(emptyList())
    val pickerQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val pickable = pickerQuery.flatMapLatest { repo.foods(it, "ALL") }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Food>())

    init {
        if (foodId != 0L) viewModelScope.launch(safeLaunch) {
            val f = repo.food(foodId) ?: return@launch
            name.value = f.name
            servings.value = repo.recipe(foodId)?.servings ?: 1
            items.value = repo.recipeItems(foodId).map { it.food to it.item.servings }
        }
    }

    fun add(f: Food) = items.update { list -> if (list.any { it.first.id == f.id }) list else list + (f to 1.0) }
    fun setAmount(f: Food, v: Double) = items.update { list -> list.map { if (it.first.id == f.id) f to v.coerceIn(0.25, 99.0) else it } }
    fun remove(f: Food) = items.update { list -> list.filter { it.first.id != f.id } }

    fun save(onSaved: (Long) -> Unit) = viewModelScope.launch(safeLaunch) {
        onSaved(repo.saveRecipe(foodId, name.value, servings.value, items.value))
    }
}

@Composable
fun RecipeEditScreen(foodId: Long, onBack: () -> Unit, onSaved: (Long) -> Unit) {
    val vm = macrowVm(key = "recipe$foodId") { RecipeVm(it, foodId) }
    val p = MaterialTheme.macrow
    val name by vm.name.collectAsStateWithLifecycle()
    val servings by vm.servings.collectAsStateWithLifecycle()
    val items by vm.items.collectAsStateWithLifecycle()
    var picking by rememberSaveable { mutableStateOf(false) }
    var tried by rememberSaveable { mutableStateOf(false) }
    val total = items.fold(Nutrients.ZERO) { a, (f, q) -> a + f.nutrients * q }
    val per = total * (1.0 / servings)

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar(if (foodId == 0L) "New recipe" else "Edit recipe", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MacrowField(name, { vm.name.value = it }, "Recipe name", Modifier.fillMaxWidth(), error = if (tried && name.isBlank()) "Give it a name" else null)
            MacrowCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Makes", style = MaterialTheme.typography.titleMedium)
                    Stepper("$servings ${if (servings == 1) "serving" else "servings"}", { vm.servings.update { (it - 1).coerceAtLeast(1) } }, { vm.servings.update { (it + 1).coerceAtMost(99) } }, minusLabel = "Fewer servings", plusLabel = "More servings")
                }
            }
            Text("Ingredients", style = MaterialTheme.typography.titleSmall, color = p.sub)
            if (items.isEmpty()) {
                Text("Add what goes in the pot. Amounts are in servings of each food.", style = MaterialTheme.typography.bodyMedium, color = p.sub)
            }
            items.forEach { (f, q) ->
                MacrowCard(Modifier.animateContentSize(tween(220)), padding = 12.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(f.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${q.servingsText()} × ${f.servingLabel}, ${(f.kcal * q).kcalText()} kcal", style = MaterialTheme.typography.bodySmall, color = p.sub)
                        }
                        IconButton({ vm.remove(f) }, Modifier.size(48.dp)) { Icon(Icons.Rounded.Close, "Remove ${f.name}", tint = p.muted) }
                    }
                    Spacer(Modifier.height(6.dp))
                    val step = if (q < 1.0) 0.25 else 0.5
                    Stepper(q.servingsText(), { vm.setAmount(f, q - step) }, { vm.setAmount(f, q + step) }, minusLabel = "Less ${f.name}", plusLabel = "More ${f.name}")
                }
            }
            SoftButton("Add ingredient", { picking = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
            HeroCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Per serving", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.9f))
                    Text("${per.kcal.kcalText()} kcal", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text("Protein ${per.protein.pretty()} g, carbs ${per.carbs.pretty()} g, fat ${per.fat.pretty()} g", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.92f))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        GradientButton(
            "Save recipe",
            {
                tried = true
                when {
                    name.isBlank() -> UiErrors.say("Give the recipe a name.")
                    items.isEmpty() -> UiErrors.say("Add at least one ingredient.")
                    else -> vm.save(onSaved)
                }
            },
            Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
        )
    }

    if (picking) {
        val query by vm.pickerQuery.collectAsStateWithLifecycle()
        val foods by vm.pickable.collectAsStateWithLifecycle()
        MacrowSheet({ picking = false; vm.pickerQuery.value = "" }) {
            Text("Add ingredient", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            MacrowField(query, { vm.pickerQuery.value = it }, "Search your foods", Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Column(Modifier.height(320.dp).verticalScroll(rememberScrollState())) {
                if (foods.isEmpty()) Text("No match. Create the food first, then come back.", style = MaterialTheme.typography.bodyMedium, color = p.sub, modifier = Modifier.padding(8.dp))
                foods.filter { !it.isRecipe }.forEach { f ->
                    Row(
                        Modifier.fillMaxWidth().clickable(role = Role.Button) { vm.add(f); picking = false; vm.pickerQuery.value = "" }.defaultMinSize(minHeight = 52.dp).padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(f.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(f.servingLabel, style = MaterialTheme.typography.bodySmall, color = p.sub)
                        }
                        Text("${f.kcal.kcalText()} kcal", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
