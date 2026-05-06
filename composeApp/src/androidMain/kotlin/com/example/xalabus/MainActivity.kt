package com.example.xalabus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.xalabus.db.DriverFactory
import com.example.xalabus.core.util.AndroidMapFileManager
import com.example.xalabus.core.prefs.appContext
import com.example.xalabus.data.SupabaseClientProvider
import com.example.xalabus.ui.App
import io.github.jan.supabase.auth.handleDeeplinks
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Manejar Deep Link de Supabase
        SupabaseClientProvider.client.handleDeeplinks(intent)

        // Inicializar MapLibre antes de setContent
        MapLibre.getInstance(this, null, WellKnownTileServer.MapLibre)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 0. Inyectar contexto para OnboardingPreferences (SharedPreferences)
        appContext = applicationContext

        // 1. Mantienes tu DriverFactory actual para SQLDelight
        val driverFactory = DriverFactory(applicationContext)

        // 2. Creas el manejador de archivos para el mapa de Xalapa
        val fileManager = AndroidMapFileManager(applicationContext)

        setContent {
            // Pasamos ambos a la función App de commonMain
            App(driverFactory = driverFactory, fileManager = fileManager)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        SupabaseClientProvider.client.handleDeeplinks(intent)
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