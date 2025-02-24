package com.example.smartlightingapp.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ConfigScreen() {
    var isDarkMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Einstellungen", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Dark Mode")
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = isDarkMode,
                onCheckedChange = { isDarkMode = it }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { /* Hier könnte man weitere Settings speichern */ }) {
            Text("Speichern")
        }
    }
}
