package com.paketcast.analytics.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paketcast.analytics.data.repository.AnalyticsState
import com.paketcast.analytics.ui.components.EpisodeRow
import com.paketcast.analytics.ui.components.PodcastStatRow
import com.paketcast.analytics.ui.components.SectionHeader
import com.paketcast.analytics.ui.components.StatCard
import com.paketcast.analytics.ui.components.StateScaffold
import com.paketcast.analytics.ui.format.Format

@Composable
fun RemainingScreen(state: AnalyticsState) {
    StateScaffold(state) { snapshot ->
        val remaining = snapshot.remaining
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    StatCard(
                        title = "Noch zu hören (gesamt)",
                        value = Format.duration(remaining.totalRemainingSec),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    StatCard(
                        title = "Offene Folgen",
                        value = "${remaining.openEpisodeCount}",
                        subtitle = "nicht abgeschlossen, nicht archiviert",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item { SectionHeader("Noch offen pro Podcast") }
            items(remaining.byPodcast, key = { it.podcast.uuid }) { PodcastStatRow(it) }
            item { SectionHeader("Offene Folgen (neueste zuerst)") }
            items(remaining.episodes, key = { it.episode.uuid }) { EpisodeRow(it) }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
