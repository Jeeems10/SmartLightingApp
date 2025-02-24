package com.example.smartlightingapp.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.smartlightingapp.navigation.Screen
import com.example.smartlightingapp.viewModel.AuthViewModel
import com.example.smartlightingapp.viewModel.LightsViewModel
import com.example.smartlightingapp.widgets.LightItem

@Composable
fun DeviceListScreen(
    bottomNavController: NavController,
    mainNavController: NavController,
    lightsViewModel: LightsViewModel = viewModel(),
    authViewModel: AuthViewModel
) {
    val lights = lightsViewModel.lights.collectAsState().value
    var showDialog by remember { mutableStateOf(false) }
    var lightName by remember { mutableStateOf("") }
    var lightId by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Smart Lighting", modifier = Modifier.padding(bottom = 16.dp))

        Button(
            onClick = {
                authViewModel.logout()
                mainNavController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            },
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("Logout")
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f) // 👈 WICHTIG: Verhindert, dass der Button verschwindet
        ) {
            items(lights) { light ->
                LightItem(light, lightsViewModel) {
                    bottomNavController.navigate(Screen.DeviceDetailScreen.withId(light.id))
                }
            }
        }

        // 🔴 Hinzufügen-Button
        Button(
            onClick = { showDialog = true },
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("Licht hinzufügen")
        }

        // 🔥 Dialog für Licht hinzufügen
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Neues Licht hinzufügen") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = lightName,
                            onValueChange = { lightName = it },
                            label = { Text("Lichtname") }
                        )
                        OutlinedTextField(
                            value = lightId,
                            onValueChange = { lightId = it },
                            label = { Text("Licht ID") }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (lightId.isNotBlank() && lightName.isNotBlank()) {
                            lightsViewModel.addDevice(lightId, lightName)
                            showDialog = false
                        }
                    }) {
                        Text("Hinzufügen")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDialog = false }) {
                        Text("Abbrechen")
                    }
                }
            )
        }

    }
}
