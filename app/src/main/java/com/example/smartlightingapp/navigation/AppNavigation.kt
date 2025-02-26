package com.example.smartlightingapp.navigation

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smartlightingapp.repository.AuthRepository
import com.example.smartlightingapp.screen.DeviceDetailScreen
import com.example.smartlightingapp.screen.DeviceListScreen
import com.example.smartlightingapp.screen.LoginScreen
import com.example.smartlightingapp.viewModel.AuthViewModel
import com.example.smartlightingapp.viewModel.AuthViewModelFactory
import com.example.smartlightingapp.viewModel.LightsViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authRepository = AuthRepository()
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(authRepository))
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    if (isLoggedIn == null) {
        CircularProgressIndicator()  // Ladebildschirm
    } else {
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn == true) "main" else "login"
        ) {
            composable("login") {
                LoginScreen(navController, authRepository)
            }

            // For authenticated area, create a new LightsViewModel instance here.
            composable("main") {
                // Create a new LightsViewModel scoped to this "main" route.
                val lightsViewModel: LightsViewModel = viewModel()
                BottomNavContainerScreen(navController, lightsViewModel, authViewModel)
            }

            composable(
                route = "device_detail/{deviceId}",
                arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
            ) { backStackEntry ->
                val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable
                DeviceDetailScreen(navController, deviceId, /* pass LightsViewModel if needed */)
            }
        }
    }
}

