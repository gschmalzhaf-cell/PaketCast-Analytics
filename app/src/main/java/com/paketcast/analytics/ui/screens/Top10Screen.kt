package com.paketcast.analytics.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paketcast.analytics.data.repository.AnalyticsState
import com.paketcast.analytics.ui.components.RankedPodcastRow
import com.paketcast.analytics.ui.components.SectionHeader
import com.paketcast.analytics.ui.components.StateScaffold

@Composable
fun Top10Screen(state: AnalyticsState) {
    StateScaffold(state, content = { snapshot ->
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { SectionHeader("Top 10 nach Hörzeit") }
            itemsIndexed(snapshot.top10.byListened) { index, stats ->
                RankedPodcastRow(rank = index + 1, stats = stats)
            }
            item { SectionHeader("Top 10 nach abgeschlossenen Folgen") }
            itemsIndexed(snapshot.top10.byFinished) { index, stats ->
                RankedPodcastRow(rank = index + 1, stats = stats)
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
