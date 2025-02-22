package com.example.smartlightingapp.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.smartlightingapp.viewModel.LightsViewModel
import com.example.smartlightingapp.model.LightDevice

@Composable
fun DeviceDiscoveryScreen(
    navController: NavController,
    lightsViewModel: LightsViewModel
) {
    // Start discovery when entering the screen
    LaunchedEffect(Unit) {
        lightsViewModel.startDeviceDiscovery()
    }
    // Collect discovered devices from the view model
    val discoveredDevices by lightsViewModel.discoveredDevices.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Discovered ESP Devices",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (discoveredDevices.isEmpty()) {
            Text("No devices found. Please try again.")
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(discoveredDevices) { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        // Display device info
                        Text(
                            text = "${device.id} (${device.name})",
                            modifier = Modifier.weight(1f)
                        )
                        // Button to add the device
                        Button(onClick = {
                            lightsViewModel.addDevice(device.id, device.name)
                        }) {
                            Text("Add")
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Back button to return to the device list
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}