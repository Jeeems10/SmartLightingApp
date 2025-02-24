package com.example.smartlightingapp.model

data class LightDevice(
    val id: String,
    var name: String,
    val isOn: Boolean,
    val isOnline: Boolean, // 🔥 Neu: Online-Status für Rot/Grün-Anzeige
    val brightness: Int
)