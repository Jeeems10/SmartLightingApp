package com.example.smartlightingapp.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.smartlightingapp.viewModel.AuthViewModel
import com.example.smartlightingapp.viewModel.LightsViewModel
import com.example.smartlightingapp.widgets.LightItem

@Composable
fun DeviceListScreen(
    navController: NavController,
    lightsViewModel: LightsViewModel = viewModel(),
    authViewModel: AuthViewModel
) {
    val lights = lightsViewModel.lights.collectAsState().value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Smart Lighting", modifier = Modifier.padding(bottom = 16.dp))

        Button(
            onClick = {
                authViewModel.logout()
                navController.navigate("login") {
                    popUpTo("device_list") { inclusive = true }
                }
            },
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("Logout")
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f) // 👈 WICHTIG: Verhindert, dass der Button verschwindet
        ) {
            items(lights) { light ->
                LightItem(light, lightsViewModel) {
                    navController.navigate("device_detail/${light.id}")
                }
            }
        }
        // Logout-Button bleibt fix oben

    }
}
