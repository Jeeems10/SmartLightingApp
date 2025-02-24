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
        brokerUrl = "tcp://192.168.0.53:1883",
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
                mqttRepository.subscribe("tele/${device.id}/LWT") { message ->
                    updateOnlineStatus(device.id, message)
                }
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
                    val ipAddress = parts[1]
                    // Create a discovered device (default name "ESP Device")
                    val device = LightDevice(deviceId, "ESP Device",
                        isOn = false,
                        isOnline = false,
                        brightness = 50
                    )
                    // Add device if not already in the list
                    if (_discoveredDevices.value.none { it.id == deviceId }) {
                        _discoveredDevices.value = _discoveredDevices.value + device
                    }
                }
            }
            // Publish discovery request so ESP devices announce themselves
            mqttRepository.publishMessage("lights/discovery/request", "discover")
            // Wait 10 seconds to gather responses
            delay(1000)
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
        println("DEBUG: Update für $deviceId mit Nachricht: $message")

        viewModelScope.launch {
            val updatedLights = _lights.value.toMutableList()
            val existingLight = updatedLights.find { it.id == deviceId }

            val newLight = LightDevice(
                id = deviceId,
                name = existingLight?.name ?: "Licht $deviceId",
                isOn = message.contains("\"POWER\":\"ON\""),
                isOnline = existingLight?.isOnline ?: false,
                brightness = extractBrightness(message) ?: existingLight?.brightness ?: 50
            )

            if (existingLight != null) {
                updatedLights[updatedLights.indexOf(existingLight)] = newLight
            } else {
                updatedLights.add(newLight)
            }

            _lights.value = updatedLights

            // Update Firestore
            lightsRepository.updateLight(
                id = deviceId,
                isOn = newLight.isOn,
                name = null,
                brightness = newLight.brightness,
                isOnline = null
            )

            // 🔥 Speichere den letzten Zeitpunkt, an dem das Gerät eine Nachricht gesendet hat
            onlineStatusTimeout[deviceId] = System.currentTimeMillis()
        }
    }


    // 🌟 Licht togglen
    fun toggleLight(id: String) {
        viewModelScope.launch {
            val light = _lights.value.find { it.id == id } ?: return@launch
            val newState = !light.isOn


            lightsRepository.updateLight(id, isOn = newState, name = null, brightness = null, isOnline = null) // Firestore updaten
            mqttRepository.publishMessage("cmnd/$id/Power", if (newState) "ON" else "OFF") // MQTT senden

            onlineStatusTimeout[id] = System.currentTimeMillis()
        }
    }

    fun setBrightness(id: String, brightness: Int) {
        viewModelScope.launch {
            mqttRepository.publishMessage("cmnd/$id/Dimmer", brightness.toString())

            val updatedLights = _lights.value.toMutableList()
            val lightIndex = updatedLights.indexOfFirst { it.id == id }
            if (lightIndex != -1) {
                updatedLights[lightIndex] = updatedLights[lightIndex].copy(brightness = brightness)
                _lights.value = updatedLights
                onlineStatusTimeout[id] = System.currentTimeMillis()
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

    private fun startOfflineWatcher() {
        viewModelScope.launch {
            while (true) {
                delay(10_000) // 🔥 Alle 10 Sekunden prüfen

                val currentTime = System.currentTimeMillis()
                val updatedLights = _lights.value.toMutableList()
                var hasChanges = false

                updatedLights.forEachIndexed { index, light ->
                    val lastUpdate = onlineStatusTimeout[light.id] ?: 0
                    val isDeviceOffline = (currentTime - lastUpdate) > 10_000 // 🔥 Timeout nach 10 Sekunden

                    if (isDeviceOffline && light.isOnline) {
                        println("DEBUG: Gerät ${light.id} wurde als OFFLINE erkannt!")

                        updatedLights[index] = light.copy(isOnline = false)
                        hasChanges = true

                        // ✅ Firestore aktualisieren
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
            val updatedLights = _lights.value.toMutableList()
            val existingLight = updatedLights.find { it.id == deviceId }

            val isOnline = message == "Online"

            println("DEBUG: Online-Status von $deviceId wurde aktualisiert: $isOnline (Nachricht: $message)")

            if (existingLight != null && existingLight.isOnline != isOnline) {
                updatedLights[updatedLights.indexOf(existingLight)] = existingLight.copy(isOnline = isOnline)
                _lights.value = updatedLights

                lightsRepository.updateLight(
                    id = deviceId,
                    isOn = existingLight.isOn,
                    name = null,
                    brightness = existingLight.brightness,
                    isOnline = isOnline
                )
            }

            if (isOnline) {
                onlineStatusTimeout[deviceId] = System.currentTimeMillis()
            }
        }
    }

}