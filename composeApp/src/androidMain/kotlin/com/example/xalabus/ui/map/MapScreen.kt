package com.example.xalabus.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.xalabus.ui.viewmodel.RouteViewModel
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import com.example.xalabus.core.util.MapFileManager
import com.example.xalabus.R
import java.io.File

@Composable
actual fun MapScreen(
    fileManager: MapFileManager,
    viewModel: RouteViewModel,
    isDarkMode: Boolean
) {
    val context = LocalContext.current
    val mapPath by viewModel.mapFilePath.collectAsState()
    val routePoints by viewModel.selectedRoutePoints.collectAsState()

    val xalapaCenter = LatLng(19.5273, -96.9239)
    val mapView = rememberMapViewWithLifecycle()
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }

    // --- GESTIÓN DE PERMISOS ---
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) {
            mapLibreMap?.style?.let { style ->
                enableLocation(mapLibreMap!!, style, context)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize()
    ) { mv ->
        mv.getMapAsync { map ->
            mapLibreMap = map
            if (map.cameraPosition?.target?.latitude == 0.0) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(xalapaCenter, 13.0))
            }

            mapPath?.let { path ->
                val styleFileName = if (isDarkMode) "style_dark.json" else "style.json"
                val styleJson = context.assets.open(styleFileName).bufferedReader().use { it.readText() }
                val mapDir = File(path).parent ?: ""
                val finalStyle = styleJson.replace("{mbtiles_path}", mapDir)

                map.setStyle(Style.Builder().fromJson(finalStyle)) { style ->
                    // Activar localización si hay permisos
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        enableLocation(map, style, context)
                    }

                    // --- LÓGICA DE LA RUTA (LINEA) ---
                    if (routePoints.isNotEmpty()) {
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

                        val boundsCoords = rawCoords.map { LatLng(it[1], it[0]) }
                        val bounds = LatLngBounds.Builder().includes(boundsCoords).build()
                        map.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120), 1000)
                    } else {
                        style.getSourceAs<GeoJsonSource>("route-source")?.setGeoJson(LineString.fromLngLats(emptyList()).toJson())
                    }
                }
            }
        }
    }
}

private fun enableLocation(map: MapLibreMap, style: Style, context: android.content.Context) {
    val locationComponent = map.locationComponent
    
    val options = LocationComponentOptions.builder(context)
        .foregroundDrawable(R.drawable.ic_user_marker)
        .gpsDrawable(R.drawable.ic_user_marker) // Icono para modo GPS
        .bearingDrawable(R.drawable.ic_user_marker)
        .accuracyAlpha(0.0f) // Quitamos el círculo de precisión para que se vea más limpio
        .build()

    val activationOptions = LocationComponentActivationOptions.builder(context, style)
        .locationComponentOptions(options)
        .build()

    locationComponent.activateLocationComponent(activationOptions)
    locationComponent.isLocationComponentEnabled = true
    
    // RenderMode.GPS hace que el icono siempre esté "derecho" (hacia el norte)
    locationComponent.renderMode = org.maplibre.android.location.modes.RenderMode.GPS
    locationComponent.cameraMode = org.maplibre.android.location.modes.CameraMode.NONE
}

@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
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
