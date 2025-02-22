package com.example.smartlightingapp.widgets

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smartlightingapp.model.LightDevice
import com.example.smartlightingapp.viewModel.LightsViewModel

@Composable
fun LightControl(light: LightDevice, lightsViewModel: LightsViewModel) {
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Row {
            Text("Status: ${if (light.isOn) "Ein" else "Aus"}", modifier = Modifier.weight(1f))
            Switch(checked = light.isOn, onCheckedChange = { lightsViewModel.toggleLight(light.id) })
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Helligkeit: ${light.brightness}%")
        Slider(
            value = light.brightness.toFloat(),
            onValueChange = { lightsViewModel.setBrightness(light.id, it.toInt()) },
            valueRange = 0f..100f
        )
    }
}