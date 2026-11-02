package com.paketcast.analytics.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.paketcast.analytics.data.repository.AnalyticsState
import com.paketcast.analytics.ui.screens.EpisodesScreen
import com.paketcast.analytics.ui.screens.NetworksScreen
import com.paketcast.analytics.ui.screens.OverviewScreen
import com.paketcast.analytics.ui.screens.PodcastsScreen
import com.paketcast.analytics.ui.screens.RemainingScreen
import com.paketcast.analytics.ui.screens.Top10Screen
import com.paketcast.analytics.ui.viewmodel.AnalyticsViewModel

data class Screen(val route: String, val label: String, val icon: ImageVector)

private val screens = listOf(
    Screen("overview", "Start", Icons.Default.Dashboard),
    Screen("podcasts", "Podcasts", Icons.Default.Audiotrack),
    Screen("top10", "Top 10", Icons.Default.Star),
    Screen("networks", "Netzwerke", Icons.Default.Cloud),
    Screen("episodes", "Episoden", Icons.Default.BarChart),
    Screen("remaining", "Offen", Icons.Default.Schedule),
)

private const val DB_MIME_TYPES = "application/octet-stream,application/x-sqlite3,application/vnd.sqlite3,*/*"
private const val OPML_MIME_TYPES = "text/x-opml,text/xml,application/xml,text/*,*/*"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaketCastApp(viewModel: AnalyticsViewModel = viewModel()) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val state by viewModel.state.collectAsStateWithLifecycle()

    val dbPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importDatabase(it) }
    }
    val opmlPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importOpml(it) }
    }
    var showImportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screens.firstOrNull { it.route == currentRoute }?.label ?: "PaketCast Analytics") },
                actions = {
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Daten importieren")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = screens.first().route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("overview") {
                OverviewScreen(
                    state = state,
                    onImportDatabase = { dbPicker.launch(arrayOf(*DB_MIME_TYPES.split(",").toTypedArray())) },
                    onImportOpml = { opmlPicker.launch(arrayOf(*OPML_MIME_TYPES.split(",").toTypedArray())) },
                )
            }
            composable("podcasts") { PodcastsScreen(state) }
            composable("top10") { Top10Screen(state) }
            composable("networks") { NetworksScreen(state) }
            composable("episodes") { EpisodesScreen(state) }
            composable("remaining") { RemainingScreen(state) }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Pocket-Casts-Daten importieren") },
            text = {
                Column {
                    Text("Alle Daten bleiben auf diesem Gerät – es wird nichts hochgeladen.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            showImportDialog = false
                            dbPicker.launch(arrayOf(*DB_MIME_TYPES.split(",").toTypedArray()))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Datenbank wählen (pocketcasts.db)")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            showImportDialog = false
                            opmlPicker.launch(arrayOf(*OPML_MIME_TYPES.split(",").toTypedArray()))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("OPML-Export wählen")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Schließen") }
            },
        )
    }
}
