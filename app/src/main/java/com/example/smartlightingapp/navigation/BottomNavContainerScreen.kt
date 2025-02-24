package com.example.smartlightingapp.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.example.smartlightingapp.repository.AuthRepository
import com.example.smartlightingapp.screen.ConfigScreen
import com.example.smartlightingapp.screen.DeviceDetailScreen
import com.example.smartlightingapp.screen.DeviceListScreen
import com.example.smartlightingapp.screen.LoginScreen
import com.example.smartlightingapp.viewModel.AuthViewModel
import com.example.smartlightingapp.viewModel.LightsViewModel
import com.example.smartlightingapp.widgets.BottomNavigationBar

@Composable
fun BottomNavContainerScreen(mainNavController: NavController, lightsViewModel: LightsViewModel, authViewModel: AuthViewModel) {
    val bottomNavController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavigationBar(bottomNavController) }  // ⬅ Hier binden wir die BottomNavigationBar ein
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavHost(
                navController = bottomNavController,
                startDestination = BottomNavItem.Devices.route
            ) {
                composable("login") {
                    LoginScreen(mainNavController, AuthRepository()) // Falls AuthRepository notwendig ist
                }

                composable(BottomNavItem.Devices.route) {
                    DeviceListScreen(mainNavController,bottomNavController, lightsViewModel, authViewModel)
                }
                composable(BottomNavItem.Config.route) {
                    ConfigScreen()
                }
                composable("device_detail/{deviceId}") { backStackEntry ->
                    val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable
                    DeviceDetailScreen(mainNavController, deviceId, lightsViewModel)
                }
            }
        }
    }
}
