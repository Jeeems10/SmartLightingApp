package com.example.smartlightingapp.repository

import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttRepository(
    private val brokerUrl: String = "tcp://192.168.0.53:1883",
    private val clientId: String = "SmartLightingApp",
    private val messageCallback: (String, String) -> Unit
) {
    private var client: MqttClient? = null
    private val mqttUsername = "bjugoy"
    private val mqttPassword = "pass1"

    init {
        connect()
    }

    fun connect() {
        if (client?.isConnected == true) {
            println("✅ MQTT: Already connected to broker.")
            return
        }

        try {
            client = MqttClient(brokerUrl, clientId, MemoryPersistence())
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                isAutomaticReconnect = true
                userName = mqttUsername
                password = mqttPassword.toCharArray()
                connectionTimeout = 10
            }

            client?.connect(options)
            println("✅ MQTT: Connected to broker at $brokerUrl")

            // Subscribe to device discovery
            subscribe("lights/discovery") { message ->
                println("🔥 MQTT Discovery: $message")
                val parts = message.split(":")
                if (parts.size == 2) {
                    val deviceId = parts[0]
                    val ip = parts[1]
                    messageCallback(deviceId, ip) // Send to ViewModel
                }
            }
        } catch (e: MqttException) {
            println("❌ MQTT: Connection failed - ${e.message}")
            e.printStackTrace()
        }
    }

    fun publishMessage(topic: String, payload: String) {
        try {
            if (client == null || !client!!.isConnected) {
                println("⚠️ MQTT: Client not connected, reconnecting...")
                connect()
            }

            val message = MqttMessage(payload.toByteArray()).apply {
                qos = 1
                isRetained = false
            }
            client?.publish(topic, message)
            println("📩 MQTT: Published to $topic - $payload")
        } catch (e: MqttException) {
            println("❌ MQTT: Failed to publish message")
            e.printStackTrace()
        }
    }

    fun subscribe(topic: String, callback: (String) -> Unit) {
        try {
            println("📡 MQTT: Subscribing to $topic")
            client?.subscribe(topic) { _, message ->
                val payload = String(message.payload)
                println("📥 MQTT: Received from $topic - $payload")
                callback(payload)
            }
        } catch (e: MqttException) {
            println("❌ MQTT: Failed to subscribe to $topic")
            e.printStackTrace()
        }
    }

    fun unsubscribe(topic: String) {
        try {
            client?.unsubscribe(topic)
            println("🚫 MQTT: Unsubscribed from $topic")
        } catch (e: MqttException) {
            println("❌ MQTT: Failed to unsubscribe from $topic")
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            client?.disconnect()
            println("❌ MQTT: Disconnected")
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun requestDeviceStatus(deviceId: String) {
        val topic = "cmnd/$deviceId/STATUS"
        println("📡 MQTT: Requesting status for $deviceId")
        publishMessage(topic, "0")
    }
}