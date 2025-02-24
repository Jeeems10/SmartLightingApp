package com.example.smartlightingapp.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.smartlightingapp.repository.AuthRepository
import com.example.smartlightingapp.viewModel.AuthViewModel
import com.example.smartlightingapp.viewModel.AuthViewModelFactory

@Composable
fun LoginScreen(
    navController: NavController,
    authRepository: AuthRepository
) {
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(authRepository))

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isRegisterMode by remember { mutableStateOf(false) } // Umschalten zwischen Login & Registrierung

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(if (isRegisterMode) "Registrieren" else "Login", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-Mail") }
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Passwort") },
            visualTransformation = PasswordVisualTransformation()
        )

        Button(
            onClick = {
                if (isRegisterMode) {
                    authViewModel.register(email, password)
                } else {
                    authViewModel.login(email, password) { success ->
                        if (success) {
                            println("Login erfolgreich, navigiere zur Geräteliste")
                            navController.navigate("main") {
                                popUpTo("login") { inclusive = true } // Verhindert Zurück-Navigation zum Login
                            }
                        } else {
                            error = "Fehler bei der Anmeldung"
                        }
                    }
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(if (isRegisterMode) "Registrieren" else "Login")
        }


        error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = { isRegisterMode = !isRegisterMode }
        ) {
            Text(if (isRegisterMode) "Zum Login wechseln" else "Zur Registrierung wechseln")
        }
    }
}

