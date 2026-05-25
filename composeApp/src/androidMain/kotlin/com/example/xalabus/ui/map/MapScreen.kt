package com.example.xalabus.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import com.example.xalabus.core.util.MapFileManager
import com.example.xalabus.R
import java.io.File

/**
 * CU-11: actual MapScreen para Android.
 * Se agrega un [Box] que superpone el [RouteTimePanel] en la parte inferior
 * del mapa cuando hay una ruta activa. El panel se muestra/oculta según
 * el estado del [RouteTimeViewModel].
 */
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
    // CU-11: ID de la ruta actualmente seleccionada para cargar sus paradas
    val selectedRouteId by viewModel.selectedRouteId.collectAsState()

    val xalapaCenter = LatLng(19.5273, -96.9239)
    val mapView = rememberMapViewWithLifecycle()

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var loadedStyle by remember { mutableStateOf<Style?>(null) }
    // CU-11: controla la visibilidad del panel de tiempo estimado
    var showTimePanel by remember { mutableStateOf(false) }

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

    // CU-11: mostrar el panel cuando hay una ruta seleccionada
    LaunchedEffect(selectedRouteId) {
        showTimePanel = selectedRouteId.isNotBlank()
    }

    // ── Efecto 1: Inicializar mapa y cargar estilo UNA SOLA VEZ ────────────────
    LaunchedEffect(mapPath, isDarkMode) {
        val path = mapPath ?: return@LaunchedEffect
        mapView.getMapAsync { map ->
            mapLibreMap = map
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(xalapaCenter, 13.0))

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

    // ── Efecto 2: Dibujar/actualizar la ruta cuando cambian los puntos ──────────
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

    // ── UI: mapa + panel CU-11 superpuesto en la parte inferior ─────────────
    Box(modifier = Modifier.fillMaxSize()) {

        // Capa 1: el mapa nativo de MapLibre
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // Capa 2 (CU-11): panel de tiempo estimado en la parte inferior
        if (showTimePanel && selectedRouteId.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                RouteTimePanel(
                    routeId = selectedRouteId,
                    routeTimeViewModel = routeTimeViewModel,
                    onDismiss = { showTimePanel = false }
                )
            }
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
