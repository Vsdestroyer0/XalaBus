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
import androidx.core.content.ContextCompat
import com.example.xalabus.ui.viewmodel.IncidentViewModel
import com.example.xalabus.ui.viewmodel.RouteTimeViewModel
import com.example.xalabus.ui.viewmodel.RouteViewModel
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
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
    isDarkMode: Boolean,
    routeTimeViewModel: RouteTimeViewModel,
    incidentViewModel: IncidentViewModel,
    onUserLocationChanged: (lat: Double, lng: Double) -> Unit
) {
    val context = LocalContext.current
    val mapPath    by viewModel.mapFilePath.collectAsState()
    val routePoints by viewModel.selectedRoutePoints.collectAsState()
    val routeStops  by viewModel.routeStops.collectAsState()
    val incidentes  by incidentViewModel.incidentes.collectAsState()

    val xalapaCenter = LatLng(19.5273, -96.9239)
    val mapView = rememberMapViewWithLifecycle()

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var loadedStyle  by remember { mutableStateOf<Style?>(null) }

    // ── Permisos de ubicación ────────────────────────────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) {
            loadedStyle?.let { style ->
                mapLibreMap?.let { enableLocation(it, style, context, onUserLocationChanged) }
            }
        }
    }

    LaunchedEffect(Unit) {
        // Solicitar permisos si no están concedidos
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
        // CU-13 postcondición: cargar incidentes al abrir el mapa
        incidentViewModel.cargarIncidentes()
    }

    // ── Efecto 1: Inicializar mapa ───────────────────────────────────────
    LaunchedEffect(Unit) {
        mapView.getMapAsync { map ->
            mapLibreMap = map
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(xalapaCenter, 13.0))

            val styleFileName = if (isDarkMode) "style_dark.json" else "style.json"
            val styleJson = try {
                context.assets.open(styleFileName).bufferedReader().use { it.readText() }
            } catch (e: Exception) { "" }

            val mbtilesDir = if (mapPath != null) {
                File(mapPath!!).parent
            } else {
                val defaultFile = File(context.filesDir, "xalapa.mbtiles")
                if (defaultFile.exists()) context.filesDir.absolutePath else null
            }

            val finalStyle = if (styleJson.isNotEmpty() && mbtilesDir != null) {
                styleJson.replace("{mbtiles_path}", mbtilesDir)
            } else {
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
                  "layers": [{"id": "osm-layer", "type": "raster", "source": "osm"}]
                }
                """.trimIndent()
            }

            loadedStyle = null
            map.setStyle(Style.Builder().fromJson(finalStyle)) { style ->
                if (ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    enableLocation(map, style, context, onUserLocationChanged)
                }
                loadedStyle = style
            }
        }
    }

    // ── Efecto 2: Re-aplicar estilo si cambia isDarkMode o mapPath ──────────
    LaunchedEffect(mapPath, isDarkMode) {
        val map = mapLibreMap ?: return@LaunchedEffect
        val styleFileName = if (isDarkMode) "style_dark.json" else "style.json"
        val styleJson = try {
            context.assets.open(styleFileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) { "" }

        val mbtilesDir = if (mapPath != null) {
            File(mapPath!!).parent
        } else {
            val defaultFile = File(context.filesDir, "xalapa.mbtiles")
            if (defaultFile.exists()) context.filesDir.absolutePath else null
        }

        if (styleJson.isNotEmpty() && mbtilesDir != null) {
            val finalStyle = styleJson.replace("{mbtiles_path}", mbtilesDir)
            loadedStyle = null
            map.setStyle(Style.Builder().fromJson(finalStyle)) { style ->
                if (ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    enableLocation(map, style, context, onUserLocationChanged)
                }
                loadedStyle = style
            }
        }
    }

    // ── Efecto 3: Dibujar/actualizar ruta ─────────────────────────────────
    LaunchedEffect(routePoints, loadedStyle) {
        val style = loadedStyle ?: return@LaunchedEffect
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

    // ── Efecto 4: CU-09 paradas aprobadas (naranjas) ────────────────────
    LaunchedEffect(routeStops, loadedStyle) {
        val style = loadedStyle ?: return@LaunchedEffect
        val stopsSourceId = "stops-source"
        val stopsLayerId  = "stops-layer"

        val features   = routeStops.map { stop -> Feature.fromGeometry(Point.fromLngLat(stop.lng, stop.lat)) }
        val collection = FeatureCollection.fromFeatures(features)

        val existing = style.getSourceAs<GeoJsonSource>(stopsSourceId)
        if (existing == null && features.isNotEmpty()) {
            style.addSource(GeoJsonSource(stopsSourceId, collection.toJson()))
            style.addLayer(
                CircleLayer(stopsLayerId, stopsSourceId).apply {
                    setProperties(
                        PropertyFactory.circleRadius(8f),
                        PropertyFactory.circleColor(android.graphics.Color.parseColor("#FF9800")),
                        PropertyFactory.circleStrokeWidth(2f),
                        PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE)
                    )
                }
            )
        } else {
            existing?.setGeoJson(collection.toJson())
        }
    }

    // ── Efecto 5: CU-13 postcondición — incidentes pendientes (rojos) ──────
    LaunchedEffect(incidentes, loadedStyle) {
        val style = loadedStyle ?: return@LaunchedEffect

        val incSourceId = "incidentes-source"
        val incLayerId  = "incidentes-layer"

        val features = incidentes.map { inc ->
            Feature.fromGeometry(Point.fromLngLat(inc.longitud, inc.latitud))
        }
        val collection = FeatureCollection.fromFeatures(features)

        val existing = style.getSourceAs<GeoJsonSource>(incSourceId)
        if (existing == null) {
            style.addSource(GeoJsonSource(incSourceId, collection.toJson()))
            style.addLayer(
                CircleLayer(incLayerId, incSourceId).apply {
                    setProperties(
                        PropertyFactory.circleRadius(12f),
                        PropertyFactory.circleColor(android.graphics.Color.parseColor("#E53935")),
                        PropertyFactory.circleStrokeWidth(2f),
                        PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE),
                        PropertyFactory.circleOpacity(0.9f)
                    )
                }
            )
        } else {
            existing.setGeoJson(collection.toJson())
        }
    }

    // ── UI ─────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory  = { mapView },
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun enableLocation(
    map: MapLibreMap,
    style: Style,
    context: android.content.Context,
    onUserLocationChanged: (lat: Double, lng: Double) -> Unit
) {
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

    val request = LocationEngineRequest.Builder(2_000L)
        .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
        .setFastestInterval(1_000L)
        .build()

    locationComponent.locationEngine?.requestLocationUpdates(
        request,
        object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult?) {
                result?.lastLocation?.let {
                    onUserLocationChanged(it.latitude, it.longitude)
                }
            }
            override fun onFailure(exception: Exception) {}
        },
        null
    )
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
