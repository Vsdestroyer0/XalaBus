package com.example.xalabus.ui.reports

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.xalabus.ui.map.rememberMapViewWithLifecycle
import com.example.xalabus.ui.viewmodel.IncidentViewModel
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

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

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var loadedStyle  by remember { mutableStateOf<Style?>(null) }

    // Inicialización del mapa — sólo una vez
    LaunchedEffect(Unit) {
        mapView.getMapAsync { map ->
            mapLibreMap = map
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(xalapaCenter, 13.0))

            map.addOnMapClickListener { latLng ->
                viewModel.updateLocation(latLng.latitude, latLng.longitude)
                true
            }

            val styleFileName = if (isDarkMode) "style_dark.json" else "style.json"

            val styleJson = try {
                context.assets.open(styleFileName).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                ""
            }

            val mbtilesDir = if (mapStylePath != null) {
                java.io.File(mapStylePath).parent
            } else {
                val defaultFile = java.io.File(context.filesDir, "xalapa.mbtiles")
                if (defaultFile.exists()) context.filesDir.absolutePath else null
            }

            val finalStyle = if (styleJson.isNotEmpty() && mbtilesDir != null) {
                styleJson.replace("{mbtiles_path}", mbtilesDir)
            } else {
                // Fallback a OpenStreetMap tiles (no requiere API key)
                """
                {
                  "version": 8,
                  "sources": {
                    "osm": {
                      "type": "raster",
                      "tiles": ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
                      "tileSize": 256,
                      "attribution": "© OpenStreetMap contributors"
                    }
                  },
                  "layers": [{
                    "id": "osm-layer",
                    "type": "raster",
                    "source": "osm"
                  }]
                }
                """.trimIndent()
            }

            map.setStyle(Style.Builder().fromJson(finalStyle)) { style ->
                loadedStyle = style

                // Fuente GeoJSON para el marcador
                style.addSource(GeoJsonSource(MARKER_SOURCE))

                // CircleLayer: no depende de sprites — funciona siempre
                style.addLayer(
                    CircleLayer(MARKER_LAYER, MARKER_SOURCE).apply {
                        setProperties(
                            PropertyFactory.circleRadius(10f),
                            PropertyFactory.circleColor(android.graphics.Color.RED),
                            PropertyFactory.circleStrokeWidth(2f),
                            PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE),
                        )
                    }
                )

                // Renderizar posición inicial si ya hay una seleccionada
                updateMarker(style, selectedLat, selectedLng)
            }
        }
    }

    // Actualizar marcador cuando cambia la posición seleccionada
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
    source.setGeoJson(FeatureCollection.fromFeature(feature))
}
