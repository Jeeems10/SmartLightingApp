package com.example.smartlightingapp.data

import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttManager(
        private val brokerUrl: String,
        private val clientId: String
        ){
    private var client: MqttClient? = null

    // Definiere Topics für Lichtsteuerung & Helligkeit
    private val powerTopic = "D1Mini_1/cmnd/POWER"
    private val dimmerTopic = "D1Mini_1/cmnd/Dimmer"
    private val stateTopic = "D1Mini_1/stat/RESULT" // Hier kommen Rückmeldungen an

    init{
        connect()
    }

     fun connect(){
         try {
             if (client?.isConnected == true) {
                 println("MQTT: Bereits verbunden.")
                 return
             }

             client = MqttClient(brokerUrl, clientId, MemoryPersistence())
             val options = MqttConnectOptions().apply {
                 isCleanSession = false  // Verbindung behalten
                 isAutomaticReconnect = true  // Automatisch neu verbinden
                 userName = "jamesponce"  // Falls nötig
                 password = "jamesponce".toCharArray()  // Falls nötig
             }

             client?.connect(options)
             println(" MQTT: Verbindung hergestellt mit $brokerUrl")
             subscribe { stateTopic }

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

    fun subscribe(callback: (String) -> Unit) {
        try {
            client?.subscribe(stateTopic) { _, message ->
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