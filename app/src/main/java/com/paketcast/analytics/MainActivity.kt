package com.paketcast.analytics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.paketcast.analytics.ui.PaketCastApp
import com.paketcast.analytics.ui.theme.PaketCastTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PaketCastTheme {
                PaketCastApp()
            }
        }
    }
}
