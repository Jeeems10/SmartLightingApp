package com.example.smartlightingapp.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smartlightingapp.model.LightDevice
import com.example.smartlightingapp.viewModel.LightsViewModel

@Composable
fun LightItem(
    light: LightDevice,
    lightsViewModel: LightsViewModel,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = light.name, modifier = Modifier.weight(1f))
        Switch(
            checked = light.isOn,
            onCheckedChange = {
                lightsViewModel.toggleLight(light.id)
            }
        )
    }
}