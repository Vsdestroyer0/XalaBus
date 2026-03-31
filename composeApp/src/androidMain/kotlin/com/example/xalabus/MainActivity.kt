package com.example.xalabus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.xalabus.db.DriverFactory
import com.example.xalabus.core.util.AndroidMapFileManager
import com.example.xalabus.ui.App
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Inicializar MapLibre antes de setContent
        MapLibre.getInstance(this, null, WellKnownTileServer.MapLibre)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 1. Mantienes tu DriverFactory actual para SQLDelight
        val driverFactory = DriverFactory(applicationContext)

        // 2. Creas el manejador de archivos para el mapa de Xalapa
        val fileManager = AndroidMapFileManager(applicationContext)

        setContent {
            // Pasamos ambos a la función App de commonMain
            App(driverFactory = driverFactory, fileManager = fileManager)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    val context = LocalContext.current
    val driverFactory = DriverFactory(context)
    val fileManager = AndroidMapFileManager(context)
    App(driverFactory, fileManager)
}