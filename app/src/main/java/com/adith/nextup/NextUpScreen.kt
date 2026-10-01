package com.adith.nextup

import android.text.format.DateFormat
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun NextUpTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) darkColorScheme(
        primary = Color(0xFFB3C5FF), onPrimary = Color(0xFF092B78),
        primaryContainer = Color(0xFF233F80), onPrimaryContainer = Color(0xFFDDE5FF),
        secondaryContainer = Color(0xFF2C3E64), onSecondaryContainer = Color(0xFFDDE5FF),
        background = Color(0xFF111724), onBackground = Color(0xFFE6ECFA),
        surface = Color(0xFF111724), onSurface = Color(0xFFE6ECFA),
        surfaceContainerHigh = Color(0xFF222E46), onSurfaceVariant = Color(0xFFBCC7DE),
        outline = Color(0xFF8795B1), outlineVariant = Color(0xFF44516A),
    ) else lightColorScheme(
        primary = Color(0xFF284FB9), onPrimary = Color.White,
        primaryContainer = Color(0xFFD3DFFC), onPrimaryContainer = Color(0xFF182F68),
        secondaryContainer = Color(0xFFD3DFFC), onSecondaryContainer = Color(0xFF182F68),
        background = Color(0xFFF7F9FE), onBackground = Color(0xFF18243D),
        surface = Color(0xFFF7F9FE), onSurface = Color(0xFF18243D),
        surfaceContainerHigh = Color(0xFFE5EBF9), onSurfaceVariant = Color(0xFF51617D),
        outline = Color(0xFF71809B), outlineVariant = Color(0xFFC3CEE2),
    )
    val view = LocalView.current
    SideEffect {
        (view.context as? android.app.Activity)?.window?.let { window ->
            androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
fun NextUpScreen(
    state: AgendaState,
    onCount: (Int) -> Unit,
    onRefresh: () -> Unit,
    onCalendarPermission: () -> Unit,
    onNotificationPermission: () -> Unit,
    onWalkingSettings: () -> Unit = {},
) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val feedbackScope = rememberCoroutineScope()
    var refreshRequested by remember { mutableStateOf(false) }
    var refreshStarted by remember { mutableStateOf(false) }
    var refreshBaseline by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(state.loading, state.lastUpdated, refreshRequested) {
        if (refreshRequested && state.loading) refreshStarted = true
        if (refreshRequested && (refreshStarted || state.lastUpdated != refreshBaseline) && !state.loading) {
            refreshRequested = false
            refreshStarted = false
            val message = if (state.error == null) "Agenda updated" else "Couldn't refresh. Try again."
            feedbackScope.launch { snackbar.showSnackbar(message) }
        }
    }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { now = System.currentTimeMillis(); delay(1_000) }
    }
    val locale = context.resources.configuration.locales[0]
    val zone = ZoneId.systemDefault()
    val labels = TimeLabels(zone, locale, DateFormat.is24HourFormat(context))
    val activeEvents = selectEvents(state.events, now, state.settings.count)
    val date = Instant.ofEpochMilli(now).atZone(zone).format(DateTimeFormatter.ofPattern("EEEE, MMM d", locale))
    val colors = MaterialTheme.colorScheme
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = colors.surface,
        topBar = {
            Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars), contentAlignment = Alignment.Center) {
                Row(Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("NextUp", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(date, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                    FilledTonalIconButton(onClick = { refreshBaseline = state.lastUpdated; refreshRequested = true; onRefresh() }, enabled = !state.loading,
                        modifier = Modifier.size(48.dp).semantics { contentDescription = "Refresh" }) {
                        if (state.loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(painterResource(R.drawable.ic_refresh), contentDescription = null, modifier = Modifier.size(22.dp))
                    }
                }
            }
        },
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().animateContentSize(tween(220)).verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                when {
                    state.loading && state.lastUpdated == null && state.events.isEmpty() && state.error == null ->
                        AgendaMessage("Getting your day ready", "Checking the calendars on your phone.")
                    state.error != null -> AgendaMessage("Couldn't refresh", state.error)
                    !state.calendarAllowed -> AgendaMessage("Calendar access required",
                        "See what's next from calendars already synced to your phone.", "Allow Calendar Access", onCalendarPermission)
                    activeEvents.isEmpty() -> AgendaMessage("A little room to breathe.", "Nothing coming up in the next 7 days.")
                    else -> {
                        Crossfade(targetState = activeEvents.first(), animationSpec = tween(220), label = "Next event") { event ->
                            NextEvent(event, labels, now)
                        }
                        if (activeEvents.size > 1) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Later", style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp))
                                activeEvents.drop(1).forEachIndexed { index, event ->
                                    TimelineEvent(event, labels, now)
                                    if (index < activeEvents.size - 2) HorizontalDivider(
                                        modifier = Modifier.padding(start = 100.dp), color = colors.outlineVariant.copy(alpha = .6f))
                                }
                            }
                        }
                    }
                }
                if (!state.notificationsAllowed && !state.loading) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Notifications are disabled", style = MaterialTheme.typography.titleSmall)
                        Text("Your agenda is ready here. Enable notifications to keep it in view.",
                            style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        TextButton(onNotificationPermission, contentPadding = PaddingValues(horizontal = 0.dp)) {
                            Text("Enable notifications")
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = .6f))
                    Text("Events to show", style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp))
                    EventCount(state.settings.count, !state.loading, onCount)
                    Text("In your silent notification", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onWalkingSettings, contentPadding = PaddingValues(0.dp)) { Text("Walking times") }
                    Text(state.walking.message, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (state.calendarAllowed && state.notificationsAllowed) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.ic_check), null, Modifier.size(16.dp), tint = colors.primary)
                            Text("Calendar connected · Notifications on", style = MaterialTheme.typography.labelMedium,
                                color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
                        }
                    }
                    state.lastUpdated?.let {
                        Text("Updated ${labels.clock(it)}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    Text("Swipe the notification away to hide it. Open NextUp to bring it back. Updates may be delayed while your phone sleeps.",
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun NextEvent(event: CalendarEvent, labels: TimeLabels, now: Long) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(16.dp), color = colors.surfaceContainerHigh) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(event.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_clock), null, Modifier.size(18.dp), tint = colors.onSurfaceVariant)
                Text(labels.absolute(event.start, now), style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
            }
            Text(labels.relative(event, now), style = MaterialTheme.typography.titleMedium, color = colors.primary)
            WalkingLabel(event, labels, now)
        }
    }
}

