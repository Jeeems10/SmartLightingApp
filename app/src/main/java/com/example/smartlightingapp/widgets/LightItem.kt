package com.example.smartlightingapp.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.smartlightingapp.model.LightDevice
import com.example.smartlightingapp.viewModel.LightsViewModel

@Composable
fun LightItem(light: LightDevice, lightsViewModel: LightsViewModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Light Icon",
                    tint = if (light.isOn) Color.Yellow else Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text(text = light.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (light.isOnline) "Online" else "Offline",
                        color = if (light.isOnline) Color.Green else Color.Red,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = light.isOn,
                    onCheckedChange = {
                        lightsViewModel.toggleLight(light.id)
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = light.brightness.toFloat(),
                onValueChange = { newBrightness ->
                    // Update the state without sending an MQTT command
                    lightsViewModel.updateBrightnessLocally(light.id, newBrightness.toInt())
                },
                onValueChangeFinished = {
                    // When the user stops sliding, apply the brightness if the light is on.
                    lightsViewModel.applyBrightness(light.id)
                },
                valueRange = 0f..100f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
