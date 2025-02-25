package com.example.smartlightingapp.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartlightingapp.repository.LightsRepository
import com.example.smartlightingapp.model.LightDevice
import com.example.smartlightingapp.repository.MqttRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class LightsViewModel : ViewModel() {

    private val _mqttConnected = MutableStateFlow(true)
    val mqttConnected = _mqttConnected.asStateFlow()

    private val mqttRepository = MqttRepository(
        brokerUrl = "tcp://172.20.10.5:1883",
        clientId = "SmartLightingApp",
        messageCallback = { deviceId, message -> updateDeviceState(deviceId, message) },
        connectionLostCallback = {
            viewModelScope.launch {
                onMqttDisconnected()
            }
        },
        connectionEstablishedCallback = {
            viewModelScope.launch {
                _mqttConnected.value = true
            }
        }
    )


    private val lightsRepository = LightsRepository()

    private val _discoveredDevices = MutableStateFlow<List<LightDevice>>(emptyList())
    val discoveredDevices = _discoveredDevices.asStateFlow()


    // 🌟 Liste aller Lichter speichern
    private val _lights = MutableStateFlow<List<LightDevice>>(emptyList())
    val lights = _lights.asStateFlow()

    private val onlineStatusTimeout = mutableMapOf<String, Long>()


    // 🌟 MQTT-Subscription für mehrere Geräte
    init {
        viewModelScope.launch {
            val savedLights = lightsRepository.getAllLights()
            _lights.value = savedLights  // Firestore-Lichter laden

            // Nach dem Laden der Geräte -> MQTT abonnieren
            savedLights.forEach { device ->
                // Subscribe to the device's LWT topic to get online/offline updates
                mqttRepository.subscribe("tele/${device.id}/LWT") { message ->
                    updateOnlineStatus(device.id, message)
                }

                mqttRepository.subscribe("tele/${device.id}/HEARTBEAT") { message ->
                    updateOnlineStatusWithHeartbeat(device.id, message)
                }
                // Optionally, you can still send a status request if needed, but ensure the device responds to it.
                mqttRepository.requestDeviceStatus(device.id)
            }
        }
        // 🔥 Echtzeit-Listener für Firestore (damit UI sofort aktualisiert wird)
        lightsRepository.lightsCollection.addSnapshotListener { snapshot, e ->
            if (e != null) {
                println("Firestore Fehler: ${e.message}")
                return@addSnapshotListener
            }

            snapshot?.documents?.mapNotNull { doc ->
                val id = doc.getString("id") ?: return@mapNotNull null
                val name = doc.getString("name") ?: "Unbekannt"
                val isOn = doc.getBoolean("isOn") ?: false
                val isOnline = doc.getBoolean("isOnline")?:false
                val brightness = doc.getLong("brightness")?.toInt() ?: 50

                LightDevice(id, name, isOn,isOnline, brightness)
            }?.let { newLights ->
                println("🔥 Firestore Update: $newLights")
                _lights.value = newLights
            }
        }

        startOfflineWatcher()
    }

    private fun onMqttDisconnected() {
        _mqttConnected.value = false
    }

    private fun updateOnlineStatusWithHeartbeat(deviceId: String, message: String) {
        // You can also check the message content if you need it.
        viewModelScope.launch {
            // Mark the device as online immediately when a heartbeat arrives.
            onlineStatusTimeout[deviceId] = System.currentTimeMillis()

            // Update the device's online state if it isn’t already online.
            val updatedLights = _lights.value.toMutableList()
            val index = updatedLights.indexOfFirst { it.id == deviceId }
            if (index != -1 && !updatedLights[index].isOnline) {
                val device = updatedLights[index]
                updatedLights[index] = device.copy(isOnline = true)
                _lights.value = updatedLights
                lightsRepository.updateLight(
                    id = deviceId,
                    isOn = device.isOn,
                    name = null,
                    brightness = device.brightness,
                    isOnline = true
                )
            }
        }
    }

    // 🌟 Gerät hinzufügen
    fun addDevice(id: String, name: String) {
        viewModelScope.launch {
            val success = lightsRepository.addLight(id, name)
            if (success) {
                val updatedLights = _lights.value.toMutableList()
                updatedLights.add(LightDevice(id, name, false,false, 50))
                _lights.value = updatedLights
            } else {
                println("Fehler: Konnte Gerät nicht zu Firestore hinzufügen!")
            }
        }
    }

    // 🌟 Gerät entfernen
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

    fun updateDevice(id: String, name: String, isOn: Boolean, brightness: Int, isOnline: Boolean ) {
        viewModelScope.launch {
            lightsRepository.updateLight(id, name, isOn, brightness, isOnline)
        }
    }

    fun startDeviceDiscovery() {
        viewModelScope.launch {
            // Clear previous discoveries
            _discoveredDevices.value = emptyList()
            // Subscribe to discovery topic
            mqttRepository.subscribe("lights/discovery") { message ->
                val parts = message.split(":")
                if (parts.size == 2) {
                    val deviceId = parts[0]
                    // Create a discovered device without assuming it's online
                    val device = LightDevice(deviceId, "ESP Device", isOn = false, isOnline = false, brightness = 50)
                    if (_discoveredDevices.value.none { it.id == deviceId }) {
                        _discoveredDevices.value = _discoveredDevices.value + device
                    }
                }
            }
            // Publish discovery request so ESP devices announce themselves
            mqttRepository.publishMessage("lights/discovery/request", "discover")
            // Wait 10 seconds to gather responses
            delay(700)
            mqttRepository.unsubscribe("lights/discovery")
        }
    }

    // 🌟 Gerät umbenennen
    fun renameDevice(id: String, newName: String) {
        viewModelScope.launch {
            val updatedLights = _lights.value.toMutableList()
            val deviceIndex = updatedLights.indexOfFirst { it.id == id }

            if (deviceIndex != -1) {
                updatedLights[deviceIndex] = updatedLights[deviceIndex].copy(name = newName) // ✅ Name direkt ändern
                _lights.value = updatedLights // ✅ StateFlow updaten
                lightsRepository.updateLight(id, name = newName, isOn = null, brightness = null, isOnline = null) // ✅ Firestore speichern
            }
        }
    }


    // 🌟 Geräteliste aktualisieren
    private fun updateDeviceState(deviceId: String, message: String) {
        println("DEBUG: Update for $deviceId with message: $message")
        viewModelScope.launch {
            val updatedLights = _lights.value.toMutableList()
            val existingLight = updatedLights.find { it.id == deviceId }

            // Only update the on/off state from the message. Do not change brightness.
            val isOn = message.contains("\"POWER\":\"ON\"")
            val newLight = LightDevice(
                id = deviceId,
                name = existingLight?.name ?: "Licht $deviceId",
                isOn = isOn,
                isOnline = true,
                brightness = existingLight?.brightness ?: 50 // Keep existing brightness
            )

            if (existingLight != null) {
                updatedLights[updatedLights.indexOf(existingLight)] = newLight
            } else {
                updatedLights.add(newLight)
            }

            _lights.value = updatedLights

            // Update Firestore with the new on/off state but preserve the brightness
            lightsRepository.updateLight(
                id = deviceId,
                isOn = newLight.isOn,
                name = null,
                brightness = newLight.brightness,
                isOnline = true
            )

            // Save the time of the last received message
            onlineStatusTimeout[deviceId] = System.currentTimeMillis()
        }
    }

    // 🌟 Licht togglen
    fun toggleLight(id: String) {
        viewModelScope.launch {
            val light = _lights.value.find { it.id == id } ?: return@launch
            val newState = !light.isOn

            // Update Firestore and local state
            lightsRepository.updateLight(
                id = id,
                isOn = newState,
                name = null,
                brightness = light.brightness, // Preserve current brightness
                isOnline = null
            )
            mqttRepository.publishMessage("cmnd/$id/Power", if (newState) "ON" else "OFF")
            onlineStatusTimeout[id] = System.currentTimeMillis()

            // If turning on, send the brightness command after a brief delay
            if (newState) {
                delay(300) // wait a bit for the light to power on
                mqttRepository.publishMessage("cmnd/$id/Dimmer", light.brightness.toString())
            }
        }
    }

    fun setBrightness(id: String, brightness: Int) {
        viewModelScope.launch {
            // Get the current light state.
            val light = _lights.value.find { it.id == id } ?: return@launch

            // Only send the MQTT command if the light is already on.
            if (light.isOn) {
                mqttRepository.publishMessage("cmnd/$id/Dimmer", brightness.toString())
            }

            // Update the brightness state regardless of the on/off status.
            val updatedLights = _lights.value.toMutableList()
            val lightIndex = updatedLights.indexOfFirst { it.id == id }
            if (lightIndex != -1) {
                updatedLights[lightIndex] = updatedLights[lightIndex].copy(brightness = brightness)
                _lights.value = updatedLights
                onlineStatusTimeout[id] = System.currentTimeMillis()
            }
        }
    }

    fun updateBrightnessLocally(id: String, brightness: Int) {
        viewModelScope.launch {
            val updatedLights = _lights.value.toMutableList()
            val index = updatedLights.indexOfFirst { it.id == id }
            if (index != -1) {
                updatedLights[index] = updatedLights[index].copy(brightness = brightness)
                _lights.value = updatedLights
                onlineStatusTimeout[id] = System.currentTimeMillis()
                // Update brightness in Firestore as needed:
                lightsRepository.updateLight(
                    id = id,
                    isOn = null,
                    name = null,
                    brightness = brightness,
                    isOnline = null
                )
            }
        }
    }

    // Apply the brightness by sending the MQTT command if the light is on.
    fun applyBrightness(id: String) {
        viewModelScope.launch {
            val light = _lights.value.find { it.id == id } ?: return@launch
            if (light.isOn) {
                mqttRepository.publishMessage("cmnd/$id/Dimmer", light.brightness.toString())
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mqttRepository.disconnect()
    }

    /** Extrahiert den Dimmer-Wert aus der MQTT-Nachricht **/
    private fun extractBrightness(message: String): Int? {
        val regex = """"Dimmer":(\d+)""".toRegex()
        val matchResult = regex.find(message)
        return matchResult?.groups?.get(1)?.value?.toIntOrNull()
    }

    private val heartbeatTimeout = 15000L // 35 seconds

    private fun startOfflineWatcher() {
        viewModelScope.launch {
            while (true) {
                delay(10000) // Check periodically
                val currentTime = System.currentTimeMillis()
                val updatedLights = _lights.value.toMutableList()
                var hasChanges = false

                updatedLights.forEachIndexed { index, light ->
                    val lastUpdate = onlineStatusTimeout[light.id] ?: 0
                    // If no heartbeat received in the timeout window, mark device as offline.
                    val isDeviceOffline = (currentTime - lastUpdate) > heartbeatTimeout
                    if (isDeviceOffline && light.isOnline) {
                        println("DEBUG: Device ${light.id} marked as OFFLINE (heartbeat timeout)")
                        updatedLights[index] = light.copy(isOnline = false)
                        hasChanges = true
                        lightsRepository.updateLight(
                            id = light.id,
                            isOn = light.isOn,
                            name = null,
                            brightness = light.brightness,
                            isOnline = false
                        )
                    }
                }
                if (hasChanges) {
                    _lights.value = updatedLights
                }
            }
        }
    }

    private fun updateOnlineStatus(deviceId: String, message: String) {
        viewModelScope.launch {
            // Assume the device sends "Online" in its heartbeat
            val isOnline = message == "Online"
            if (isOnline) {
                onlineStatusTimeout[deviceId] = System.currentTimeMillis()
            }

            // Only update if there is an actual change
            val updatedLights = _lights.value.toMutableList()
            val index = updatedLights.indexOfFirst { it.id == deviceId }
            if (index != -1 && updatedLights[index].isOnline != isOnline) {
                val device = updatedLights[index]
                updatedLights[index] = device.copy(isOnline = isOnline)
                _lights.value = updatedLights
                lightsRepository.updateLight(
                    id = deviceId,
                    isOn = device.isOn,
                    name = null,
                    brightness = device.brightness,
                    isOnline = isOnline
                )
            }
        }
    }

    fun markDeviceOnline(deviceId: String) {
        viewModelScope.launch {
            val updatedLights = _lights.value.toMutableList()
            val index = updatedLights.indexOfFirst { it.id == deviceId }
            if (index != -1) {
                val device = updatedLights[index]
                if (!device.isOnline) {
                    updatedLights[index] = device.copy(isOnline = true)
                    _lights.value = updatedLights
                    lightsRepository.updateLight(
                        id = deviceId,
                        isOn = device.isOn,
                        name = null,
                        brightness = device.brightness,
                        isOnline = true
                    )
                }
            }
            // Refresh the online timeout for the device.
            onlineStatusTimeout[deviceId] = System.currentTimeMillis()
        }
    }

}