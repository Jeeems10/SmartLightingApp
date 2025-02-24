package com.example.smartlightingapp.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.smartlightingapp.viewModel.LightsViewModel
import com.example.smartlightingapp.widgets.LightControl

@Composable
fun DeviceDetailScreen(
    navController: NavController,
    deviceId: String,
    lightsViewModel: LightsViewModel = viewModel()
) {
    val lights by lightsViewModel.lights.collectAsState()
    val device = lights.find { it.id == deviceId }

    if (device == null) {
        Text("Gerät nicht gefunden!")
        return
    }

    // Store device name as state for editing
    var deviceName by remember { mutableStateOf(device.name) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Gerätedetails für $deviceName")

        // Text field to edit device name
        OutlinedTextField(
            value = deviceName,
            onValueChange = { newName -> deviceName = newName },
            label = { Text("Gerätename ändern") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Button to save new device name
        Button(
            onClick = { lightsViewModel.renameDevice(device.id, deviceName) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Namen speichern")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LightControl(light = device, lightsViewModel = lightsViewModel)

        Spacer(modifier = Modifier.height(16.dp))

        // Button to remove the device
        Button(
            onClick = {
                lightsViewModel.removeDevice(device.id)
                navController.popBackStack()  // Return to device list after removal
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Gerät entfernen", color = MaterialTheme.colorScheme.onError)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Button to go back to the list
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Zurück zur Übersicht")
        }
    }
}