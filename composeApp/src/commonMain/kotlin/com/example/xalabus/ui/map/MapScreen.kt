package com.example.xalabus.ui.map

import com.example.xalabus.ui.viewmodel.IncidentViewModel
import com.example.xalabus.ui.viewmodel.RouteTimeViewModel
import com.example.xalabus.ui.viewmodel.RouteViewModel
import com.example.xalabus.core.util.MapFileManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun MapScreen(
    fileManager: MapFileManager,
    viewModel: RouteViewModel,
    isDarkMode: Boolean,
    routeTimeViewModel: RouteTimeViewModel,
    incidentViewModel: IncidentViewModel,
    onUserLocationChanged: (lat: Double, lng: Double) -> Unit
)
