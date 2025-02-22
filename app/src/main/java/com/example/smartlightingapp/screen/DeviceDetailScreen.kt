package com.example.smartlightingapp.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.smartlightingapp.viewModel.MqttViewModel

@Composable
fun DeviceDetailScreen(navController: NavController, deviceId: String, mqttViewModel: MqttViewModel = viewModel()) {
    val lights by mqttViewModel.lights.collectAsState()
    val device = lights.find { it.id == deviceId }

    if (device == null) {
        Text("Gerät nicht gefunden!")
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Gerätedetails für ${device.name}")

        OutlinedTextField(
            value = device.name,
            onValueChange = { mqttViewModel.renameDevice(device.id, it) },
            label = { Text("Gerätename ändern") }
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Status: ${if (device.isOn) "Eingeschaltet" else "Ausgeschaltet"}")
            Switch(checked = device.isOn, onCheckedChange = { mqttViewModel.toggleLight(device.id) })
        }

        Slider(
            value = device.brightness.toFloat(),
            onValueChange = { mqttViewModel.setBrightness(device.id, it.toInt()) },
            valueRange = 0f..100f
        )

        Button(onClick = { navController.popBackStack() }) {
            Text("Zurück zur Übersicht")
        }
    }
}
