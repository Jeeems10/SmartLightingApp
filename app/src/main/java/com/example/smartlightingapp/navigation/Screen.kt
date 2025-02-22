package com.example.smartlightingapp.navigation

const val DEVICE_ID_ARGUMENT = "deviceId"

sealed class Screen(val route: String) {
    object DeviceListScreen : Screen("device_list")
    object DeviceDetailScreen : Screen("device_detail/{$DEVICE_ID_ARGUMENT}") {
        fun withId(id: String): String = "device_detail/$id"
    }
    object LoginScreen : Screen("login")
}