@Composable
private fun TimelineEvent(event: CalendarEvent, labels: TimeLabels, now: Long) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(labels.clock(event.start), style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(84.dp).padding(top = 3.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(event.title, style = MaterialTheme.typography.titleMedium)
            Text(labels.relative(event, now), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            WalkingLabel(event, labels, now)
        }
    }
}

@Composable
private fun WalkingLabel(event: CalendarEvent, labels: TimeLabels, now: Long) {
    event.walk?.takeIf { it.fresh(now) }?.let { estimate ->
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("~${estimate.minutes} min walk · ${event.location}", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary)
            Text("As of ${labels.clock(estimate.checkedAt)} · openrouteservice / © OpenStreetMap",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventCount(selected: Int, enabled: Boolean, onCount: (Int) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        (1..5).forEach { count ->
            val selectionColor by animateColorAsState(
                if (selected == count) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                animationSpec = tween(180), label = "Event count selection")
            SegmentedButton(selected = selected == count, onClick = { onCount(count) }, enabled = enabled,
                colors = SegmentedButtonDefaults.colors(activeContainerColor = selectionColor, inactiveContainerColor = selectionColor),
                shape = SegmentedButtonDefaults.itemShape(count - 1, 5), icon = {},
                modifier = Modifier.heightIn(min = 48.dp).semantics {
                    contentDescription = "$count ${if (count == 1) "event" else "events"}"
                }) { Text(count.toString(), style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

@Composable
private fun AgendaMessage(title: String, detail: String, action: String? = null, onAction: () -> Unit = {}) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (action != null) FilledTonalButton(onAction, modifier = Modifier.heightIn(min = 48.dp)) { Text(action) }
        }
    }
}
