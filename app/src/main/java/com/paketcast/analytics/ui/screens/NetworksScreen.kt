package com.paketcast.analytics.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paketcast.analytics.data.repository.AnalyticsState
import com.paketcast.analytics.stats.NetworkStats
import com.paketcast.analytics.ui.components.SectionHeader
import com.paketcast.analytics.ui.components.StateScaffold
import com.paketcast.analytics.ui.format.Format

@Composable
fun NetworksScreen(state: AnalyticsState) {
    StateScaffold(state, content = { snapshot ->
        val maxListened = snapshot.networks.maxOfOrNull { it.listenedSec }?.coerceAtLeast(1L) ?: 1L
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { SectionHeader("${snapshot.networks.size} Podcast-Netzwerke – sortiert nach Hörzeit") }
            items(snapshot.networks, key = { it.network }) { network ->
                NetworkRow(network, maxListened)
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun NetworkRow(network: NetworkStats, maxListenedSec: Long) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(network.network, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                "${network.podcastCount} Podcasts • Gehört: ${Format.duration(network.listenedSec)} • " +
                    "${network.finishedEpisodes} abgeschlossene Folgen",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (network.listenedSec.toDouble() / maxListenedSec).toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
