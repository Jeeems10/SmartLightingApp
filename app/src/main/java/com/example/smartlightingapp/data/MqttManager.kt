package com.example.smartlightingapp.data

import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttManager(
        private val brokerUrl: String,
        private val clientId: String,
        private val topic: String
        ){
    private var client: MqttClient? = null

    init{

    }

    private fun connect(){
        try {
            client = MqttClient(brokerUrl, clientId, MemoryPersistence())
            val options = MqttConnectOptions()
            options.isCleanSession = true
            client?.connect(options)
            println("MQTT: Verbunden mit $brokerUrl")
        } catch (e: MqttException){
            e.printStackTrace()
        }
    }

    fun publishMessage(payload: String) {
        try {
            val message = MqttMessage(payload.toByteArray())
            client?.publish(topic, message)
            println("MQTT: Nachricht gesendet - $payload")
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun subscribe(callback: (String) -> Unit) {
        try {
            client?.subscribe(topic) { _, message ->
                val payload = String(message.payload)
                println("MQTT: Nachricht empfangen - $payload")
                callback(payload)
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            client?.disconnect()
            println("MQTT: Verbindung getrennt")
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }
}