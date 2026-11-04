package com.paketcast.analytics.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.paketcast.analytics.data.repository.AnalyticsState
import com.paketcast.analytics.stats.HistoryStats
import com.paketcast.analytics.stats.StatisticsEngine
import com.paketcast.analytics.ui.components.SectionHeader
import com.paketcast.analytics.ui.components.StateScaffold
import com.paketcast.analytics.ui.format.Format
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val MAX_DAYS = 30
private const val MAX_WEEKS = 12

private val PrimaryBar = Color(0xFF6750A4)
private val EmptyBar = Color(0xFFE0E0E0)
private val DayPattern = DateTimeFormatter.ofPattern("dd.MM.", Locale.GERMANY)

@Composable
fun HistoryScreen(state: AnalyticsState) {
    if (state is AnalyticsState.ApiLoaded) {
        HistoryContent(StatisticsEngine.buildHistory(state.account.history))
        return
    }
    StateScaffold(state, content = { snapshot ->
        HistoryContent(snapshot.history)
    })
}

@Composable
private fun HistoryContent(history: HistoryStats) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp),
    ) {
        if (!history.available) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Kein Hörverlauf verfügbar", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Weder die Datenbank enthält eine Verlauf-Tabelle (z. B. episode_interactions), " +
                        "noch wurden Verlaufsdaten über die API geladen.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            return
        }
        var showWeek by remember { mutableStateOf(false) }
        val daily = history.daily.takeLast(MAX_DAYS)
        val weekly = history.weekly.takeLast(MAX_WEEKS)
        val activeDays = history.daily.count { it.seconds > 0 }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SummaryChip(
                label = "Verlauf gesamt",
                value = Format.duration(history.totalSeconds) + " · ${history.eventCount} Events",
                modifier = Modifier.weight(1f),
            )
            SummaryChip(
                label = "Ø pro aktivem Tag",
                value = if (activeDays > 0) Format.duration(history.daily.sumOf { it.seconds } / activeDays) else "–",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            SegmentedButton(
                selected = !showWeek,
                onClick = { showWeek = false },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text("Tag") }
            SegmentedButton(
                selected = showWeek,
                onClick = { showWeek = true },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text("Woche") }
        }
        Spacer(Modifier.height(8.dp))
        if (showWeek) {
            HistoryChart(
                values = weekly.map { it.seconds },
                firstLabel = weekly.firstOrNull()?.weekStart?.format(DayPattern).orEmpty(),
                lastLabel = weekly.lastOrNull()?.weekStart?.format(DayPattern).orEmpty(),
                unit = "Wochen",
            )
        } else {
            HistoryChart(
                values = daily.map { it.seconds },
                firstLabel = daily.firstOrNull()?.date?.format(DayPattern).orEmpty(),
                lastLabel = daily.lastOrNull()?.date?.format(DayPattern).orEmpty(),
                unit = "Tage",
            )
        }
    }
}

@Composable
private fun SummaryChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun HistoryChart(values: List<Long>, firstLabel: String, lastLabel: String, unit: String) {
    if (values.isEmpty()) return
    val max = values.max().coerceAtLeast(1L)
    SectionHeader("Hörzeit pro $unit (letzte ${values.size}) – Maximum ${Format.duration(max)}")
    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp).padding(horizontal = 16.dp)) {
        val slot = size.width / values.size
        val barWidth = slot * 0.7f
        values.forEachIndexed { i, seconds ->
            val fraction = seconds.toFloat() / max
            val h = size.height * fraction
            drawRect(
                color = if (seconds > 0) PrimaryBar else EmptyBar,
                topLeft = Offset(i * slot + (slot - barWidth) / 2f, size.height - h),
                size = Size(barWidth, h.coerceAtLeast(1f)),
            )
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(firstLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        Text(lastLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    }
}
