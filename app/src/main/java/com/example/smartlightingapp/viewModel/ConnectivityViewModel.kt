package com.example.smartlightingapp.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartlightingapp.widgets.NetworkStatusHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConnectivityViewModel(application: Application) : AndroidViewModel(application) {
    private val networkHelper = NetworkStatusHelper(application)

    private val _isConnected = MutableStateFlow(true)
    val isConnected = _isConnected.asStateFlow()

    init {
        viewModelScope.launch {
            networkHelper.isConnected.collect { status ->
                _isConnected.value = status
            }
        }
    }
}
