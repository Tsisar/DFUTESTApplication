package com.example.dfutestapplication

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dfutestapplication.ui.theme.DFUTESTApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DFUTESTApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("DFU test")
                    ButtonSection()
                }
            }
        }
    }
}

@Composable
fun Greeting(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DFUTESTApplicationTheme {
        Greeting("Android")
    }
}

@Composable
fun ButtonSection() {
    Column(
        modifier = Modifier
            .padding(32.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ControlButton()
        ScannerButton()
    }
}

@Composable
fun ControlButton() {
    val context = LocalContext.current
    val isServiceRunning = remember { mutableStateOf(false) }

    Button(
        onClick = {
            val serviceIntent = Intent(context, MyService::class.java)
            if (isServiceRunning.value) {
                context.stopService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            isServiceRunning.value = !isServiceRunning.value
        },
        modifier = Modifier.padding(8.dp)
    ) {
        Text(if (isServiceRunning.value) "STOP DFU" else "START DFU")
    }
}

@Composable
fun ScannerButton() {
    val context = LocalContext.current
    val isServiceRunning = remember { mutableStateOf(false) }

    Button(
        onClick = {
            val serviceIntent = Intent(context, ScanService::class.java)
            if (isServiceRunning.value) {
                context.stopService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            isServiceRunning.value = !isServiceRunning.value
        },
        modifier = Modifier.padding(8.dp)
    ) {
        Text(if (isServiceRunning.value) "STOP SCANNER" else "START SCANNER")
    }
}
