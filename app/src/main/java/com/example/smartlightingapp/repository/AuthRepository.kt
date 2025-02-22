package com.example.smartlightingapp.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    suspend fun registerUser(email: String, password: String): Boolean {
        return try {
            auth.createUserWithEmailAndPassword(email, password).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun loginUser(email: String, password: String): Boolean {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            println("Login erfolgreich für $email")
            true
        } catch (e: Exception) {
            println("Fehler bei der Anmeldung: ${e.message}")
            false
        }
    }

    fun getCurrentUser() = auth.currentUser
}
