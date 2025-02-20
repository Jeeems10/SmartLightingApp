package com.example.smartlightingapp.data

data class LightDevice(
    val id: String,
    val name: String,
    val isOn: Boolean,
    val brightness: Int
)