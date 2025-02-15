package com.example.smartlightingapp.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartlightingapp.data.MqttManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class MqttViewModel: ViewModel() {
    private val mqttManager = MqttManager("tcp://192.168.0.67:1883", "SmartLightingApp", "smartlight/power")

    private val _lightState = MutableStateFlow(false)  // True = Licht an, False = Licht aus
    val lightState = _lightState.asStateFlow()

    private val _brightness = MutableStateFlow(50)
    val brightness = _brightness.asStateFlow()

    init {
        // MQTT Nachrichten empfangen
        mqttManager.subscribe { message ->
            when (message) {
                "ON" -> _lightState.value = true
                "OFF" -> _lightState.value = false
                else -> {
                    val value = message.toIntOrNull()
                    if (value != null) _brightness.value = value
                }
            }
        }
    }

    fun toggleLight() {
        viewModelScope.launch {
            val newState = !_lightState.value
            mqttManager.publishMessage(if (newState) "ON" else "OFF")
            _lightState.value = newState
        }
    }

    fun setBrightness(value: Int) {
        viewModelScope.launch {
            mqttManager.publishMessage(value.toString())
            _brightness.value = value
        }
    }

    override fun onCleared() {
        super.onCleared()
        mqttManager.disconnect()
    }
}