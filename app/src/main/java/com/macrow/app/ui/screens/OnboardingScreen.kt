package com.macrow.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.macrow.app.MacrowApp
import com.macrow.app.data.MacrowRepository
import com.macrow.app.domain.Activity
import com.macrow.app.domain.Goal
import com.macrow.app.domain.Limits
import com.macrow.app.domain.Profile
import com.macrow.app.domain.Sex
import com.macrow.app.domain.Targets
import com.macrow.app.domain.maintenanceKcal
import com.macrow.app.domain.splitMacros
import com.macrow.app.domain.suggestedKcal
import com.macrow.app.domain.weeklyChangeKg
import com.macrow.app.ui.UiErrors
import com.macrow.app.ui.components.GlassPill
import com.macrow.app.ui.components.GradientButton
import com.macrow.app.ui.components.HeroCard
import com.macrow.app.ui.components.MacrowCard
import com.macrow.app.ui.components.MacrowField
import com.macrow.app.ui.components.Segmented
import com.macrow.app.ui.components.SoftButton
import com.macrow.app.ui.components.Stepper
import com.macrow.app.ui.components.TopBar
import com.macrow.app.ui.theme.macrow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private fun String.toIntIn(range: IntRange) = trim().toIntOrNull()?.takeIf { it in range }
private fun String.toKg() = replace(',', '.').trim().toDoubleOrNull()?.takeIf { it in 30.0..400.0 }

/**
 * Three steps: goal, about you, your target. With [editing] it starts on "about you" with your saved
 * numbers and saves back to Settings instead of starting fresh.
 */
