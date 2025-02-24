package com.example.smartlightingapp.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartlightingapp.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _isLoggedIn = MutableStateFlow<Boolean?>(null)  // Anfangszustand: unbekannt
    val isLoggedIn = _isLoggedIn.asStateFlow()

    init {
        viewModelScope.launch {
            _isLoggedIn.value = authRepository.getCurrentUser() != null
        }
    }


    // Login mit Callback
    fun login(email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = authRepository.loginUser(email, password)
            _isLoggedIn.value = success
            onResult(success) // Callback aufrufen
        }
    }

    // Registrierung mit Callback
    fun register(email: String, password: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = authRepository.registerUser(email, password)
            _isLoggedIn.value = success
            onResult(success) // Callback aufrufen
        }
    }

    fun logout() {
        authRepository.logoutUser()
        _isLoggedIn.value = false
    }

}

