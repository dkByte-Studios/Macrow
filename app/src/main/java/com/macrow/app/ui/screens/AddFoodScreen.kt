package com.macrow.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macrow.app.data.Food
import com.macrow.app.data.Source
import com.macrow.app.domain.Meal
import com.macrow.app.domain.kcalText
import com.macrow.app.ui.AddFoodVm
import com.macrow.app.ui.Online
import com.macrow.app.ui.UiErrors
import com.macrow.app.ui.components.ChoiceChip
import com.macrow.app.ui.components.EmptyState
import com.macrow.app.ui.components.LogSheet
import com.macrow.app.ui.components.MacrowCard
import com.macrow.app.ui.components.Pill
import com.macrow.app.ui.components.SoftButton
import com.macrow.app.ui.components.TopBar
import com.macrow.app.ui.macrowVm
import com.macrow.app.ui.theme.Tone
import com.macrow.app.ui.theme.macrow
import kotlinx.coroutines.launch

private val filters = listOf("ALL" to "All", "RECENT" to "Recent", "MINE" to "My foods", "RECIPES" to "Recipes")

@Composable
fun AddFoodScreen(
    planned: Boolean,
    meal: Meal,
    day: Long,
    newFoodId: Long,
    onNewFoodHandled: () -> Unit,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onCreateFood: () -> Unit,
    onNewRecipe: () -> Unit,
    onEdit: (Long, Boolean) -> Unit,
) {
    val vm = macrowVm { AddFoodVm(it) }
    val p = MaterialTheme.macrow
    val query by vm.query.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val foods by vm.foods.collectAsStateWithLifecycle()
    val online by vm.online.collectAsStateWithLifecycle()
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selected by remember { mutableStateOf<Food?>(null) }
    val scope = rememberCoroutineScope()

    // After a rotation only the id survives; load the food again so the sheet comes back.
    LaunchedEffect(selectedId) { selected = selectedId?.let { vm.load(it) } }
    // A food or recipe you just created comes back here and opens its log sheet straight away.
    LaunchedEffect(newFoodId) { if (newFoodId > 0) { selectedId = newFoodId; onNewFoodHandled() } }

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar(if (planned) "Plan ${meal.label.lowercase()}" else "Add to ${meal.label.lowercase()}", onBack = onBack)
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { vm.setQuery(it.take(60)) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search foods") },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = p.green) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.searchOnline() }),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = p.card, unfocusedContainerColor = p.card,
                    focusedIndicatorColor = p.green, unfocusedIndicatorColor = p.muted.copy(alpha = 0.4f), cursorColor = p.green,
                ),
            )
            IconButton(onScan, Modifier.size(52.dp)) { Icon(Icons.Rounded.QrCodeScanner, "Scan a barcode", tint = p.green) }
        }
        LazyRow(Modifier.padding(vertical = 10.dp), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters) { (key, label) -> ChoiceChip(label, filter == key, { vm.setFilter(key) }) }
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoftButton("Create food", onCreateFood, Modifier.weight(1f), icon = Icons.Rounded.Add)
                    SoftButton("New recipe", onNewRecipe, Modifier.weight(1f), tone = Tone.Gold, icon = Icons.Rounded.Restaurant)
                }
            }
            if (foods.isEmpty() && online !is Online.Done) {
                item {
                    EmptyState(
                        Icons.Rounded.Search,
                        if (filter == "ALL") "No match on your phone" else "Nothing here yet",
                        when (filter) {
                            "RECENT" -> "Foods you log show up here so you can add them again in one tap."
                            "MINE" -> "Foods and recipes you create show up here."
                            "RECIPES" -> "Make a recipe once, then log a serving of it any time."
                            else -> "Try another word, search online, or create the food yourself."
                        },
                    )
                }
            }
            items(foods, key = { it.id }) { f ->
                FoodRow(f, onClick = { selectedId = f.id }, onEdit = if (f.source == Source.CUSTOM || f.source == Source.RECIPE || f.source == Source.BUILTIN || f.source == Source.ONLINE) ({ onEdit(f.id, f.isRecipe) }) else null)
            }
            if (query.trim().length >= 2) {
                item("online") {
                    MacrowCard(padding = 12.dp) {
                      Column(Modifier.animateContentSize(tween(220))) {
                        when (val o = online) {
                            Online.Idle -> Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { vm.searchOnline() }.defaultMinSize(minHeight = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CloudQueue, null, tint = p.green)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Search online for “${query.trim()}”", style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text("Open Food Facts. Only the words you typed are sent.", style = MaterialTheme.typography.bodySmall, color = p.sub)
                                }
                            }
                            Online.Loading -> Text("Searching Open Food Facts…", style = MaterialTheme.typography.bodyMedium, color = p.sub, modifier = Modifier.padding(vertical = 8.dp))
                            is Online.Failed -> Column {
                                Text(o.message, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(8.dp))
                                SoftButton("Try again", { vm.searchOnline() })
                            }
                            is Online.Done -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(if (o.foods.isEmpty()) "No online matches." else "From Open Food Facts", style = MaterialTheme.typography.titleSmall, color = p.sub)
                                o.foods.forEach { f ->
                                    FoodRow(f, flat = true, onClick = { scope.launch { runCatching { vm.keep(f) }.onSuccess { selectedId = it.id }.onFailure { UiErrors.report(it) } } }, onEdit = null)
                                }
                            }
                        }
                      }
                    }
                }
            }
        }
    }

    selected?.let { f ->
        LogSheet(
            f, meal, if (planned) "Add to plan" else "Add to diary",
            onConfirm = { m, servings ->
                vm.log(day, m.name, f, servings, planned)
                UiErrors.say("Added ${f.name} to ${m.label.lowercase()}")
                selectedId = null
            },
            onDismiss = { selectedId = null },
        )
    }
}

@Composable
fun FoodRow(f: Food, onClick: () -> Unit, onEdit: (() -> Unit)?, flat: Boolean = false) {
    val p = MaterialTheme.macrow
    val row: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(f.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        listOfNotNull(f.brand.ifBlank { null }, f.servingLabel).joinToString(", "),
                        style = MaterialTheme.typography.bodySmall, color = p.sub, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                    )
                    when (f.source) {
                        Source.BUILTIN -> Pill("Built in", Tone.Green)
                        Source.ONLINE -> Pill("Online", Tone.Blue)
                        Source.RECIPE -> Pill("Recipe", Tone.Gold)
                        else -> Pill("Yours", Tone.Berry)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(f.kcal.kcalText(), style = MaterialTheme.typography.titleSmall)
                Text("kcal", style = MaterialTheme.typography.labelSmall, color = p.sub)
            }
            if (onEdit != null) {
                IconButton(onEdit, Modifier.size(48.dp)) { Icon(Icons.Rounded.Edit, "Edit ${f.name}", Modifier.size(18.dp), tint = p.muted) }
            } else {
                Spacer(Modifier.width(8.dp))
            }
        }
    }
    if (flat) {
        Box(Modifier.clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onClick)) { row() }
    } else {
        Box(Modifier.clip(RoundedCornerShape(16.dp)).background(p.card).clickable(role = Role.Button, onClick = onClick).padding(start = 14.dp, top = 4.dp, bottom = 4.dp, end = 2.dp)) { row() }
    }
}
