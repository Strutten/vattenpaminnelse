@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package se.vatten.paminnelse

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

// ---------- Hjälpfunktioner för text ----------

private val SV = Locale.forLanguageTag("sv")
private val DAY_LETTERS = listOf("M", "T", "O", "T", "F", "L", "S")
private val DAY_NAMES = listOf("mån", "tis", "ons", "tor", "fre", "lör", "sön")

fun fmt(m: Int) = String.format(SV, "%02d:%02d", m / 60, m % 60)

fun intervalText(m: Int) = when {
    m == 60 -> "Varje timme"
    m % 60 == 0 -> "Var ${m / 60}:e timme"
    m < 60 -> "Var $m:e minut"
    else -> "Var ${m / 60} h ${m % 60} min"
}

fun shortInterval(m: Int) = when {
    m < 60 -> "$m min"
    m % 60 == 0 -> "${m / 60} h"
    else -> "${m / 60} h ${m % 60}"
}

fun daysText(days: Int) = when (days) {
    Reminder.ALL_DAYS -> "varje dag"
    Reminder.WEEKDAYS -> "vardagar"
    Reminder.WEEKEND -> "helger"
    0 -> "inga dagar"
    else -> (0..6).filter { days and (1 shl it) != 0 }.joinToString(", ") { DAY_NAMES[it] }
}

fun scheduleText(r: Reminder): String {
    val what = if (r.useTimes) r.times.sorted().joinToString(", ") { fmt(it) }
    else "${intervalText(r.intervalMin)} ${fmt(r.startMin)}–${fmt(r.endMin)}"
    return "$what, ${daysText(r.days)}"
}

fun whenText(ms: Long): String {
    val t = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    val d = t.toLocalDate()
    val today = LocalDate.now()
    val day = when (d) {
        today -> "idag"
        today.plusDays(1) -> "imorgon"
        else -> t.format(DateTimeFormatter.ofPattern("EEEE", SV))
    }
    return day
}

fun clockText(ms: Long): String =
    Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))

private val EMOJIS = listOf(
    "💧", "💊", "🧘", "👀", "🌱", "📚", "🪥", "🏃", "💪", "🍎", "☕", "😴",
    "🧹", "🧺", "🛒", "🐶", "🐱", "📞", "💼", "🎵", "🎮", "❤️", "⭐", "✨", "🔔", "⏰",
)

private val COLORS = listOf(
    0xFF3D5AFE, 0xFF2F80ED, 0xFF12A594, 0xFF30A46C, 0xFF46A758, 0xFFF5A524,
    0xFFF76B15, 0xFFE5484D, 0xFFD6409F, 0xFF8E4EC6, 0xFF5B6B7A,
)

// ---------- Navigation ----------

sealed interface Screen {
    data object Home : Screen
    data class Edit(val r: Reminder, val isNew: Boolean) : Screen
}

@Composable
fun App(list: List<Reminder>, refresh: () -> Unit) {
    val ctx = LocalContext.current
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) perm.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            (fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 12 }) togetherWith fadeOut(tween(150))
        },
        label = "nav",
    ) { s ->
        when (s) {
            is Screen.Home -> HomeScreen(
                list = list,
                refresh = refresh,
                onOpen = { screen = Screen.Edit(it, false) },
                onNew = { screen = Screen.Edit(it.copy(id = Store.newId(ctx)), true) },
            )
            is Screen.Edit -> EditScreen(s.r, s.isNew) { screen = Screen.Home; refresh() }
        }
    }
}

// ---------- Startsidan ----------

