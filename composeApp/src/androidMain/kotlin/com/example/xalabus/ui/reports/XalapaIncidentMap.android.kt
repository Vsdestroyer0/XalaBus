package com.example.xalabus.ui.reports

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.xalabus.ui.map.rememberMapViewWithLifecycle
import com.example.xalabus.ui.viewmodel.IncidentViewModel
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import java.io.File

private const val MARKER_SOURCE = "incident-marker-source"
private const val MARKER_LAYER  = "incident-marker-layer"

@Composable
actual fun XalapaIncidentMap(
    viewModel: IncidentViewModel,
    isDarkMode: Boolean,
    mapStylePath: String?,
    modifier: Modifier,
) {
    val context     = LocalContext.current
    val selectedLat by viewModel.selectedLat.collectAsState()
    val selectedLng by viewModel.selectedLng.collectAsState()

    val xalapaCenter = LatLng(19.5273, -96.9239)
    val mapView = rememberMapViewWithLifecycle()

    var mapLibreMap  by remember { mutableStateOf<MapLibreMap?>(null) }
    var loadedStyle  by remember { mutableStateOf<Style?>(null) }

    // Carga el mapa y el estilo (igual que MapScreen.android.kt)
    LaunchedEffect(mapStylePath, isDarkMode) {
        mapView.getMapAsync { map ->
            mapLibreMap = map
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(xalapaCenter, 13.0))

            // Registrar listener de taps
            map.addOnMapClickListener { latLng ->
                viewModel.updateLocation(latLng.latitude, latLng.longitude)
                true
            }

            val styleFileName = if (isDarkMode) "style_dark.json" else "style.json"
            val styleJson = try {
                context.assets.open(styleFileName).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                // fallback: estilo OpenMapTiles desde CARTO (no requiere assets locales)
                """
                {
                  "version": 8,
                  "sources": {
                    "carto-light": {
                      "type": "raster",
                      "tiles": ["https://a.basemaps.cartocdn.com/light_all/{z}/{x}/{y}@2x.png"],
                      "tileSize": 256
                    }
                  },
                  "layers": [{
                    "id": "carto-light-layer",
                    "type": "raster",
                    "source": "carto-light"
                  }]
                }
                """.trimIndent()
            }

            val finalStyle = if (mapStylePath != null) {
                val mapDir = File(mapStylePath).parent ?: ""
                styleJson.replace("{mbtiles_path}", mapDir)
            } else {
                styleJson
            }

            loadedStyle = null
            map.setStyle(Style.Builder().fromJson(finalStyle)) { style ->
                loadedStyle = style
                // Fuente + capa para el marcador del incidente
                style.addSource(GeoJsonSource(MARKER_SOURCE))
                style.addLayer(
                    SymbolLayer(MARKER_LAYER, MARKER_SOURCE).apply {
                        setProperties(
                            PropertyFactory.iconImage("marker-15"),
                            PropertyFactory.iconSize(2.0f),
                            PropertyFactory.iconColor(android.graphics.Color.RED),
                            PropertyFactory.iconAllowOverlap(true),
                            PropertyFactory.iconIgnorePlacement(true),
                        )
                    }
                )
                // Colocar marcador en la posición inicial del ViewModel
                updateMarker(style, selectedLat, selectedLng)
            }
        }
    }

    // Actualizar marcador cada vez que cambian las coordenadas del ViewModel
    LaunchedEffect(selectedLat, selectedLng, loadedStyle) {
        loadedStyle?.let { style ->
            updateMarker(style, selectedLat, selectedLng)
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxSize(),
    )
}

private fun updateMarker(style: Style, lat: Double, lng: Double) {
    val source = style.getSourceAs<GeoJsonSource>(MARKER_SOURCE) ?: return
    val feature = Feature.fromGeometry(Point.fromLngLat(lng, lat))
    source.setGeoJson(feature)
}
