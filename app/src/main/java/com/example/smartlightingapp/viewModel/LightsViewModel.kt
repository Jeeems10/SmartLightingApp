package com.example.smartlightingapp.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartlightingapp.model.LightDevice
import com.example.smartlightingapp.repository.LightsRepository
import com.example.smartlightingapp.repository.MqttRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
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
            viewModelScope.launch { onMqttDisconnected() }
        },
        connectionEstablishedCallback = {
            viewModelScope.launch { _mqttConnected.value = true }
        }
    )

    private val lightsRepository = LightsRepository()

    private val _discoveredDevices = MutableStateFlow<List<LightDevice>>(emptyList())
    val discoveredDevices = _discoveredDevices.asStateFlow()

    // 🌟 List of all lights
    private val _lights = MutableStateFlow<List<LightDevice>>(emptyList())
    val lights = _lights.asStateFlow()

    private val onlineStatusTimeout = mutableMapOf<String, Long>()

    //test
    // Save the snapshot listener registration so we can remove it later.
    private var lightsListener: ListenerRegistration? = null

    init {
        viewModelScope.launch {
            // Initial load of Firestore lights (filtered by current user)
            val savedLights = lightsRepository.getAllLights()
            _lights.value = savedLights

            // Subscribe to MQTT topics for each device
            savedLights.forEach { device ->
                mqttRepository.subscribe("tele/${device.id}/LWT") { message ->
                    updateOnlineStatus(device.id, message)
                }
                mqttRepository.subscribe("tele/${device.id}/HEARTBEAT") { message ->
                    updateOnlineStatusWithHeartbeat(device.id, message)
                }
                mqttRepository.requestDeviceStatus(device.id)
            }
        }

        attachFirestoreListener()
        startOfflineWatcher()
    }

    private fun attachFirestoreListener() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        currentUserId?.let { uid ->
            lightsListener = lightsRepository.lightsCollection
                .whereEqualTo("userId", uid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        println("Firestore Fehler: ${e.message}")
                        return@addSnapshotListener
                    }
                    snapshot?.documents?.mapNotNull { doc ->
                        val id = doc.getString("id") ?: return@mapNotNull null
                        val name = doc.getString("name") ?: "Unbekannt"
                        val isOn = doc.getBoolean("isOn") ?: false
                        val isOnline = doc.getBoolean("isOnline") ?: false
                        val brightness = doc.getLong("brightness")?.toInt() ?: 50
                        val userId = doc.getString("userId") ?: ""
                        LightDevice(id, name, isOn, isOnline, brightness, userId)
                    }?.let { newLights ->
                        println("🔥 Firestore Update: $newLights")
                        _lights.value = newLights
                    }
                }
        }
    }

    private fun onMqttDisconnected() {
        _mqttConnected.value = false
    }

    private fun updateOnlineStatusWithHeartbeat(deviceId: String, message: String) {
        viewModelScope.launch {
            onlineStatusTimeout[deviceId] = System.currentTimeMillis()
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

    // 🌟 Add a device; the repository will attach the current user's ID
    fun addDevice(id: String, name: String) {
        viewModelScope.launch {
            val success = lightsRepository.addLight(id, name)
            if (success) {
                // Optionally, fetch the newly added device to get its userId
                val currentUserId = lightsRepository.getAllLights().firstOrNull { it.id == id }?.userId ?: ""
                val updatedLights = _lights.value.toMutableList()
                updatedLights.add(LightDevice(id, name, false, false, 50, currentUserId))
                _lights.value = updatedLights
            } else {
                println("Fehler: Konnte Gerät nicht zu Firestore hinzufügen!")
            }
        }
    }

    fun clearConnections() {
        lightsListener?.remove() // Remove Firestore listener
        mqttRepository.disconnect() // Disconnect MQTT
        println("DEBUG: Cleared connections in LightsViewModel")
    }

    // 🌟 Remove a device
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

    fun updateDevice(id: String, name: String, isOn: Boolean, brightness: Int, isOnline: Boolean) {
        viewModelScope.launch {
            lightsRepository.updateLight(id, name, isOn, brightness, isOnline)
        }
    }

    fun startDeviceDiscovery() {
        viewModelScope.launch {
            _discoveredDevices.value = emptyList()
            mqttRepository.subscribe("lights/discovery") { message ->
                val parts = message.split(":")
                if (parts.size == 2) {
                    val deviceId = parts[0]
                    // Create a discovered device without assuming it's online. User id is unknown until added.
                    val device = LightDevice(deviceId, "ESP Device", isOn = false, isOnline = false, brightness = 50, userId = "")
                    if (_discoveredDevices.value.none { it.id == deviceId }) {
                        _discoveredDevices.value = _discoveredDevices.value + device
                    }
                }
            }
            mqttRepository.publishMessage("lights/discovery/request", "discover")
            delay(700)
            mqttRepository.unsubscribe("lights/discovery")
        }
    }

    // 🌟 Rename a device
    fun renameDevice(id: String, newName: String) {
        viewModelScope.launch {
            val updatedLights = _lights.value.toMutableList()
            val deviceIndex = updatedLights.indexOfFirst { it.id == id }
            if (deviceIndex != -1) {
                updatedLights[deviceIndex] = updatedLights[deviceIndex].copy(name = newName)
                _lights.value = updatedLights
                lightsRepository.updateLight(id, name = newName, isOn = null, brightness = null, isOnline = null)
            }
        }
    }

    // 🌟 Update the device state from MQTT messages
    private fun updateDeviceState(deviceId: String, message: String) {
        println("DEBUG: Update for $deviceId with message: $message")
        viewModelScope.launch {
            val updatedLights = _lights.value.toMutableList()
            val existingLight = updatedLights.find { it.id == deviceId }
            val isOn = message.contains("\"POWER\":\"ON\"")
            val newLight = LightDevice(
                id = deviceId,
                name = existingLight?.name ?: "Licht $deviceId",
                isOn = isOn,
                isOnline = true,
                brightness = existingLight?.brightness ?: 50,
                userId = existingLight?.userId ?: ""
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
                brightness = newLight.brightness,
                isOnline = true
            )
            onlineStatusTimeout[deviceId] = System.currentTimeMillis()
        }
    }

    // 🌟 Toggle light state
    fun toggleLight(id: String) {
        viewModelScope.launch {
            val light = _lights.value.find { it.id == id } ?: return@launch
            val newState = !light.isOn
            lightsRepository.updateLight(
                id = id,
                isOn = newState,
                name = null,
                brightness = light.brightness,
                isOnline = null
            )
            mqttRepository.publishMessage("cmnd/$id/Power", if (newState) "ON" else "OFF")
            onlineStatusTimeout[id] = System.currentTimeMillis()
            if (newState) {
                delay(300)
                mqttRepository.publishMessage("cmnd/$id/Dimmer", light.brightness.toString())
            }
        }
    }

    fun setBrightness(id: String, brightness: Int) {
        viewModelScope.launch {
            val light = _lights.value.find { it.id == id } ?: return@launch
            if (light.isOn) {
                mqttRepository.publishMessage("cmnd/$id/Dimmer", brightness.toString())
            }
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

    // Apply brightness via MQTT if the light is on.
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
        // Remove the Firestore snapshot listener to avoid stale data
        lightsListener?.remove()
        mqttRepository.disconnect()
    }

    private fun extractBrightness(message: String): Int? {
        val regex = """"Dimmer":(\d+)""".toRegex()
        val matchResult = regex.find(message)
        return matchResult?.groups?.get(1)?.value?.toIntOrNull()
    }

    private val heartbeatTimeout = 15000L

    private fun startOfflineWatcher() {
        viewModelScope.launch {
            while (true) {
                delay(10000)
                val currentTime = System.currentTimeMillis()
                val updatedLights = _lights.value.toMutableList()
                var hasChanges = false
                updatedLights.forEachIndexed { index, light ->
                    val lastUpdate = onlineStatusTimeout[light.id] ?: 0
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
            val isOnline = message == "Online"
            if (isOnline) {
                onlineStatusTimeout[deviceId] = System.currentTimeMillis()
            }
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
            onlineStatusTimeout[deviceId] = System.currentTimeMillis()
        }
    }
}