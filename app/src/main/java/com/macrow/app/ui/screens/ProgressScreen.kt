package com.macrow.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macrow.app.data.MacrowRepository
import com.macrow.app.domain.Nutrients
import com.macrow.app.domain.WeightPoint
import com.macrow.app.domain.kcalText
import com.macrow.app.domain.weightTrend
import com.macrow.app.ui.ProgressVm
import com.macrow.app.ui.UiErrors
import com.macrow.app.ui.components.ChoiceChip
import com.macrow.app.ui.components.EmptyState
import com.macrow.app.ui.components.GradientButton
import com.macrow.app.ui.components.MacrowCard
import com.macrow.app.ui.components.MacrowField
import com.macrow.app.ui.components.MacrowSheet
import com.macrow.app.ui.components.SoftButton
import com.macrow.app.ui.macrowVm
import com.macrow.app.ui.theme.Tone
import com.macrow.app.ui.theme.macrow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun ProgressScreen(onSettings: () -> Unit) {
    val vm = macrowVm { ProgressVm(it) }
    val p = MaterialTheme.macrow
    val settings by vm.settings.collectAsStateWithLifecycle()
    val weights by vm.weights.collectAsStateWithLifecycle()
    val week by vm.week.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()
    var logging by rememberSaveable { mutableStateOf(false) }
    var range by rememberSaveable { mutableIntStateOf(30) }
    val today = MacrowRepository.today()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Progress", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
            IconButton(onSettings) { Icon(Icons.Rounded.Settings, "Settings") }
        }
        Spacer(Modifier.height(8.dp))

        MacrowCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Weight", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                SoftButton("Log weight", { logging = true }, icon = Icons.Rounded.Add)
            }
            if (weights.isEmpty()) {
                EmptyState(Icons.Rounded.MonitorWeight, "Log your first weigh-in", "Weigh yourself in the morning, once or twice a week. The trend line smooths out the day-to-day noise.")
            } else {
                val latest = weights.maxBy { it.day }
                val first = weights.minBy { it.day }
                val trend = weightTrend(weights).maxBy { it.day }
                val change = latest.kg - first.kg
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Stat("Now", "${"%.1f".format(latest.kg)} kg", Modifier.weight(1f))
                    Stat("Trend", "${"%.1f".format(trend.kg)} kg", Modifier.weight(1f))
                    Stat("Change", "${if (change > 0) "+" else if (change < 0) "−" else ""}${"%.1f".format(abs(change))} kg", Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip("30 days", range == 30, { range = 30 })
                    ChoiceChip("90 days", range == 90, { range = 90 })
                    ChoiceChip("All", range == 0, { range = 0 })
                }
                Spacer(Modifier.height(10.dp))
                val from = if (range == 0) Long.MIN_VALUE else today - range + 1
                val shown = weights.filter { it.day >= from }
                if (shown.size < 2) {
                    Text(if (shown.isEmpty()) "No weigh-ins in this range." else "Log one more weigh-in to see a line.", style = MaterialTheme.typography.bodyMedium, color = p.sub)
                } else {
                    WeightChart(shown, weightTrend(weights).filter { it.day >= from })
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        val perDay = (today - 6..today).map { d -> d to week.filter { it.entry.day == d }.fold(Nutrients.ZERO) { a, e -> a + e.nutrients } }
        MacrowCard {
            Text("Calories, last 7 days", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            CalorieBars(perDay, settings.targets.kcal)
            val logged = perDay.filter { it.second.kcal > 0 }
            Spacer(Modifier.height(8.dp))
            Text(
                if (logged.isEmpty()) "Log meals in your diary and your week shows up here." else "Average ${logged.map { it.second.kcal }.average().kcalText()} kcal on the ${logged.size} ${if (logged.size == 1) "day" else "days"} you logged.",
                style = MaterialTheme.typography.bodyMedium, color = p.sub,
            )
        }
        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile("Streak", if (streak == 1) "1 day" else "$streak days", Modifier.weight(1f), Icons.Rounded.LocalFireDepartment, Tone.Gold)
            val logged = perDay.count { it.second.kcal > 0 }
            Tile("Logged this week", "$logged of 7", Modifier.weight(1f), Icons.Rounded.EventAvailable, Tone.Green)
        }
        Spacer(Modifier.height(112.dp))
    }

    if (logging) {
        LogWeightSheet(
            initial = weights.maxByOrNull { it.day }?.kg ?: settings.profile.weightKg,
            recent = weights.sortedByDescending { it.day }.take(5),
            onSave = { kg -> vm.setWeight(today, kg); logging = false },
            onDelete = { vm.deleteWeight(it) },
            onDismiss = { logging = false },
        )
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    val p = MaterialTheme.macrow
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(p.tint(Tone.Green).first).padding(10.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = p.tint(Tone.Green).second)
        Text(value, style = MaterialTheme.typography.titleMedium, color = p.tint(Tone.Green).second)
    }
}

