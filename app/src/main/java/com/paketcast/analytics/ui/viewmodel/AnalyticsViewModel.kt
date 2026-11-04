package com.paketcast.analytics.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paketcast.analytics.data.repository.AnalyticsRepository
import com.paketcast.analytics.data.repository.AnalyticsState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AnalyticsRepository(application)

    val state: StateFlow<AnalyticsState> = repository.state

    fun importDatabase(uri: Uri) {
        viewModelScope.launch { repository.importDatabase(uri) }
    }

    fun importOpml(uri: Uri) {
        viewModelScope.launch { repository.importOpml(uri) }
    }

    fun connectAccount(email: String, password: String) {
        viewModelScope.launch { repository.connectAccount(email, password) }
    }
}
