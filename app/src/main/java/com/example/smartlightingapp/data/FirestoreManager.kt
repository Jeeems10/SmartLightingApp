package com.example.smartlightingapp.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreManager {
    private val db = FirebaseFirestore.getInstance()
    private val lightsCollection = db.collection("lights")

    suspend fun addLight(id: String, name: String) {
        val light = hashMapOf("id" to id, "name" to name, "isOn" to false, "brightness" to 50)
        lightsCollection.document(id).set(light).await()
    }

    suspend fun updateLight(id: String, name: String?, isOn: Boolean?, brightness: Int?) {
        val updates = mutableMapOf<String, Any>()
        name?.let { updates["name"] = it }
        isOn?.let { updates["isOn"] = it }
        brightness?.let { updates["brightness"] = it }

        lightsCollection.document(id).update(updates).await()
    }

    suspend fun removeLight(id: String) {
        lightsCollection.document(id).delete().await()
    }
}
