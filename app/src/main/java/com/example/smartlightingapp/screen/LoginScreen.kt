package com.example.smartlightingapp.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.smartlightingapp.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(authRepository: AuthRepository, onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        TextField(value = email, onValueChange = { email = it }, label = { Text("E-Mail") })
        TextField(value = password, onValueChange = { password = it }, label = { Text("Passwort") }, visualTransformation = PasswordVisualTransformation())

        Button(onClick = {
            CoroutineScope(Dispatchers.IO).launch {
                val success = authRepository.loginUser(email, password)
                if (success) onLoginSuccess()
                else error = "Fehler bei der Anmeldung"
            }
        }) {
            Text("Login")
        }

        error?.let { Text(text = it, color = Color.Red) }
    }
}
