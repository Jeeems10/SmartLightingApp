package com.example.smartlightingapp.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

    // 🎯 Name als State speichern, damit er bearbeitet werden kann
    var deviceName by remember { mutableStateOf(device.name) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Gerätedetails für $deviceName")

        // 🔥 Textfeld für den Gerätenamen
        OutlinedTextField(
            value = deviceName,
            onValueChange = { newName -> deviceName = newName }, // UI aktualisieren
            label = { Text("Gerätename ändern") },
            singleLine = true
        )

        // 🌟 Button zum Speichern des neuen Namens
        Button(
            onClick = { mqttViewModel.renameDevice(device.id, deviceName) },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Namen speichern")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🔥 Status und Schalter für AN/AUS
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Status: ${if (device.isOn) "Eingeschaltet" else "Ausgeschaltet"}")
            Switch(
                checked = device.isOn,
                onCheckedChange = { mqttViewModel.toggleLight(device.id) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🔥 Slider für Helligkeit
        Text("Helligkeit: ${device.brightness}%")
        Slider(
            value = device.brightness.toFloat(),
            onValueChange = { mqttViewModel.setBrightness(device.id, it.toInt()) },
            valueRange = 0f..100f
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 🔙 Zurück zur Übersicht
        Button(onClick = { navController.popBackStack() }) {
            Text("Zurück zur Übersicht")
        }
    }
}
