package com.example.smartlightingapp.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smartlightingapp.DeviceListScreen
import com.example.smartlightingapp.screen.DeviceDetailScreen
import com.example.smartlightingapp.viewModel.LightsViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val lightsViewModel: LightsViewModel = viewModel()

    NavHost(navController = navController, startDestination = "device_list") {
        composable("device_list") { DeviceListScreen(navController, lightsViewModel) }

        composable(
            "device_detail/{deviceId}",
            arguments = listOf(navArgument("deviceId"){type = NavType.StringType})
            ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable
            DeviceDetailScreen(navController, deviceId, lightsViewModel)
        }
    }
}