package com.example.smartlightingapp.repository

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttRepository(
    private val brokerUrl: String,
    private val clientId: String,
    private val messageCallback: (String, String) -> Unit,
    private val connectionLostCallback: () -> Unit,
    private val connectionEstablishedCallback: () -> Unit
) {
    private var client: MqttClient? = null
    var isConnected = false

    init {
        connect()
    }

    fun connect() {
        // If we already have a client and it's connected, do nothing
        if (client?.isConnected == true) {
            println("MQTT: Bereits verbunden.")
            return
        }

        try {
            client = MqttClient(brokerUrl, clientId, MemoryPersistence())
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                isAutomaticReconnect = true
                userName = "bjugoy"
                password = "pass1".toCharArray()
                connectionTimeout = 10
                keepAliveInterval = 60 // Adjusted keep-alive
            }

            client?.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    println("MQTT: connectComplete - reconnect: $reconnect, serverURI: $serverURI")
                    isConnected = true
                    connectionEstablishedCallback()
                }

                override fun connectionLost(cause: Throwable?) {
                    println("⚠ MQTT: Verbindung verloren! ${cause?.message}")
                    isConnected = false
                    connectionLostCallback()
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    val payload = message?.toString() ?: return
                    topic?.let { messageCallback(it, payload) }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            client?.connect(options)
            println("MQTT: Verbindung hergestellt.")

        } catch (e: MqttException) {
            println("MQTT: Verbindung fehlgeschlagen - ${e.message}")
            e.printStackTrace()
        }
    }

    fun publishMessage(topic: String, payload: String) {
        try {
            if (client == null || !client!!.isConnected) {
                println("⚠ MQTT: Client ist nicht verbunden, versuche zu verbinden...")
                connect()
            }
            val message = MqttMessage(payload.toByteArray()).apply {
                qos = 1
                isRetained = false
            }
            client?.publish(topic, message)
            println("MQTT: Nachricht gesendet - $payload")
        } catch (e: MqttException) {
            println("MQTT: Nachricht konnte nicht gesendet werden")
            e.printStackTrace()
        }
    }

    fun subscribe(topic: String, qos: Int = 1, callback: (String) -> Unit) {
        try {
            println("DEBUG: Abonniere Topic $topic")
            client?.subscribe(topic, qos) { _, message ->
                val payload = String(message.payload)
                println("MQTT: Nachricht empfangen von $topic - $payload")
                callback(payload)
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun unsubscribe(topic: String) {
        try {
            client?.unsubscribe(topic)
            println("MQTT: Unsubscribed from $topic")
        } catch (e: MqttException) {
            println("MQTT: Failed to unsubscribe from $topic")
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            client?.disconnect()
            println("MQTT: Verbindung getrennt")
        } catch (e: MqttException) {
            e.printStackTrace()
        } finally {
            client = null
        }
    }

    fun requestDeviceStatus(deviceId: String) {
        val topic = "cmnd/$deviceId/STATUS"
        println("DEBUG: Sende Statusabfrage an $topic")
        publishMessage(topic, "0")
    }
}