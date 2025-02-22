package com.example.smartlightingapp.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartlightingapp.data.LightDevice
import com.example.smartlightingapp.ui.theme.SmartLightingAppTheme
import com.example.smartlightingapp.viewModel.MqttViewModel



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartLightingAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen()
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    mqttViewModel: MqttViewModel = viewModel()
) {
    val lights by mqttViewModel.lights.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Smart Lighting")

        // Eingabefeld für neues Gerät
        var newDeviceName by remember { mutableStateOf("") }
        var newDeviceId by remember { mutableStateOf("") }

        OutlinedTextField(
            value = newDeviceId,
            onValueChange = { newDeviceId = it },
            label = { Text("Geräte-ID") }
        )

        OutlinedTextField(
            value = newDeviceName,
            onValueChange = { newDeviceName = it },
            label = { Text("Gerätename") }
        )

        Button(onClick = {
            if (newDeviceId.isNotEmpty() && newDeviceName.isNotEmpty()) {
                mqttViewModel.addDevice(newDeviceId, newDeviceName)
                newDeviceId = ""
                newDeviceName = ""
            }
        }) {
            Text("Gerät hinzufügen")
        }

        LazyColumn {
            items(lights) { light ->
                LightItem(
                    light = light,
                    onToggle = { mqttViewModel.toggleLight(light.id) },
                    onBrightnessChange = { mqttViewModel.setBrightness(light.id, it) },
                    onRemove = { mqttViewModel.removeDevice(light.id) },
                    onRename = { newName -> mqttViewModel.renameDevice(light.id, newName) }
                )
            }
        }
    }
}

@Composable
fun LightItem(light: LightDevice, onToggle: () -> Unit, onBrightnessChange: (Int) -> Unit, onRemove: () -> Unit, onRename: (String) -> Unit) {
    var newName by remember { mutableStateOf(light.name) }

    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Gerätename") }
            )
            Button(onClick = { onRename(newName) }) { Text("Umbenennen") }
            Button(onClick = { onRemove() }) { Text("Löschen") }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = light.name)
            Spacer(modifier = Modifier.width(8.dp))
            Switch(checked = light.isOn, onCheckedChange = { onToggle() })
        }

        Slider(
            value = light.brightness.toFloat(),
            onValueChange = { onBrightnessChange(it.toInt()) },
            valueRange = 0f..100f
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
}