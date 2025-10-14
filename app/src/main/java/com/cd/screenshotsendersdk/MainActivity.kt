package com.cd.screenshotsendersdk

import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cd.screenshotsender.presentation.ScreenShotSenderSDK
import com.cd.screenshotsendersdk.ui.theme.ScreenShotSenderSDKTheme

class MainActivity : ComponentActivity() {


    private lateinit var projectionManager: MediaProjectionManager

    private val mediaProjectionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                ScreenShotSenderSDK.startSDK(
                    this,
                    result.resultCode,
                    result.data!!,
                    "deliveryapp.countrydelight.in.deliveryapp"
                )
            } else {
                Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        enableEdgeToEdge()
        requestMediaProjectionPermission()
        setContent {
            ScreenShotSenderSDKTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun requestMediaProjectionPermission() {
        val intent = projectionManager.createScreenCaptureIntent()
        mediaProjectionLauncher.launch(intent)
    }

    override fun onPause() {
        super.onPause()
        ScreenShotSenderSDK.stopSDK(this)
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    var value by remember {
        mutableStateOf("")
    }
    OutlinedTextField(value, onValueChange = {
        value = it
    }, modifier = Modifier.padding(32.dp))
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ScreenShotSenderSDKTheme {
        Greeting("Android")
    }
}