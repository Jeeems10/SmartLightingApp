package com.example.smartlightingapp.repository

import com.example.smartlightingapp.model.LightDevice
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class LightsRepository {
    private val db = FirebaseFirestore.getInstance()
    val lightsCollection = db.collection("lights")

    suspend fun addLight(id: String, name: String): Boolean {
        return try {
            val light = hashMapOf("id" to id, "name" to name, "isOn" to false, "brightness" to 50)
            lightsCollection.document(id).set(light).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateLight(id: String, name: String?, isOn: Boolean?, brightness: Int?, isOnline: Boolean?) {
        val docRef = lightsCollection.document(id)

        try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                println("⚠ Firestore: Dokument für $id existiert nicht! Erstelle es neu.")
                addLight(id, name ?: "Neues Licht")  // Falls nicht vorhanden, erstelle das Dokument
            }

            val updates = mutableMapOf<String, Any>()
            name?.let { updates["name"] = it }
            isOn?.let { updates["isOn"] = it }
            brightness?.let { updates["brightness"] = it }
            isOnline?.let { updates["isOnline"] = it }  // 🔥 Speichere Online-Status

            docRef.update(updates).await()
        } catch (e: Exception) {
            println("🔥 Fehler beim Update von $id: ${e.message}")
        }
    }



    suspend fun removeLight(id: String): Boolean {
        return try {
            lightsCollection.document(id).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAllLights(): List<LightDevice> {
        return try {
            val snapshot = lightsCollection.get().await()
            if (snapshot.isEmpty) {
                println("Firestore: Keine Lichter gefunden.")
                return emptyList()
            }

            snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: return@mapNotNull null
                val name = doc.getString("name") ?: "Unbekannt"
                val isOn = doc.getBoolean("isOn") ?: false
                val isOnline = doc.getBoolean("isOnline")?:false
                val brightness = doc.getLong("brightness")?.toInt() ?: 50


                println("Firestore: Gerät gefunden - $name ($id)")
                LightDevice(id, name, isOn, isOnline, brightness)
            }
        } catch (e: Exception) {
            println("Fehler beim Abrufen der Geräte: ${e.message}")
            emptyList()
        }
    }

}
