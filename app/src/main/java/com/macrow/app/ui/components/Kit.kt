package com.macrow.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.macrow.app.domain.Limits
import com.macrow.app.ui.theme.Tone
import com.macrow.app.ui.theme.macrow

val LocalHaptics = staticCompositionLocalOf { true }

/** Returns a function that taps the phone, unless haptics are switched off in Settings. */
@Composable
fun rememberHaptic(): (HapticFeedbackType) -> Unit {
    val on = LocalHaptics.current
    val h = LocalHapticFeedback.current
    return { type -> if (on) h.performHapticFeedback(type) }
}

private val cardShape = RoundedCornerShape(20.dp)

/** Lets text inside a fixed-size shape (like the calorie ring) grow with the system font size, but only up to [max] times. */
@Composable
fun CappedFont(max: Float, content: @Composable () -> Unit) {
    val d = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(d.density, d.fontScale.coerceAtMost(max)), content = content)
}

@Composable
fun MacrowCard(modifier: Modifier = Modifier, padding: Dp = 14.dp, content: @Composable ColumnScope.() -> Unit) {
    val p = MaterialTheme.macrow
    Column(
        modifier
            .shadow(6.dp, cardShape, ambientColor = p.shadow.copy(alpha = 0.10f), spotColor = p.shadow.copy(alpha = 0.14f))
            .background(p.card, cardShape)
            .padding(padding),
        content = content,
    )
}

/** Big gradient card with soft circles behind the content for depth. */
@Composable
fun HeroCard(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val p = MaterialTheme.macrow
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .shadow(14.dp, shape, ambientColor = p.greenDeep.copy(alpha = 0.35f), spotColor = p.greenDeep.copy(alpha = 0.45f))
            .clip(shape)
            .background(Brush.linearGradient(listOf(p.greenDeep, p.greenLight)))
            .drawBehind {
                drawCircle(Color.White.copy(alpha = 0.12f), radius = size.width * 0.36f, center = Offset(size.width * 0.94f, size.height * 0.04f))
                drawCircle(Color(0xFFFFD166).copy(alpha = 0.24f), radius = size.width * 0.22f, center = Offset(size.width * 0.80f, size.height * 1.04f))
                drawCircle(Color.White.copy(alpha = 0.07f), radius = size.width * 0.18f, center = Offset(size.width * 0.04f, size.height * 0.96f))
            },
        content = content,
    )
}

@Composable
fun Pill(text: String, tone: Tone, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val (bg, fg) = MaterialTheme.macrow.tint(tone)
    Row(modifier.clip(RoundedCornerShape(14.dp)).background(bg).padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(14.dp), tint = fg)
            Spacer(Modifier.size(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}

@Composable
fun GlassPill(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.2f)).padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(14.dp), tint = Color.White)
            Spacer(Modifier.size(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

@Composable
fun IconTile(icon: ImageVector, tone: Tone, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val (bg, fg) = MaterialTheme.macrow.tint(tone)
    Box(modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(size * 0.55f), tint = fg)
    }
}

@Composable
fun GradientButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val p = MaterialTheme.macrow
    val colors = listOf(p.greenLight, p.greenDeep)
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "press")
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(10.dp, shape, ambientColor = colors.last().copy(alpha = 0.3f), spotColor = colors.last().copy(alpha = 0.4f))
            .clip(shape)
            .background(Brush.linearGradient(colors))
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp), tint = Color.White)
            Spacer(Modifier.size(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White, textAlign = TextAlign.Center)
    }
}

@Composable
fun SoftButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, tone: Tone = Tone.Green, icon: ImageVector? = null) {
    val (bg, fg) = MaterialTheme.macrow.tint(tone)
    Row(
        modifier.clip(RoundedCornerShape(16.dp)).background(bg).clickable(role = Role.Button, onClick = onClick).defaultMinSize(minHeight = 48.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp), tint = fg)
            Spacer(Modifier.size(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = fg, textAlign = TextAlign.Center)
    }
}

@Composable
fun ProgressRing(
    fraction: Float,
    modifier: Modifier = Modifier,
    stroke: Dp = 10.dp,
    track: Color = Color.White.copy(alpha = 0.25f),
    color: Color = Color(0xFFFFD166),
    content: @Composable BoxScope.() -> Unit = {},
) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(900), label = "ring")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val d = Size(size.width - s, size.height - s)
            drawArc(track, 0f, 360f, false, Offset(s / 2, s / 2), d, style = Stroke(s, cap = StrokeCap.Round))
            if (f > 0.001f) drawArc(color, -90f, 360f * f, false, Offset(s / 2, s / 2), d, style = Stroke(s, cap = StrokeCap.Round))
        }
        content()
    }
}

