package com.example.smartlightingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartlightingapp.ui.theme.SmartLightingAppTheme
import com.example.smartlightingapp.viewModel.MqttViewModel



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartLightingAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen()
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    mqttViewModel: MqttViewModel = viewModel() // ViewModel für MQTT
) {
    //State für Button und Slider
    val isLightOn by mqttViewModel.lightState.collectAsState()
    val brightness by mqttViewModel.brightness.collectAsState()

    // Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center

    ) {
        Text(text = "Smart Lighting")
        
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Light is: ${if (isLightOn) "On" else "Off"}")
            Spacer(modifier = Modifier.width(8.dp))
            Switch(checked = isLightOn,
                onCheckedChange = {mqttViewModel.toggleLight()}
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(text = "Brightsness:$brightness")
        
        Slider(value = brightness.toFloat(),
            onValueChange = {
                mqttViewModel.setBrightness(it.toInt()) // Helligkeit über MQTT senden
            },
            valueRange = 0f..100f
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
}