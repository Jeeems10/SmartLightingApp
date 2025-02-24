package com.example.smartlightingapp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Devices : BottomNavItem("device_list", Icons.Filled.Home, "Geräte")
    object Config : BottomNavItem("config", Icons.Filled.Settings, "Config")
}