@Composable
private fun Tile(label: String, value: String, modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, tone: Tone) {
    val p = MaterialTheme.macrow
    MacrowCard(modifier, padding = 12.dp) {
        com.macrow.app.ui.components.IconTile(icon, tone, size = 36.dp)
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodySmall, color = p.sub)
    }
}

/** Weigh-ins as dots, the smoothed trend as a green line that draws itself in. */
@Composable
private fun WeightChart(points: List<WeightPoint>, trend: List<WeightPoint>) {
    val p = MaterialTheme.macrow
    val locale = LocalLocale.current.platformLocale
    val progress = remember { Animatable(0f) }
    LaunchedEffect(points) { progress.snapTo(0f); progress.animateTo(1f, tween(900)) }
    val minDay = points.minOf { it.day }
    val maxDay = points.maxOf { it.day }
    val lo = minOf(points.minOf { it.kg }, trend.minOf { it.kg })
    val hi = maxOf(points.maxOf { it.kg }, trend.maxOf { it.kg })
    val pad = ((hi - lo) * 0.2).coerceAtLeast(0.5)
    val yMin = lo - pad
    val yMax = hi + pad
    val fmt = DateTimeFormatter.ofPattern("d MMM", locale)

    Column {
        Box(Modifier.fillMaxWidth().height(170.dp).semantics { contentDescription = "Weight chart: ${"%.1f".format(points.minBy { it.day }.kg)} kg on ${LocalDate.ofEpochDay(minDay).format(fmt)} to ${"%.1f".format(points.maxBy { it.day }.kg)} kg on ${LocalDate.ofEpochDay(maxDay).format(fmt)}" }) {
            Canvas(Modifier.fillMaxSize()) {
                fun x(d: Long) = if (maxDay == minDay) size.width / 2 else ((d - minDay).toFloat() / (maxDay - minDay)) * size.width
                fun y(kg: Double) = (size.height * (1 - (kg - yMin) / (yMax - yMin))).toFloat()
                // Two guide lines.
                val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                for (kg in listOf(lo, hi)) drawLine(p.line, Offset(0f, y(kg)), Offset(size.width, y(kg)), 2f, pathEffect = dash)
                val line = Path().apply {
                    trend.sortedBy { it.day }.forEachIndexed { i, t -> if (i == 0) moveTo(x(t.day), y(t.kg)) else lineTo(x(t.day), y(t.kg)) }
                }
                val measure = PathMeasure().apply { setPath(line, false) }
                val partial = Path()
                measure.getSegment(0f, measure.length * progress.value, partial, true)
                // Soft fill under the part of the line drawn so far.
                val first = trend.minBy { it.day }
                val lastX = if (measure.length * progress.value <= 0f) x(first.day) else measure.getPosition(measure.length * progress.value).x
                val fill = Path().apply {
                    addPath(partial)
                    lineTo(lastX, size.height); lineTo(x(first.day), size.height); close()
                }
                drawPath(fill, Brush.verticalGradient(listOf(p.green.copy(alpha = 0.22f), Color.Transparent)))
                drawPath(partial, p.green, style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                points.forEach { drawCircle(p.card, 9f, Offset(x(it.day), y(it.kg))); drawCircle(p.green, 6f, Offset(x(it.day), y(it.kg))) }
            }
            Text("%.1f".format(hi), Modifier.align(Alignment.TopStart), style = MaterialTheme.typography.labelSmall, color = p.muted)
            Text("%.1f".format(lo), Modifier.align(Alignment.BottomStart), style = MaterialTheme.typography.labelSmall, color = p.muted)
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(LocalDate.ofEpochDay(minDay).format(fmt), style = MaterialTheme.typography.labelSmall, color = p.muted)
            Text(LocalDate.ofEpochDay(maxDay).format(fmt), style = MaterialTheme.typography.labelSmall, color = p.muted)
        }
    }
}

@Composable
private fun CalorieBars(days: List<Pair<Long, Nutrients>>, target: Int) {
    val p = MaterialTheme.macrow
    val locale = LocalLocale.current.platformLocale
    val top = maxOf(target * 1.3, days.maxOf { it.second.kcal } * 1.1, 1.0)
    val summary = days.joinToString(", ") { (d, n) -> "${LocalDate.ofEpochDay(d).dayOfWeek.getDisplayName(TextStyle.SHORT, locale)} ${n.kcal.roundToInt()}" }
    Box(Modifier.fillMaxWidth().height(150.dp).semantics { contentDescription = "Calories per day against a target of $target: $summary" }) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            days.forEach { (d, n) ->
                val h by animateDpAsState((150 * (n.kcal / top)).toFloat().dp, tween(600), label = "bar$d")
                val color = when {
                    n.kcal <= 0 -> p.line
                    n.kcal > target * 1.05 -> p.bad
                    n.kcal < target * 0.9 -> p.carbs
                    else -> p.good
                }
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.fillMaxWidth().height(h.coerceAtLeast(4.dp)).background(color, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)))
                }
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val y = (size.height * (1 - target / top)).toFloat()
            drawLine(p.ink.copy(alpha = 0.45f), Offset(0f, y), Offset(size.width, y), 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)))
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        days.forEach { (d, _) ->
            Text(
                LocalDate.ofEpochDay(d).dayOfWeek.getDisplayName(TextStyle.SHORT, locale).take(3), Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall, color = p.sub, textAlign = TextAlign.Center, maxLines = 1,
            )
        }
    }
}

