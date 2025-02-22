package com.example.smartlightingapp.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartlightingapp.data.FirestoreManager
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

    private val firestoreManager = FirestoreManager()

    // 🌟 Liste aller Lichter speichern
    private val _lights = MutableStateFlow<List<LightDevice>>(emptyList())
    val lights = _lights.asStateFlow()


    // 🌟 MQTT-Subscription für mehrere Geräte
    init {
        viewModelScope.launch {
            val savedLights = firestoreManager.getAllLights()
            _lights.value = savedLights  // Firestore-Lichter laden

            // Nach dem Laden der Geräte -> MQTT abonnieren
            savedLights.forEach { device ->
                mqttManager.subscribe("stat/${device.id}/RESULT") { message ->
                    updateDeviceState(device.id, message)
                }
                mqttManager.requestDeviceStatus(device.id)
            }
        }
        // 🔥 Echtzeit-Listener für Firestore (damit UI sofort aktualisiert wird)
        firestoreManager.lightsCollection.addSnapshotListener { snapshot, e ->
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

    // 🌟 Gerät hinzufügen
    fun addDevice(id: String, name: String) {
        viewModelScope.launch {
            val success = firestoreManager.addLight(id, name)
            if (success) {
                val updatedLights = _lights.value.toMutableList()
                updatedLights.add(LightDevice(id, name, false, 50))
                _lights.value = updatedLights
            } else {
                println("Fehler: Konnte Gerät nicht zu Firestore hinzufügen!")
            }
        }
    }

    // 🌟 Gerät entfernen
    fun removeDevice(id: String) {
        viewModelScope.launch {
            val success = firestoreManager.removeLight(id)
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
            firestoreManager.updateLight(id, name, isOn, brightness)
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
                firestoreManager.updateLight(id, name = newName, isOn = null, brightness = null) // ✅ Firestore speichern
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
                brightness = extractBrightness(message) ?: existingLight?.brightness ?: 50
            )

            if (existingLight != null) {
                updatedLights[updatedLights.indexOf(existingLight)] = newLight
            } else {
                updatedLights.add(newLight)
            }

            _lights.value = updatedLights

            // Update Firestore
            firestoreManager.updateLight(
                id = deviceId,
                isOn = newLight.isOn,
                name = null,
                brightness = newLight.brightness
            )
        }
    }


    // 🌟 Licht togglen
    fun toggleLight(id: String) {
        viewModelScope.launch {
            val light = _lights.value.find { it.id == id } ?: return@launch
            val newState = !light.isOn


            firestoreManager.updateLight(id, isOn = newState, name = null, brightness = null) // Firestore updaten
            mqttManager.publishMessage("cmnd/$id/Power", if (newState) "ON" else "OFF") // MQTT senden
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