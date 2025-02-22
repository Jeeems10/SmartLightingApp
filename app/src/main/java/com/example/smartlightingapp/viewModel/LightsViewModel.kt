package com.example.smartlightingapp.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartlightingapp.model.LightDevice
import com.example.smartlightingapp.repository.LightsRepository
import com.example.smartlightingapp.repository.MqttRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LightsViewModel : ViewModel() {
    // Updated MQTT broker address (Mac IP 192.168.0.53) with messageCallback provided
    private val mqttRepository = MqttRepository("tcp://192.168.0.53:1883", "SmartLightingApp") { deviceId, message ->
        updateDeviceState(deviceId, message)
    }
    private val lightsRepository = LightsRepository()

    // Store list of registered devices
    private val _lights = MutableStateFlow<List<LightDevice>>(emptyList())
    val lights = _lights.asStateFlow()

    // New: store list of discovered (but not yet added) devices
    private val _discoveredDevices = MutableStateFlow<List<LightDevice>>(emptyList())
    val discoveredDevices = _discoveredDevices.asStateFlow()

    init {
        viewModelScope.launch {
            // Load registered devices from Firestore
            val savedLights = lightsRepository.getAllLights()
            _lights.value = savedLights

            // For each registered device, subscribe to its MQTT topic and request its status
            savedLights.forEach { device ->
                mqttRepository.subscribe("stat/${device.id}/RESULT") { message ->
                    updateDeviceState(device.id, message)
                }
                mqttRepository.requestDeviceStatus(device.id)
            }
        }
        // Real-time Firestore listener to update UI immediately
        lightsRepository.lightsCollection.addSnapshotListener { snapshot, e ->
            if (e != null) {
                println("Firestore Fehler: ${e.message}")
                return@addSnapshotListener
            }
            snapshot?.documents?.mapNotNull { doc ->
                val id = doc.getString("id") ?: return@mapNotNull null
                val name = doc.getString("name") ?: "Unbekannt"
                val isOn = doc.getBoolean("isOn") ?: false
                val brightness = doc.getLong("brightness")?.toInt() ?: 50
                LightDevice(id, name, isOn, brightness)
            }?.let { newLights ->
                println("🔥 Firestore Update: $newLights")
                _lights.value = newLights
            }
        }
    }

    // Function to start discovery of ESP devices
    fun startDeviceDiscovery() {
        viewModelScope.launch {
            // Clear previous discoveries
            _discoveredDevices.value = emptyList()
            // Subscribe to discovery topic
            mqttRepository.subscribe("lights/discovery") { message ->
                val parts = message.split(":")
                if (parts.size == 2) {
                    val deviceId = parts[0]
                    val ipAddress = parts[1]
                    // Create a discovered device (default name "ESP Device")
                    val device = LightDevice(deviceId, "ESP Device", false, 50)
                    // Add device if not already in the list
                    if (_discoveredDevices.value.none { it.id == deviceId }) {
                        _discoveredDevices.value = _discoveredDevices.value + device
                    }
                }
            }
            // Publish discovery request so ESP devices announce themselves
            mqttRepository.publishMessage("lights/discovery/request", "discover")
            // Wait 10 seconds to gather responses
            delay(10000)
            mqttRepository.unsubscribe("lights/discovery")
        }
    }

    // Register a discovered device (add it to Firestore and to the registered list)
    fun addDevice(id: String, name: String) {
        viewModelScope.launch {
            val success = lightsRepository.addLight(id, name)
            if (success) {
                val updatedLights = _lights.value.toMutableList()
                updatedLights.add(LightDevice(id, name, false, 50))
                _lights.value = updatedLights
            } else {
                println("Fehler: Konnte Gerät nicht zu Firestore hinzufügen!")
            }
        }
    }

    // Existing functions…

    fun removeDevice(id: String) {
        viewModelScope.launch {
            val success = lightsRepository.removeLight(id)
            if (success) {
                val updatedLights = _lights.value.toMutableList()
                updatedLights.removeAll { it.id == id }
                _lights.value = updatedLights
            } else {
                println("Fehler: Konnte Gerät nicht aus Firestore entfernen!")
            }
        }
    }

    fun updateDevice(id: String, name: String, isOn: Boolean, brightness: Int) {
        viewModelScope.launch {
            lightsRepository.updateLight(id, name, isOn, brightness)
        }
    }

    fun renameDevice(id: String, newName: String) {
        viewModelScope.launch {
            val updatedLights = _lights.value.toMutableList()
            val deviceIndex = updatedLights.indexOfFirst { it.id == id }
            if (deviceIndex != -1) {
                updatedLights[deviceIndex] = updatedLights[deviceIndex].copy(name = newName)
                _lights.value = updatedLights
                lightsRepository.updateLight(id, name = newName, isOn = null, brightness = null)
            }
        }
    }

    private fun updateDeviceState(deviceId: String, message: String) {
        println("DEBUG: Update für $deviceId mit Nachricht: $message")
        viewModelScope.launch {
            val updatedLights = _lights.value.toMutableList()
            val existingLight = updatedLights.find { it.id == deviceId }
            val newLight = LightDevice(
                id = deviceId,
                name = existingLight?.name ?: "Licht $deviceId",
                isOn = message.contains("\"POWER\":\"ON\""),
                brightness = extractBrightness(message) ?: existingLight?.brightness ?: 50
            )
            if (existingLight != null) {
                updatedLights[updatedLights.indexOf(existingLight)] = newLight
            } else {
                updatedLights.add(newLight)
            }
            _lights.value = updatedLights
            lightsRepository.updateLight(
                id = deviceId,
                isOn = newLight.isOn,
                name = null,
                brightness = newLight.brightness
            )
        }
    }

    fun toggleLight(id: String) {
        viewModelScope.launch {
            val light = _lights.value.find { it.id == id } ?: return@launch
            val newState = !light.isOn
            lightsRepository.updateLight(id, isOn = newState, name = null, brightness = null)
            mqttRepository.publishMessage("cmnd/$id/Power", if (newState) "ON" else "OFF")
        }
    }

    fun setBrightness(id: String, brightness: Int) {
        viewModelScope.launch {
            mqttRepository.publishMessage("cmnd/$id/Dimmer", brightness.toString())
        }
    }

    override fun onCleared() {
        super.onCleared()
        mqttRepository.disconnect()
    }

    private fun extractBrightness(message: String): Int? {
        val regex = """"Dimmer":(\d+)""".toRegex()
        val matchResult = regex.find(message)
        return matchResult?.groups?.get(1)?.value?.toIntOrNull()
    }
}