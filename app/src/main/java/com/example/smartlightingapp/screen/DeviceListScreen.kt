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
import com.example.smartlightingapp.viewModel.LightsViewModel
import com.example.smartlightingapp.widgets.LightItem

@Composable
fun DeviceListScreen(
    navController: NavController,
    lightsViewModel: LightsViewModel = viewModel()
) {
    val lights by lightsViewModel.lights.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Smart Lighting")
        LazyColumn {
            items(lights) { light ->
                LightItem(light, lightsViewModel) {
                    navController.navigate("device_detail/${light.id}")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Navigate to discovery screen
        Button(
            onClick = { navController.navigate("device_discovery") },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(text = "Register ESP Device")
        }
    }
}