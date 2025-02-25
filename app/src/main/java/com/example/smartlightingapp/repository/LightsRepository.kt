package com.example.smartlightingapp.repository

import com.example.smartlightingapp.model.LightDevice
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class LightsRepository {
    private val db = FirebaseFirestore.getInstance()
    val lightsCollection = db.collection("lights")

    // Add a light with the current user's ID
    suspend fun addLight(id: String, name: String): Boolean {
        return try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val userId = currentUser?.uid ?: return false

            val light = hashMapOf(
                "id" to id,
                "name" to name,
                "isOn" to false,
                "brightness" to 50,
                "userId" to userId
            )
            lightsCollection.document(id).set(light).await()
            true
        } catch (e: Exception) {
            println("Fehler beim Hinzufügen des Lichts: ${e.message}")
            false
        }
    }

    // Update light details, and create the document if it doesn't exist
    suspend fun updateLight(id: String, name: String?, isOn: Boolean?, brightness: Int?, isOnline: Boolean?) {
        val docRef = lightsCollection.document(id)
        try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                println("⚠ Firestore: Dokument für $id existiert nicht! Erstelle es neu.")
                addLight(id, name ?: "Neues Licht")
            }

            val updates = mutableMapOf<String, Any>()
            name?.let { updates["name"] = it }
            isOn?.let { updates["isOn"] = it }
            brightness?.let { updates["brightness"] = it }
            isOnline?.let { updates["isOnline"] = it }

            docRef.update(updates).await()
        } catch (e: Exception) {
            println("🔥 Fehler beim Update von $id: ${e.message}")
        }
    }

    // Remove a light document
    suspend fun removeLight(id: String): Boolean {
        return try {
            lightsCollection.document(id).delete().await()
            true
        } catch (e: Exception) {
            println("Fehler beim Entfernen des Lichts: ${e.message}")
            false
        }
    }

    // Retrieve all lights for the current user
    suspend fun getAllLights(): List<LightDevice> {

        return try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val userId = currentUser?.uid ?: return emptyList()
            println("USER ID: $userId")
            // Query only the lights that match the current user's ID
            val snapshot = lightsCollection.whereEqualTo("userId", userId).get().await()
            if (snapshot.isEmpty) {
                println("Firestore: Keine Lichter gefunden.")
                return emptyList()
            }

            snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: return@mapNotNull null
                val name = doc.getString("name") ?: "Unbekannt"
                val isOn = doc.getBoolean("isOn") ?: false
                val isOnline = doc.getBoolean("isOnline") ?: false
                val brightness = doc.getLong("brightness")?.toInt() ?: 50
                // Retrieve the userId if needed (should match current user's id)
                val lightUserId = doc.getString("userId") ?: ""
                println("Firestore: Gerät gefunden - $name ($id)")
                LightDevice(id, name, isOn, isOnline, brightness, lightUserId)
            }
        } catch (e: Exception) {
            println("Fehler beim Abrufen der Geräte: ${e.message}")
            emptyList()
        }
    }
}