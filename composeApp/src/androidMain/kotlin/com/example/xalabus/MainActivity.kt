package com.example.xalabus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.xalabus.db.DriverFactory
import com.example.xalabus.ui.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val driverFactory = DriverFactory(applicationContext)

        setContent {
            App(driverFactory)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    val driverFactory = DriverFactory(LocalContext.current)
    App(driverFactory)
}
