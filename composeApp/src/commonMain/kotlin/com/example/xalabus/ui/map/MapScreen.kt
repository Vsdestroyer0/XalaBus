package com.example.xalabus.ui.map

import androidx.compose.runtime.Composable
import com.example.xalabus.ui.viewmodel.RouteViewModel
import com.example.xalabus.core.util.MapFileManager

@Composable
expect fun MapScreen(
    fileManager: MapFileManager,
    viewModel: RouteViewModel,
    isDarkMode: Boolean
)