/** A rounded bar that grows to [fraction] with a short ease. */
@Composable
fun LinearBar(fraction: Float, color: Color, modifier: Modifier = Modifier, track: Color = MaterialTheme.macrow.line, height: Dp = 9.dp) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(600), label = "bar")
    Box(modifier.height(height).clip(CircleShape).background(track)) {
        Box(Modifier.fillMaxSize().graphicsLayer { scaleX = f; transformOrigin = TransformOrigin(0f, 0.5f) }.clip(CircleShape).background(color))
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.macrow.sub)
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    val p = MaterialTheme.macrow
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(128.dp).clip(CircleShape).background(p.tint(Tone.Green).first))
            Box(Modifier.size(38.dp).offset(44.dp, (-38).dp).clip(CircleShape).background(p.carbs.copy(alpha = 0.85f)))
            Box(Modifier.size(22.dp).offset((-48).dp, 40.dp).clip(CircleShape).background(p.protein.copy(alpha = 0.8f)))
            Box(
                Modifier.size(70.dp).shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = p.shadow.copy(alpha = 0.15f)).background(p.card, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, Modifier.size(36.dp), tint = p.green) }
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = p.sub, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}

@Composable
fun TopBar(title: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
        } else {
            Spacer(Modifier.size(8.dp))
        }
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        actions()
    }
}

data class Tab(val route: String, val label: String, val icon: ImageVector)

val LeftTabs = listOf(Tab("diary", "Diary", Icons.AutoMirrored.Rounded.MenuBook), Tab("plan", "Plan", Icons.Rounded.CalendarMonth))
val RightTabs = listOf(Tab("progress", "Progress", Icons.AutoMirrored.Rounded.ShowChart), Tab("nutrients", "Nutrients", Icons.Rounded.Eco))
val TabRoutes = (LeftTabs + RightTabs).map { it.route }.toSet()

/** Floating bottom bar with the scan button in the middle. Its text stops growing past 130 percent so the labels always fit. */
@Composable
fun FloatingNav(current: String?, onSelect: (String) -> Unit, onScan: () -> Unit, modifier: Modifier = Modifier) {
    val p = MaterialTheme.macrow
    val shape = RoundedCornerShape(28.dp)
    val d = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(d.density, d.fontScale.coerceAtMost(1.3f))) {
        Row(
            modifier
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .shadow(14.dp, shape, ambientColor = p.shadow.copy(alpha = 0.16f), spotColor = p.shadow.copy(alpha = 0.22f))
                .background(p.card, shape)
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LeftTabs.forEach { NavItem(it, it.route == current, onSelect) }
            ScanButton(onScan)
            RightTabs.forEach { NavItem(it, it.route == current, onSelect) }
        }
    }
}

@Composable
private fun RowScope.NavItem(t: Tab, selected: Boolean, onSelect: (String) -> Unit) {
    val p = MaterialTheme.macrow
    val bg by animateColorAsState(if (selected) p.tint(Tone.Green).first else Color.Transparent, tween(200), label = "navBg")
    val fg by animateColorAsState(if (selected) p.tint(Tone.Green).second else p.muted, tween(200), label = "navFg")
    val lift by animateFloatAsState(if (selected) 1.08f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "navLift")
    Column(
        Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(18.dp)).background(bg).selectable(selected = selected, role = Role.Tab) { onSelect(t.route) }.padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(t.icon, null, Modifier.size(24.dp).graphicsLayer { scaleX = lift; scaleY = lift }, tint = fg)
        Text(t.label, style = MaterialTheme.typography.labelSmall, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ScanButton(onClick: () -> Unit) {
    val p = MaterialTheme.macrow
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "scanPress")
    Box(
        Modifier
            .padding(horizontal = 4.dp)
            .size(56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(10.dp, CircleShape, ambientColor = p.greenDeep.copy(alpha = 0.4f), spotColor = p.greenDeep.copy(alpha = 0.5f))
            .background(Brush.linearGradient(listOf(p.greenLight, p.greenDeep)), CircleShape)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Scan a barcode" },
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Rounded.QrCodeScanner, null, Modifier.size(28.dp), tint = Color.White) }
}

@Composable
fun MacrowField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Text,
    error: String? = null,
    suffix: String? = null,
    singleLine: Boolean = true,
    ime: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
    maxLength: Int = Limits.NAME,
) {
    val p = MaterialTheme.macrow
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(maxLength)) },
        modifier = modifier,
        label = { Text(label) },
        suffix = suffix?.let { { Text(it) } },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = ime),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }, onSearch = { onDone?.invoke() }),
        shape = RoundedCornerShape(16.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = p.card, unfocusedContainerColor = p.card, errorContainerColor = p.card,
            focusedIndicatorColor = p.green, unfocusedIndicatorColor = p.muted.copy(alpha = 0.4f), cursorColor = p.green,
            focusedLabelColor = p.green,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacrowSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val p = MaterialTheme.macrow
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = p.bg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(Modifier.imePadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 16.dp), content = content)
    }
}

