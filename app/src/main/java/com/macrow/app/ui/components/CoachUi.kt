package com.macrow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.macrow.app.data.Food
import com.macrow.app.domain.CoachReview
import com.macrow.app.domain.Insight
import com.macrow.app.domain.MacroSplit
import com.macrow.app.domain.Mood
import com.macrow.app.ui.theme.Tone
import com.macrow.app.ui.theme.macrow

fun Mood.tone(): Tone = when (this) {
    Mood.GOOD -> Tone.Green
    Mood.OKAY -> Tone.Gold
    Mood.WATCH -> Tone.Berry
}

@Composable
private fun Mood.color() = when (this) {
    Mood.GOOD -> MaterialTheme.macrow.good
    Mood.OKAY -> MaterialTheme.macrow.carbs
    Mood.WATCH -> MaterialTheme.macrow.protein
}

/** The coach on the diary: a headline and the two most useful notes. Tap for the full review. */
@Composable
fun CoachCard(review: CoachReview, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val p = MaterialTheme.macrow
    MacrowCard(modifier.clip(RoundedCornerShape(20.dp)).clickable(role = Role.Button, onClick = onOpen), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(Icons.Rounded.AutoAwesome, review.mood.tone(), size = 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Coach", style = MaterialTheme.typography.labelMedium, color = p.sub)
                Text(review.headline, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Rounded.ChevronRight, "Open the full review", tint = p.muted)
        }
        review.insights.take(2).forEach { ins ->
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.padding(top = 7.dp).size(8.dp).clip(CircleShape).background(ins.mood.color()))
                Spacer(Modifier.width(10.dp))
                Text("${ins.title}. ${ins.body}", style = MaterialTheme.typography.bodyMedium, color = p.sub, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** The full review: what you ate, how the macros split, every note, and foods that would help. */
@Composable
fun CoachSheet(review: CoachReview, onSuggestion: (Food) -> Unit, onDismiss: () -> Unit) {
    val p = MaterialTheme.macrow
    MacrowSheet(onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(Icons.Rounded.AutoAwesome, review.mood.tone(), size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Coach review", style = MaterialTheme.typography.labelMedium, color = p.sub)
                Text(review.headline, style = MaterialTheme.typography.headlineSmall)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(review.summary, style = MaterialTheme.typography.bodyLarge)

        review.split?.let { s ->
            Spacer(Modifier.height(14.dp))
            MacrowCard(Modifier.fillMaxWidth()) {
                Text("Where your calories came from", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                SplitBar(s)
                Spacer(Modifier.height(8.dp))
                SplitLegend("Protein", s.protein, review.targetSplit.protein, p.protein)
                SplitLegend("Carbs", s.carbs, review.targetSplit.carbs, p.carbs)
                SplitLegend("Fat", s.fat, review.targetSplit.fat, p.fat)
            }
        }

        if (review.insights.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { review.insights.forEach { InsightRow(it) } }
        }

        if (review.suggestions.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text("Foods that would help", style = MaterialTheme.typography.titleSmall, color = p.sub)
            Spacer(Modifier.height(6.dp))
            review.suggestions.forEach { s ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(RoundedCornerShape(16.dp)).background(p.card).clickable(role = Role.Button) { onSuggestion(s.food) }
                        .defaultMinSize(minHeight = 56.dp).padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(s.food.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${s.food.servingLabel}: ${s.reason}", style = MaterialTheme.typography.bodySmall, color = p.sub)
                    }
                    Spacer(Modifier.width(8.dp))
                    Pill("Add", Tone.Green)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "The coach reads your own numbers with simple rules and runs on your phone. It is a friendly guide, not medical advice.",
            style = MaterialTheme.typography.bodySmall, color = p.muted,
        )
    }
}

@Composable
private fun InsightRow(ins: Insight) {
    val p = MaterialTheme.macrow
    val (bg, fg) = p.tint(ins.mood.tone())
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bg).padding(12.dp)) {
        Text(ins.title, style = MaterialTheme.typography.titleSmall, color = fg)
        Spacer(Modifier.height(2.dp))
        Text(ins.body, style = MaterialTheme.typography.bodyMedium, color = p.ink)
    }
}

@Composable
private fun SplitBar(s: MacroSplit) {
    val p = MaterialTheme.macrow
    Row(Modifier.fillMaxWidth().height(14.dp).clip(CircleShape), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf(s.protein to p.protein, s.carbs to p.carbs, s.fat to p.fat).forEach { (w, c) ->
            if (w > 0) Box(Modifier.weight(w.toFloat()).fillMaxWidth().height(14.dp).background(c))
        }
    }
}

@Composable
private fun SplitLegend(label: String, actual: Int, plan: Int, color: androidx.compose.ui.graphics.Color) {
    val p = MaterialTheme.macrow
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text("$actual%", style = MaterialTheme.typography.titleSmall)
        Text("  plan $plan%", style = MaterialTheme.typography.bodySmall, color = p.sub)
    }
}