@Composable
fun HomeScreen(list: List<Reminder>, refresh: () -> Unit, onOpen: (Reminder) -> Unit, onNew: (Reminder) -> Unit) {
    val ctx = LocalContext.current
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showTemplates by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { LargeTopAppBar(title = { Text("Påminn") }, scrollBehavior = scroll) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Ny påminnelse") },
                icon = { Icon(Icons.Rounded.Add, null) },
                onClick = { showTemplates = true },
            )
        },
    ) { pad ->
        if (list.isEmpty()) {
            EmptyState(Modifier.padding(pad).fillMaxSize()) { showTemplates = true }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = pad.calculateTopPadding() + 4.dp,
                    bottom = pad.calculateBottomPadding() + 100.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { NextUp(list) }
                items(list, key = { it.id }) { r ->
                    ReminderCard(
                        r = r,
                        onClick = { onOpen(r) },
                        onToggle = { on -> Store.upsert(ctx, Scheduler.schedule(ctx, r.copy(enabled = on))); refresh() },
                        onPlus = { Store.addCount(ctx, r.id, 1); refresh() },
                    )
                }
            }
        }
    }

    if (showTemplates) {
        TemplateSheet(onDismiss = { showTemplates = false }) { t ->
            showTemplates = false
            onNew(t)
        }
    }
}

@Composable
fun NextUp(list: List<Reminder>) {
    val next = list.filter { it.enabled && it.nextAt > 0 }.minByOrNull { it.nextAt }
    val accent = next?.let { Color(it.color) } ?: MaterialTheme.colorScheme.outline
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(accent.copy(alpha = 0.14f))
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (next != null) {
                Text("Nästa ${whenText(next.nextAt)}", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(clockText(next.nextAt), style = MaterialTheme.typography.displayMedium, color = accent)
                Text(next.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                Text("Inget planerat", style = MaterialTheme.typography.headlineMedium)
                Text("Slå på en påminnelse så syns den här.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (next != null) Text(next.emoji, fontSize = 56.sp)
    }
}

@Composable
fun EmptyState(modifier: Modifier, onCreate: () -> Unit) {
    Column(
        modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🔔", fontSize = 72.sp)
        Spacer(Modifier.height(16.dp))
        Text("Inga påminnelser än", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Välj en mall eller gör en egen. Allt går att ändra efteråt.",
            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onCreate) { Text("Skapa påminnelse") }
    }
}

@Composable
fun EmojiTile(emoji: String, accent: Color, size: Int = 52) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape((size * 0.32f).dp)).background(accent.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) { Text(emoji, fontSize = (size * 0.5f).sp) }
}

@Composable
fun ReminderCard(r: Reminder, onClick: () -> Unit, onToggle: (Boolean) -> Unit, onPlus: () -> Unit) {
    val accent = Color(r.color)
    val fade by animateFloatAsState(if (r.enabled) 1f else 0.5f, label = "fade")
    ElevatedCard(onClick = onClick, shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.alpha(fade)) { EmojiTile(r.emoji, accent) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f).alpha(fade)) {
                    Text(r.title.ifBlank { "Utan namn" }, style = MaterialTheme.typography.titleMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(scheduleText(r), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = r.enabled, onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Color.White),
                )
            }
            if (r.track) {
                val c = r.todayCount()
                val progress by animateFloatAsState((c.toFloat() / r.goal).coerceIn(0f, 1f), label = "p")
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.weight(1f).height(10.dp).clip(CircleShape),
                        color = accent, trackColor = accent.copy(alpha = 0.15f),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("$c av ${r.goal}", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(8.dp))
                    FilledTonalIconButton(
                        onClick = onPlus,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = accent.copy(alpha = 0.18f), contentColor = accent),
                    ) { Icon(Icons.Rounded.Add, "Lägg till en") }
                }
            }
        }
    }
}