@Composable
fun OnboardingScreen(editing: Boolean, current: Profile, currentTargets: Targets, onBack: () -> Unit, onDone: () -> Unit) {
    val repo = (LocalContext.current.applicationContext as MacrowApp).repo
    val scope = rememberCoroutineScope()
    val p = MaterialTheme.macrow
    var step by rememberSaveable { mutableIntStateOf(if (editing) 1 else 0) }
    var goal by rememberSaveable { mutableStateOf(if (editing) current.goal else Goal.LOSE) }
    var sex by rememberSaveable { mutableStateOf(current.sex) }
    var age by rememberSaveable { mutableStateOf(if (editing) current.age.toString() else "") }
    var height by rememberSaveable { mutableStateOf(if (editing) current.heightCm.toString() else "") }
    var weight by rememberSaveable { mutableStateOf(if (editing) current.weightKg.toString() else "") }
    var activity by rememberSaveable { mutableStateOf(current.activity) }
    var kcalAdjust by rememberSaveable { mutableIntStateOf(0) }
    var tried by rememberSaveable { mutableStateOf(false) }

    val ageV = age.toIntIn(10..110)
    val heightV = height.toIntIn(100..250)
    val weightV = weight.toKg()
    val valid = ageV != null && heightV != null && weightV != null
    val profile = if (valid) Profile(sex, ageV, heightV, weightV, activity, goal) else null

    fun back() { if (step > 0 && !(editing && step == 1)) step-- else onBack() }
    BackHandler(enabled = step > 0 || editing) { back() }

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar(if (editing) "About you" else "Set up Macrow", onBack = if (step > 0 || editing) ({ back() }) else null)
        Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { i ->
                val c by animateColorAsState(if (i <= step) p.green else p.line, tween(250), label = "dot$i")
                Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(c))
            }
        }
        AnimatedContent(
            step,
            Modifier.weight(1f),
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(280)) { it / 4 * dir } + fadeIn(tween(240))) togetherWith (slideOutHorizontally(tween(200)) { -it / 6 * dir } + fadeOut(tween(140)))
            },
            label = "onboardingStep",
        ) { s ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (s) {
                    0 -> {
                        Text("What's your goal?", style = MaterialTheme.typography.headlineMedium)
                        Text("Everything you enter stays on this phone.", style = MaterialTheme.typography.bodyMedium, color = p.sub)
                        Goal.entries.forEach { g -> ChoiceCard(g.label, g.hint, goal == g) { goal = g } }
                    }
                    1 -> {
                        Text("About you", style = MaterialTheme.typography.headlineMedium)
                        Text("Used to work out your daily target. You can change it any time.", style = MaterialTheme.typography.bodyMedium, color = p.sub)
                        Segmented(Sex.entries.map { it to it.label }, sex, { sex = it })
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MacrowField(age, { age = it.filter(Char::isDigit) }, "Age", Modifier.weight(1f), KeyboardType.Number, if (tried && ageV == null) "10 to 110" else null, maxLength = 3)
                            MacrowField(height, { height = it.filter(Char::isDigit) }, "Height", Modifier.weight(1f), KeyboardType.Number, if (tried && heightV == null) "100 to 250" else null, suffix = "cm", maxLength = 3)
                        }
                        MacrowField(weight, { weight = it }, "Weight", Modifier.fillMaxWidth(), KeyboardType.Decimal, if (tried && weightV == null) "30 to 400 kg" else null, suffix = "kg", ime = ImeAction.Done, maxLength = 6)
                        Text("How active are you?", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
                        Activity.entries.forEach { a -> ChoiceCard(a.label, a.hint, activity == a) { activity = a } }
                    }
                    else -> {
                        val pr = profile ?: Profile()
                        val suggested = suggestedKcal(pr)
                        val kcal = (suggested + kcalAdjust).coerceIn(Limits.MIN_KCAL_TARGET, Limits.MAX_KCAL_TARGET)
                        val t = splitMacros(kcal, pr.weightKg, pr.goal)
                        val maintenance = maintenanceKcal(pr)
                        val weekly = weeklyChangeKg(kcal, maintenance)
                        Text("Your daily target", style = MaterialTheme.typography.headlineMedium)
                        HeroCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Stepper(
                                    "%,d".format(kcal), { kcalAdjust -= 50 }, { kcalAdjust += 50 }, minusLabel = "50 fewer calories", plusLabel = "50 more calories",
                                    textStyle = MaterialTheme.typography.displayMedium, onHero = true,
                                )
                                Text("kcal per day", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.9f))
                                Spacer(Modifier.height(8.dp))
                                GlassPill(
                                    when {
                                        abs(weekly) < 0.05 -> "Holds your weight steady"
                                        weekly < 0 -> "About ${"%.1f".format(abs(weekly))} kg lost per week"
                                        else -> "About ${"%.1f".format(weekly)} kg gained per week"
                                    },
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MacroTile("Protein", "${t.proteinG} g", p.protein, Modifier.weight(1f))
                            MacroTile("Carbs", "${t.carbsG} g", p.carbs, Modifier.weight(1f))
                            MacroTile("Fat", "${t.fatG} g", p.fat, Modifier.weight(1f))
                        }
                        Text(
                            "Maintenance is about ${"%,d".format(maintenance)} kcal. Your target is ${"%,d".format(abs(kcal - maintenance))} ${if (kcal >= maintenance) "above" else "below"} it. " +
                                "Targets are estimates, not medical advice.",
                            style = MaterialTheme.typography.bodyMedium, color = p.sub,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        GradientButton(
            if (step < 2) "Next" else if (editing) "Save" else "Start tracking",
            {
                if (step == 1 && profile == null) {
                    tried = true
                    UiErrors.say("Fill in your age, height and weight.")
                } else if (step < 2) {
                    step++
                } else {
                    val pr = profile ?: return@GradientButton
                    val kcal = (suggestedKcal(pr) + kcalAdjust).coerceIn(Limits.MIN_KCAL_TARGET, Limits.MAX_KCAL_TARGET)
                    scope.launch {
                        try {
                            repo.saveProfile(pr, splitMacros(kcal, pr.weightKg, pr.goal), if (editing) null else MacrowRepository.today())
                            onDone()
                        } catch (e: Exception) {
                            UiErrors.report(e)
                        }
                    }
                }
            },
            Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
        )
    }
}

@Composable
private fun ChoiceCard(title: String, hint: String, selected: Boolean, onClick: () -> Unit) {
    val p = MaterialTheme.macrow
    val border by animateColorAsState(if (selected) p.green else Color.Transparent, tween(180), label = "choiceBorder")
    val bg by animateColorAsState(if (selected) p.tint(com.macrow.app.ui.theme.Tone.Green).first else p.card, tween(180), label = "choiceBg")
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 60.dp).clip(RoundedCornerShape(18.dp)).background(bg)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .then(Modifier.background(Color.Transparent))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = p.sub)
        }
        Spacer(Modifier.width(10.dp))
        Icon(if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null, tint = if (selected) p.green else p.muted)
    }
}

@Composable
private fun MacroTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    val p = MaterialTheme.macrow
    MacrowCard(modifier, padding = 10.dp) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.bodySmall, color = p.sub)
    }
}
