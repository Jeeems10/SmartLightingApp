package com.example.smartlightingapp.repository

import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttRepository(
        private val brokerUrl: String,
        private val clientId: String,
        private val messageCallback: (String, String) -> Unit // Callback für MQTT-Nachrichten
        ){
    private var client: MqttClient? = null

    // Definiere Topics für Lichtsteuerung & Helligkeit
    private val stateTopic = "stat/D1Mini_1/Result"

    init{
        connect()
    }

     fun connect(){
         if (client?.isConnected == true) {
             println("MQTT: Bereits verbunden.")
             return
         }

         try {
             client = MqttClient(brokerUrl, clientId, MemoryPersistence())
             val options = MqttConnectOptions().apply {
                 isCleanSession = true  // Verbindung behalten
                 isAutomaticReconnect = true  // Automatisch neu verbinden
                 userName = "jamesponce"  // Falls nötig
                 password = "jamesponce".toCharArray()  // Falls nötig
                 connectionTimeout = 10 // Erhöhe Timeout
             }

             client?.connect(options)
             println(" MQTT: Verbindung hergestellt mit $brokerUrl")

             // Manuelles Abonnieren nach Verbindung
             listOf("D1Mini_1", "D1Mini_2").forEach { deviceId ->
                 val topic = "stat/$deviceId/RESULT"
                 println("DEBUG: Manuelles Abonnieren von $topic")

                 subscribe(topic, 1) { message ->
                     println("DEBUG: Nachricht empfangen für $deviceId -> $message")
                     messageCallback(deviceId, message) // Nachricht an MqttViewModel weitergeben
                 }
             }

         } catch (e: MqttException) {
             println(" MQTT: Verbindung fehlgeschlagen - ${e.message}")
             e.printStackTrace()
         }
    }

    fun publishMessage(topic: String, payload: String) {
        try {
            if (client == null || !client!!.isConnected) {  //  Sicherstellen, dass MQTT verbunden ist
                println("⚠ MQTT: Client ist nicht verbunden, versuche zu verbinden...")
                connect()
            }

            val message = MqttMessage(payload.toByteArray()).apply {
                qos = 1
                isRetained = false
            }
            client?.publish(topic, message)
            println(" MQTT: Nachricht gesendet - $payload")
        } catch (e: MqttException) {
            println(" MQTT: Nachricht konnte nicht gesendet werden")
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


    fun disconnect() {
        try {
            client?.disconnect()
            println("MQTT: Verbindung getrennt")
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun requestDeviceStatus(deviceId: String) {
        val topic = "cmnd/$deviceId/STATUS"
        println("DEBUG: Sende Statusabfrage an $topic")
        publishMessage(topic, "0") // Statusabfrage an Tasmota
    }

}