@Composable
private fun LogWeightSheet(initial: Double, recent: List<WeightPoint>, onSave: (Double) -> Unit, onDelete: (Long) -> Unit, onDismiss: () -> Unit) {
    val p = MaterialTheme.macrow
    var text by rememberSaveable { mutableStateOf("%.1f".format(Locale.US, initial)) }
    val kg = text.replace(',', '.').toDoubleOrNull()
    val error = if (kg == null || kg < 20 || kg > 500) "Enter a weight from 20 to 500 kg" else null
    MacrowSheet(onDismiss) {
        Text("Log today's weight", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        MacrowField(text, { text = it }, "Weight", Modifier.fillMaxWidth(), keyboard = KeyboardType.Decimal, suffix = "kg", error = error, maxLength = 6)
        Spacer(Modifier.height(12.dp))
        GradientButton("Save weight", { if (error == null && kg != null) onSave(kg) else UiErrors.say("Enter a weight from 20 to 500 kg.") }, Modifier.fillMaxWidth())
        if (recent.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Recent weigh-ins", style = MaterialTheme.typography.titleSmall, color = p.sub)
            recent.forEach { w ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(dayLabel(w.day), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Text("${"%.1f".format(w.kg)} kg", style = MaterialTheme.typography.titleSmall)
                    IconButton({ onDelete(w.day) }, Modifier.size(48.dp)) { Icon(Icons.Rounded.Close, "Delete the ${dayLabel(w.day)} weigh-in", Modifier.size(18.dp), tint = p.muted) }
                }
            }
        }
    }
}
