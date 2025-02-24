package com.example.smartlightingapp.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.smartlightingapp.navigation.Screen
import com.example.smartlightingapp.viewModel.AuthViewModel
import com.example.smartlightingapp.viewModel.ConnectivityViewModel
import com.example.smartlightingapp.viewModel.LightsViewModel
import com.example.smartlightingapp.widgets.LightItem
import kotlinx.coroutines.launch

@Composable
fun DeviceListScreen(
    bottomNavController: NavController,
    mainNavController: NavController,
    lightsViewModel: LightsViewModel = viewModel(),
    authViewModel: AuthViewModel,
    connectivityViewModel: ConnectivityViewModel = viewModel()
) {
    val lights = lightsViewModel.lights.collectAsState().value
    var showDialog by remember { mutableStateOf(false) }
    var lightName by remember { mutableStateOf("") }
    var lightId by remember { mutableStateOf("") }

    val isConnected by connectivityViewModel.isConnected.collectAsState()
    val isMqttConnected by lightsViewModel.mqttConnected.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Smart Lighting", modifier = Modifier.padding(bottom = 16.dp))

            // 🔴 Logout-Button
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

            // 🔴 Warnungen für Verbindung
            if (!isConnected) {
                Text("⚠ Keine Internetverbindung!", color = MaterialTheme.colorScheme.error)
            }
            if (!isMqttConnected) {
                Text("⚠ MQTT-Server nicht erreichbar!", color = MaterialTheme.colorScheme.error)
            }

            // 🔴 Geräte Liste
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f) // 👈 Verhindert, dass der Button verschwindet
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
                            if (lightId.isBlank() || lightName.isBlank()) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("⚠ Bitte alle Felder ausfüllen!")
                                }
                            } else if (!isConnected) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("⚠ Keine Internetverbindung!")
                                }
                            } else {
                                lightsViewModel.addDevice(lightId, lightName)
                                showDialog = false
                                lightId = ""
                                lightName = ""
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
}