@Composable
fun TemplateSheet(onDismiss: () -> Unit, onPick: (Reminder) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text("Vad vill du bli påmind om?", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 4.dp, bottom = 16.dp))
            TEMPLATES.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                    row.forEach { t ->
                        val accent = Color(t.color)
                        Surface(
                            onClick = { onPick(t) },
                            shape = RoundedCornerShape(22.dp),
                            color = accent.copy(alpha = 0.13f),
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(t.emoji, fontSize = 30.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(t.title.ifBlank { "Egen" }, style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (t.title.isBlank()) "Börja från noll" else scheduleText(t),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---------- Redigera ----------

private const val PICK_START = -1
private const val PICK_END = -2
private const val PICK_ADD = -3

@Composable
fun EditScreen(initial: Reminder, isNew: Boolean, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var d by remember { mutableStateOf(initial) }
    var pickFor by remember { mutableStateOf<Int?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val accent by animateColorAsState(Color(d.color), label = "accent")

    fun save() {
        Store.upsert(ctx, Scheduler.schedule(ctx, d.copy(title = d.title.ifBlank { "Påminnelse" })))
        onClose()
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Ny påminnelse" else "Ändra påminnelse") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Stäng") } },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(onClick = { Notifier.show(ctx, d) }, modifier = Modifier.weight(1f).height(52.dp)) {
                        Text("Testa notisen")
                    }
                    Button(
                        onClick = { save() },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                    ) { Text("Spara", fontWeight = FontWeight.SemiBold) }
                }
            }
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            NotificationPreview(d, accent)

            Section("Utseende") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(EMOJIS) { e ->
                        val sel = e == d.emoji
                        Box(
                            Modifier.size(48.dp).clip(RoundedCornerShape(15.dp))
                                .background(if (sel) accent.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface)
                                .border(2.dp, if (sel) accent else Color.Transparent, RoundedCornerShape(15.dp))
                                .clickable { d = d.copy(emoji = e) },
                            contentAlignment = Alignment.Center,
                        ) { Text(e, fontSize = 24.sp) }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    COLORS.forEach { c ->
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).background(Color(c)).clickable { d = d.copy(color = c) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (c == d.color) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Section("Text") {
                Field("Rubrik", d.title, accent) { d = d.copy(title = it) }
                Field("Meddelande", d.message, accent, singleLine = false) { d = d.copy(message = it) }
                Field("Knapp i notisen", d.actionLabel, accent) { d = d.copy(actionLabel = it) }
            }

            Section("När") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !d.useTimes, onClick = { d = d.copy(useTimes = false) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text("Med jämna mellanrum") }
                    SegmentedButton(
                        selected = d.useTimes, onClick = { d = d.copy(useTimes = true) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text("Vid klockslag") }
                }

                if (!d.useTimes) {
                    Text(intervalText(d.intervalMin), style = MaterialTheme.typography.headlineMedium, color = accent)
                    Slider(
                        value = d.intervalMin.coerceIn(5, 240).toFloat(),
                        onValueChange = { d = d.copy(intervalMin = (it / 5f).roundToInt() * 5) },
                        valueRange = 5f..240f, steps = 46,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 30, 45, 60, 90, 120, 180).forEach { m ->
                            FilterChip(
                                selected = d.intervalMin == m,
                                onClick = { d = d.copy(intervalMin = m) },
                                label = { Text(shortInterval(m)) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent.copy(alpha = 0.2f)),
                            )
                        }
                    }
                    Text("Bara mellan", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimeTile("Från", d.startMin, accent, Modifier.weight(1f)) { pickFor = PICK_START }
                        TimeTile("Till", d.endMin, accent, Modifier.weight(1f)) { pickFor = PICK_END }
                    }
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        d.times.sorted().forEach { t ->
                            InputChip(
                                selected = true,
                                onClick = { pickFor = d.times.indexOf(t) },
                                label = { Text(fmt(t), style = MaterialTheme.typography.titleMedium) },
                                trailingIcon = {
                                    Icon(Icons.Rounded.Close, "Ta bort", Modifier.size(18.dp).clickable {
                                        d = d.copy(times = d.times - t)
                                    })
                                },
                            )
                        }
                        AssistChip(
                            onClick = { pickFor = PICK_ADD },
                            label = { Text("Lägg till tid") },
                            leadingIcon = { Icon(Icons.Rounded.Add, null, Modifier.size(18.dp)) },
                        )
                    }
                }

                Text("Dagar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    (0..6).forEach { i ->
                        val on = d.days and (1 shl i) != 0
                        Box(
                            Modifier.size(40.dp).clip(CircleShape)
                                .background(if (on) accent else MaterialTheme.colorScheme.surface)
                                .clickable { d = d.copy(days = d.days xor (1 shl i)) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(DAY_LETTERS[i], fontWeight = FontWeight.SemiBold,
                                color = if (on) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Alla" to Reminder.ALL_DAYS, "Vardagar" to Reminder.WEEKDAYS, "Helger" to Reminder.WEEKEND)
                        .forEach { (label, mask) ->
                            FilterChip(
                                selected = d.days == mask, onClick = { d = d.copy(days = mask) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent.copy(alpha = 0.2f)),
                            )
                        }
                }
            }

            Section("Notisen") {
                ToggleRow("Ljud", null, d.sound, accent) { d = d.copy(sound = it) }
                ToggleRow("Vibration", null, d.vibrate, accent) { d = d.copy(vibrate = it) }
                ToggleRow("Räkna och sätt ett mål", "Visar hur många gånger du gjort det idag", d.track, accent) {
                    d = d.copy(track = it)
                }
                AnimatedVisibility(d.track, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Mål per dag", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            FilledTonalIconButton(onClick = { d = d.copy(goal = (d.goal - 1).coerceAtLeast(1)) }) {
                                Text("−", fontSize = 22.sp)
                            }
                            Text("${d.goal}", style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center, modifier = Modifier.width(48.dp))
                            FilledTonalIconButton(onClick = { d = d.copy(goal = (d.goal + 1).coerceAtMost(99)) }) {
                                Icon(Icons.Rounded.Add, "Öka")
                            }
                        }
                        ToggleRow("Tyst när målet är nått", null, d.stopAtGoal, accent) { d = d.copy(stopAtGoal = it) }
                    }
                }
            }

            if (!isNew) {
                TextButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Rounded.Delete, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ta bort påminnelsen")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    pickFor?.let { target ->
        val start = when (target) {
            PICK_START -> d.startMin
            PICK_END -> d.endMin
            PICK_ADD -> 12 * 60
            else -> d.times.getOrElse(target) { 12 * 60 }
        }
        TimeDialog(start, onDismiss = { pickFor = null }) { m ->
            d = when (target) {
                PICK_START -> d.copy(startMin = m)
                PICK_END -> d.copy(endMin = m)
                PICK_ADD -> d.copy(times = (d.times + m).distinct().sorted())
                else -> d.copy(times = d.times.toMutableList().also { it[target] = m }.distinct().sorted())
            }
            pickFor = null
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Ta bort ${d.title.ifBlank { "påminnelsen" }}?") },
            text = { Text("Den slutar påminna dig direkt och går inte att få tillbaka.") },
            confirmButton = {
                TextButton(onClick = {
                    Scheduler.cancel(ctx, d.id)
                    Store.delete(ctx, d.id)
                    onClose()
                }) { Text("Ta bort", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Behåll") } },
        )
    }
}

@Composable
fun NotificationPreview(d: Reminder, accent: Color) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(20.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Notifications, null, tint = Color.White, modifier = Modifier.size(13.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("Påminn, nu", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiTile(d.emoji, accent)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(d.title.ifBlank { "Rubrik" }, style = MaterialTheme.typography.titleMedium)
                    Text(Notifier.body(d).ifBlank { "Meddelande" }, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PreviewAction(d.actionLabel.ifBlank { "Klart" }, accent)
                PreviewAction("Om 10 min", accent)
            }
        }
    }
}

@Composable
fun PreviewAction(text: String, accent: Color) {
    Text(
        text, color = accent, style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.clip(CircleShape).background(accent.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
fun Field(label: String, value: String, accent: Color, singleLine: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        singleLine = singleLine, minLines = if (singleLine) 1 else 2,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedLabelColor = accent, cursorColor = accent),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun ToggleRow(title: String, subtitle: String?, checked: Boolean, accent: Color, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onChange(!checked) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Color.White))
    }
}

@Composable
fun TimeTile(label: String, minutes: Int, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, modifier = modifier) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(fmt(minutes), style = MaterialTheme.typography.headlineMedium, color = accent)
        }
    }
}

@Composable
fun TimeDialog(initial: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val st = rememberTimePickerState(initialHour = initial / 60, initialMinute = initial % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(st.hour * 60 + st.minute) }) { Text("Välj") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Avbryt") } },
        text = { TimePicker(state = st) },
    )
}
