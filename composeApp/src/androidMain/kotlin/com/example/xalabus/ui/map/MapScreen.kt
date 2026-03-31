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
            if (map.cameraPosition?.target?.latitude == 0.0) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(xalapaCenter, 13.0))
            }

            mapPath?.let { path ->
                // Carga dinámica del estilo local según el tema
                val styleFileName = if (isDarkMode) "style_dark.json" else "style.json"
                val context = mv.context
                val styleJson = context.assets.open(styleFileName).bufferedReader().use { it.readText() }

                val mapDir = File(path).parent ?: ""
                val finalStyle = styleJson.replace("{mbtiles_path}", mapDir)

                map.setStyle(Style.Builder().fromJson(finalStyle)) { style ->
                    if (routePoints.isNotEmpty()) {
                        val coords = routePoints.first().map { LatLng(it[1], it[0]) }

                        val lineString = LineString.fromLngLats(
                            coords.map { Point.fromLngLat(it.longitude, it.latitude) }
                        )

                        val sourceId = "route-source"
                        val layerId = "route-layer"
                        val source = style.getSourceAs<GeoJsonSource>(sourceId)

                        if (source == null) {
                            val newSource = GeoJsonSource(sourceId, lineString.toJson())
                            style.addSource(newSource)

                            // Azul chillón estilo Google Maps
                            val googleBlue = android.graphics.Color.parseColor("#00e6ff")

                            val lineLayer = LineLayer(layerId, sourceId).apply {
                                setProperties(
                                    PropertyFactory.lineColor(googleBlue),
                                    PropertyFactory.lineWidth(8f),
                                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                                    PropertyFactory.lineBlur(0.4f)
                                )
                            }
                            style.addLayer(lineLayer)
                        } else {
                            source.setGeoJson(lineString.toJson())
                        }

                        // Centrar la cámara en la ruta dibujada
                        val bounds = LatLngBounds.Builder().includes(coords).build()
                        map.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120), 1000)
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