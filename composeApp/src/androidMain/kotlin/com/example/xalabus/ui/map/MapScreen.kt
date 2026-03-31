package com.example.xalabus.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.example.xalabus.ui.viewmodel.RouteViewModel
import com.mapbox.geojson.Feature
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

@Composable
actual fun MapScreen(viewModel: RouteViewModel) {
    val mapPath by viewModel.mapFilePath.collectAsState()
    val routePoints by viewModel.selectedRoutePoints.collectAsState()

    val xalapaCenter = LatLng(19.5273, -96.9239)
    val mapView = rememberMapViewWithLifecycle()
    val routeColor = MaterialTheme.colorScheme.primary.toArgb()

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize()
    ) { mv ->
        mv.getMapAsync { map ->
            if (map.cameraPosition?.target?.latitude == 0.0) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(xalapaCenter, 13.0))
            }

            mapPath?.let {
                map.setStyle(Style.Builder().fromUri("asset://style.json")) { style ->
                    if (routePoints.isNotEmpty()) {
                        val coords = routePoints.first().map { LatLng(it[1], it[0]) }

                        // 1. Convertimos la geometría a un String JSON
                        // Esto elimina cualquier error de "type mismatch" con Feature
                        val lineString = LineString.fromLngLats(
                            coords.map { Point.fromLngLat(it.longitude, it.latitude) }
                        )
                        val geoJsonString = Feature.fromGeometry(lineString).toJson()

                        val sourceId = "route-source"
                        val layerId = "route-layer"

                        val source = style.getSourceAs<GeoJsonSource>(sourceId)
                        if (source == null) {
                            // 2. Usamos el constructor que solo recibe ID.
                            // Luego le inyectamos el JSON. Es lo más seguro en MapLibre.
                            val newSource = GeoJsonSource(sourceId)
                            newSource.setGeoJson(geoJsonString)
                            style.addSource(newSource)

                            val lineLayer = LineLayer(layerId, sourceId).apply {
                                setProperties(
                                    PropertyFactory.lineColor(routeColor),
                                    PropertyFactory.lineWidth(6f),
                                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                                )
                            }
                            style.addLayer(lineLayer)
                        } else {
                            // 3. Actualizamos usando el String
                            source.setGeoJson(geoJsonString)
                        }

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