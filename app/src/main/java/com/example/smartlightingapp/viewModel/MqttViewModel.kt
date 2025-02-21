package com.example.smartlightingapp.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartlightingapp.data.LightDevice
import com.example.smartlightingapp.data.MqttManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class MqttViewModel: ViewModel() {
    private val mqttManager = MqttManager("tcp://192.168.0.67:1883", "SmartLightingApp"){
            deviceId, message ->
        updateDeviceState(deviceId, message) // Hier wird updateDeviceState() aufgerufen
    }

    // 🌟 Liste aller Lichter speichern
    private val _lights = MutableStateFlow<List<LightDevice>>(emptyList())
    val lights = _lights.asStateFlow()


    // 🌟 MQTT-Subscription für mehrere Geräte
    init {
        listOf("D1Mini_1", "D1Mini_2").forEach { deviceId ->
            println("DEBUG: MQTT-Subscription für $deviceId gestartet") // Debugging
            mqttManager.subscribe("stat/$deviceId/RESULT") { message ->
                println("DEBUG: Nachricht empfangen für $deviceId -> $message") // Debugging
                updateDeviceState(deviceId, message)
            }
            mqttManager.requestDeviceStatus(deviceId) // 🎯 Direkt nach Start den Status abrufen

        }
    }

    // 🌟 Geräteliste aktualisieren
    private fun updateDeviceState(deviceId: String, message: String) {
        println("DEBUG: Update für $deviceId mit Nachricht: $message") // Debugging
        viewModelScope.launch {
            val updatedLights = _lights.value.toMutableList()
            val existingLight = updatedLights.find { it.id == deviceId }

            val newLight = LightDevice(
                id = deviceId,
                name = "Licht $deviceId",
                isOn = message.contains("\"POWER\":\"ON\""),
                brightness = extractBrightness(message) ?: existingLight?.brightness ?: 50
            )

            if (existingLight != null) {
                updatedLights[updatedLights.indexOf(existingLight)] = newLight
            } else {
                updatedLights.add(newLight)
            }

            _lights.value = updatedLights
        }
    }

    // 🌟 Licht togglen
    fun toggleLight(id: String) {
        viewModelScope.launch {
            val light = _lights.value.find { it.id == id } ?: return@launch
            mqttManager.publishMessage("cmnd/$id/Power", if (light.isOn) "OFF" else "ON")
        }
    }

    fun setBrightness(id: String, brightness: Int) {
        viewModelScope.launch {
            mqttManager.publishMessage("cmnd/$id/Dimmer", brightness.toString())
        }
    }

    override fun onCleared() {
        super.onCleared()
        mqttManager.disconnect()
    }

    /** Extrahiert den Dimmer-Wert aus der MQTT-Nachricht **/
    private fun extractBrightness(message: String): Int? {
        val regex = """"Dimmer":(\d+)""".toRegex()
        val matchResult = regex.find(message)
        return matchResult?.groups?.get(1)?.value?.toIntOrNull()
    }
}