package com.example.xalabus.ui.map

import androidx.compose.runtime.Composable
import com.example.xalabus.ui.viewmodel.RouteViewModel
import com.example.xalabus.ui.viewmodel.RouteTimeViewModel
import com.example.xalabus.core.util.MapFileManager

/** CU-11: se agrega routeTimeViewModel para mostrar el panel de tiempo estimado */
@Composable
expect fun MapScreen(
    fileManager: MapFileManager,
    viewModel: RouteViewModel,
    isDarkMode: Boolean,
    routeTimeViewModel: RouteTimeViewModel,
    onUserLocationChanged: (lat: Double, lng: Double) -> Unit = { _, _ -> },
)
