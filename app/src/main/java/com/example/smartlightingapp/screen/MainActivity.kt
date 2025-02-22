package com.example.smartlightingapp.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.smartlightingapp.data.LightDevice
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
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "device_list") {
        composable("device_list") { DeviceListScreen(navController) }
        composable("device_detail/{deviceId}") { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId")
            if (deviceId != null) {
                DeviceDetailScreen(navController, deviceId)
            } else {
                Text("Fehler: Kein Gerät gefunden!") // Falls `deviceId` fehlt
            }
        }
    }
}

@Composable
fun DeviceListScreen(navController: NavController, mqttViewModel: MqttViewModel = viewModel()) {
    val lights by mqttViewModel.lights.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Smart Lighting")

        LazyColumn {
            items(lights) { light ->
                LightItem(light, mqttViewModel = mqttViewModel ,onClick = { navController.navigate("device_detail/${light.id}") })
            }
        }
    }
}

@Composable
fun LightItem(light: LightDevice, mqttViewModel: MqttViewModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = light.name, modifier = Modifier.weight(1f))

        Switch(
            checked = light.isOn,
            onCheckedChange = {
                println("DEBUG: Toggle Licht ${light.id} auf ${!light.isOn}")
                mqttViewModel.toggleLight(light.id) // 🚀 Jetzt wird MQTT & Firestore gesteuert
            }
        )
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
}