@Composable
fun MacrowDialog(
    title: String,
    onDismiss: () -> Unit,
    confirm: String,
    onConfirm: () -> Unit,
    dismissText: String = "Cancel",
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = MaterialTheme.macrow
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.card,
        shape = RoundedCornerShape(26.dp),
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        // Scrolls so the buttons stay reachable at the largest font size.
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp), content = content) },
        confirmButton = { TextButton(onConfirm) { Text(confirm, fontWeight = FontWeight.SemiBold, color = p.green) } },
        dismissButton = { TextButton(onDismiss) { Text(dismissText, color = p.sub) } },
    )
}

/** Choice chip used for filters and meal pickers. */
@Composable
fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = MaterialTheme.macrow
    val bg by animateColorAsState(if (selected) p.green else p.card, tween(180), label = "chipBg")
    val fg by animateColorAsState(if (selected) Color.White else p.sub, tween(180), label = "chipFg")
    Box(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp)).background(bg).selectable(selected, role = Role.Tab, onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1) }
}

/** Two or three options in one rounded track; the chosen one is lifted onto a card. */
@Composable
fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val p = MaterialTheme.macrow
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.line).padding(4.dp)) {
        options.forEach { (value, label) ->
            val on = value == selected
            val bg by animateColorAsState(if (on) p.card else Color.Transparent, tween(180), label = "segBg")
            Box(
                Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).background(bg).selectable(on, role = Role.RadioButton) { onSelect(value) },
                contentAlignment = Alignment.Center,
            ) { Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) p.ink else p.sub, maxLines = 1) }
        }
    }
}

/** A plus/minus control around a number. With [onHero] it is drawn light, for use on the green hero card. */
@Composable
fun Stepper(
    text: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    minusLabel: String = "Less",
    plusLabel: String = "More",
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    onHero: Boolean = false,
) {
    val p = MaterialTheme.macrow
    val haptic = rememberHaptic()
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StepButton(Icons.Rounded.Remove, minusLabel, onHero) { haptic(HapticFeedbackType.ContextClick); onMinus() }
        Text(text, Modifier.defaultMinSize(minWidth = 48.dp), style = textStyle, color = if (onHero) Color.White else p.ink, textAlign = TextAlign.Center)
        StepButton(Icons.Rounded.Add, plusLabel, onHero) { haptic(HapticFeedbackType.ContextClick); onPlus() }
    }
}

@Composable
private fun StepButton(icon: ImageVector, label: String, onHero: Boolean, onClick: () -> Unit) {
    val p = MaterialTheme.macrow
    val (bg, fg) = if (onHero) Color.White.copy(alpha = 0.22f) to Color.White else p.tint(Tone.Green).first to p.tint(Tone.Green).second
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(bg).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, Modifier.size(22.dp), tint = fg) }
}

@Composable
fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, subtitle: String? = null) {
    val p = MaterialTheme.macrow
    Row(
        modifier.fillMaxWidth().toggleable(checked, role = Role.Switch, onValueChange = onChange).heightIn(min = 52.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = p.sub)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked, null,
            colors = SwitchDefaults.colors(checkedTrackColor = p.green, checkedThumbColor = Color.White, uncheckedTrackColor = p.line, uncheckedThumbColor = p.muted, uncheckedBorderColor = p.muted.copy(alpha = 0.4f)),
        )
    }
}
