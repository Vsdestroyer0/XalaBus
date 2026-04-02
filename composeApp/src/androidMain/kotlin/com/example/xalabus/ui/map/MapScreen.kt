package com.example.xalabus.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.xalabus.ui.viewmodel.RouteViewModel
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import com.example.xalabus.core.util.MapFileManager
import java.io.File

@Composable
actual fun MapScreen(
    fileManager: MapFileManager,
    viewModel: RouteViewModel,
    isDarkMode: Boolean
) {
    val mapPath by viewModel.mapFilePath.collectAsState()
    val routePoints by viewModel.selectedRoutePoints.collectAsState()

    val xalapaCenter = LatLng(19.5273, -96.9239)
    val mapView = rememberMapViewWithLifecycle()

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize()
    ) { mv ->
        mv.getMapAsync { map ->
            // 1. Configuración de cámara inicial
            if (map.cameraPosition?.target?.latitude == 0.0) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(xalapaCenter, 13.0))
            }

            mapPath?.let { path ->
                val styleFileName = if (isDarkMode) "style_dark.json" else "style.json"
                val context = mv.context
                val styleJson = context.assets.open(styleFileName).bufferedReader().use { it.readText() }

                val mapDir = File(path).parent ?: ""
                val finalStyle = styleJson.replace("{mbtiles_path}", mapDir)

                map.setStyle(Style.Builder().fromJson(finalStyle)) { style ->

                    // --- LÓGICA DE LA RUTA (LINEA) ---
                    // Dibujamos únicamente el trayecto del autobús
                    if (routePoints.isNotEmpty()) {
                        // Tomamos el primer set de coordenadas (Ida o Vuelta)
                        val rawCoords = routePoints.first()
                        val lineString = LineString.fromLngLats(
                            rawCoords.map { Point.fromLngLat(it[0], it[1]) }
                        )

                        val routeSourceId = "route-source"
                        val routeLayerId = "route-layer"
                        val existingSource = style.getSourceAs<GeoJsonSource>(routeSourceId)

                        if (existingSource == null) {
                            style.addSource(GeoJsonSource(routeSourceId, lineString.toJson()))
                            val lineLayer = LineLayer(routeLayerId, routeSourceId).apply {
                                setProperties(
                                    PropertyFactory.lineColor(android.graphics.Color.parseColor("#00e6ff")),
                                    PropertyFactory.lineWidth(7f),
                                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                                )
                            }
                            style.addLayer(lineLayer)
                        } else {
                            existingSource.setGeoJson(lineString.toJson())
                        }

                        // Ajuste automático de cámara para que la ruta sea visible
                        val boundsCoords = rawCoords.map { LatLng(it[1], it[0]) }
                        val bounds = LatLngBounds.Builder().includes(boundsCoords).build()
                        map.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120), 1000)
                    } else {
                        // Si no hay ruta seleccionada, limpiamos la capa anterior si existía
                        style.getSourceAs<GeoJsonSource>("route-source")?.setGeoJson(LineString.fromLngLats(emptyList()).toJson())
                    }
                }
            }
        }
    }
}

@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = androidx.compose.ui.platform.LocalContext.current
    val mapView = remember { MapView(context) }

    DisposableEffect(mapView) {
        mapView.onCreate(null)
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }
    return mapView
}