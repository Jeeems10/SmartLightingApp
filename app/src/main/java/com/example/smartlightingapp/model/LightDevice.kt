package com.example.smartlightingapp.model

data class LightDevice(
    val id: String,
    var name: String,
    val isOn: Boolean,
    val brightness: Int
)