package com.example.xalabus.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.core.content.ContextCompat
import com.example.xalabus.ui.viewmodel.RouteTimeViewModel
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
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.CircleLayer
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
    isDarkMode: Boolean,
    routeTimeViewModel: RouteTimeViewModel
) {
    val context = LocalContext.current
    val mapPath by viewModel.mapFilePath.collectAsState()
    val routePoints by viewModel.selectedRoutePoints.collectAsState()

    val xalapaCenter = LatLng(19.5273, -96.9239)
    val mapView = rememberMapViewWithLifecycle()

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var loadedStyle by remember { mutableStateOf<Style?>(null) }
    val destination by viewModel.destinationLocation.collectAsState()

    // --- GESTIÓN DE PERMISOS ---
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) {
            loadedStyle?.let { style -> mapLibreMap?.let { enableLocation(it, style, context) } }
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // ── Efecto 1: Inicializar mapa y cargar estilo ────────────────────────────
    LaunchedEffect(mapPath, isDarkMode) {
        val path = mapPath ?: return@LaunchedEffect
        mapView.getMapAsync { map ->
            mapLibreMap = map
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(xalapaCenter, 13.0))

            map.addOnMapLongClickListener { point ->
                viewModel.setDestinationLocation(point.latitude, point.longitude)
                true
            }

            val styleFileName = if (isDarkMode) "style_dark.json" else "style.json"
            val styleJson = context.assets.open(styleFileName).bufferedReader().use { it.readText() }
            val mapDir = File(path).parent ?: ""
            val finalStyle = styleJson.replace("{mbtiles_path}", mapDir)

            loadedStyle = null
            map.setStyle(Style.Builder().fromJson(finalStyle)) { style ->
                if (ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    enableLocation(map, style, context)
                }
                loadedStyle = style
            }
        }
    }

    // ── Efecto 2: Dibujar/actualizar la ruta cuando cambian los puntos ────────
    LaunchedEffect(routePoints, loadedStyle) {
        val style = loadedStyle ?: return@LaunchedEffect
        val map   = mapLibreMap  ?: return@LaunchedEffect

        val routeSourceId = "route-source"
        val routeLayerId  = "route-layer"

        if (routePoints.isNotEmpty()) {
            val allPoints = routePoints.flatten().map { coord -> Point.fromLngLat(coord[0], coord[1]) }
            val lineString = LineString.fromLngLats(allPoints)

            val existingSource = style.getSourceAs<GeoJsonSource>(routeSourceId)
            if (existingSource == null) {
                style.addSource(GeoJsonSource(routeSourceId, lineString.toJson()))
                style.addLayer(
                    LineLayer(routeLayerId, routeSourceId).apply {
                        setProperties(
                            PropertyFactory.lineColor(android.graphics.Color.parseColor("#00e6ff")),
                            PropertyFactory.lineWidth(7f),
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                        )
                    }
                )
            } else {
                existingSource.setGeoJson(lineString.toJson())
            }

            val boundsCoords = routePoints.flatten().map { coord -> LatLng(coord[1], coord[0]) }
            if (boundsCoords.size >= 2) {
                val bounds = LatLngBounds.Builder().includes(boundsCoords).build()
                map.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120), 1000)
            }
        } else {
            style.getSourceAs<GeoJsonSource>(routeSourceId)
                ?.setGeoJson(LineString.fromLngLats(emptyList()).toJson())
        }
    }

    // ── Efecto 3: Dibujar marcador de destino ─────────────────────────────────
    LaunchedEffect(destination, loadedStyle) {
        val style = loadedStyle ?: return@LaunchedEffect
        val dest = destination
        val sourceId = "destination-source"
        val layerId = "destination-layer"

        if (dest != null) {
            val point = Point.fromLngLat(dest.second, dest.first)
            val source = style.getSourceAs<GeoJsonSource>(sourceId)
            if (source == null) {
                style.addSource(GeoJsonSource(sourceId, point.toJson()))
                style.addLayer(CircleLayer(layerId, sourceId).apply {
                    setProperties(
                        PropertyFactory.circleRadius(8f),
                        PropertyFactory.circleColor(android.graphics.Color.RED)
                    )
                })
            } else {
                source.setGeoJson(point.toJson())
            }
        } else {
            // Eliminar o esconder el destino si es nulo
            style.getSourceAs<GeoJsonSource>(sourceId)
                ?.setGeoJson(LineString.fromLngLats(emptyList()).toJson())
        }
    }

    // ── UI: solo el mapa (CU-11 se muestra en el bottom sheet de App.kt) ─────
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        if (destination != null) {
            ExtendedFloatingActionButton(
                onClick = {
                    val loc = mapLibreMap?.locationComponent?.lastKnownLocation
                    if (loc != null) {
                        viewModel.setUserLocation(loc.latitude, loc.longitude)
                        viewModel.calculateBestRoute()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 120.dp), // Separado del borde inferior por otros elementos de la UI
                icon = { Icon(Icons.Default.Directions, contentDescription = "Mejor Ruta") },
                text = { Text("Mejor Ruta") }
            )
        }
    }
}

private fun enableLocation(map: MapLibreMap, style: Style, context: android.content.Context) {
    val locationComponent = map.locationComponent

    val options = LocationComponentOptions.builder(context)
        .foregroundDrawable(R.drawable.ic_user_marker)
        .gpsDrawable(R.drawable.ic_user_marker)
        .bearingDrawable(R.drawable.ic_user_marker)
        .accuracyAlpha(0.0f)
        .build()

    val activationOptions = LocationComponentActivationOptions.builder(context, style)
        .locationComponentOptions(options)
        .build()

    locationComponent.activateLocationComponent(activationOptions)
    locationComponent.isLocationComponentEnabled = true
    locationComponent.renderMode = RenderMode.GPS
    locationComponent.cameraMode  = CameraMode.NONE
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
