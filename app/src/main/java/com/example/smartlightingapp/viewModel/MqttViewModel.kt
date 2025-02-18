package com.example.smartlightingapp.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartlightingapp.data.MqttManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class MqttViewModel: ViewModel() {
    private val mqttManager = MqttManager("tcp://192.168.0.67:1883", "SmartLightingApp")

    private val _lightState = MutableStateFlow(false)  // True = Licht an, False = Licht aus
    val lightState = _lightState.asStateFlow()

    private val _brightness = MutableStateFlow(50)
    val brightness = _brightness.asStateFlow()

    init {
        // MQTT Nachrichten empfangen
        mqttManager.subscribe { message ->
            println(" MQTT: Nachricht empfangen - $message") // Debugging
            if (message.contains("POWER\":\"ON")) {
                _lightState.value = true
            } else if (message.contains("POWER\":\"OFF")) {
                _lightState.value = false
            } else if (message.contains("Dimmer")) {
                val brightnessValue = extractBrightness(message)
                if (brightnessValue != null) _brightness.value = brightnessValue
            }
        }
    }

    fun toggleLight() {
        viewModelScope.launch {
            println("📡 MQTT: Versuche Licht zu toggeln...")
            mqttManager.connect() //  Verbindung sicherstellen
            val newState = !_lightState.value
            mqttManager.publishMessage("cmnd/D1Mini_1/Power", if (newState) "ON" else "OFF")
            _lightState.value = newState
        }
    }

    fun setBrightness(value: Int) {
        viewModelScope.launch {
            println("📡 MQTT: Versuche Helligkeit zu setzen...")
            mqttManager.connect() //  Verbindung sicherstellen
            mqttManager.publishMessage("D1Mini_1/cmnd/Dimmer", value.toString())
            _brightness.value = value
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