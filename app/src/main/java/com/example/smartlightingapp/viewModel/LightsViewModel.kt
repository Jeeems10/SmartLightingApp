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


class LightsViewModel: ViewModel() {
    private val mqttRepository = MqttRepository("tcp://192.168.0.67:1883", "SmartLightingApp"){
            deviceId, message ->
        updateDeviceState(deviceId, message) // Hier wird updateDeviceState() aufgerufen
    }

    private val lightsRepository = LightsRepository()

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
                mqttRepository.subscribe("stat/${device.id}/RESULT") { message ->
                    updateDeviceState(device.id, message)
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

    // 🌟 Gerät umbenennen
    fun renameDevice(id: String, newName: String) {
        viewModelScope.launch {
            val updatedLights = _lights.value.toMutableList()
            val deviceIndex = updatedLights.indexOfFirst { it.id == id }

            if (deviceIndex != -1) {
                updatedLights[deviceIndex] = updatedLights[deviceIndex].copy(name = newName) // ✅ Name direkt ändern
                _lights.value = updatedLights // ✅ StateFlow updaten
                lightsRepository.updateLight(id, name = newName, isOn = null, brightness = null, isOnline = false) // ✅ Firestore speichern
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
                isOnline = true,
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
                isOnline = true
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
}