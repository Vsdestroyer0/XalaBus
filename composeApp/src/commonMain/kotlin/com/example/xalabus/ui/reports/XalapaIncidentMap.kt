package com.example.xalabus.ui.reports

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.xalabus.ui.viewmodel.IncidentViewModel

/**
 * expect/actual: en Android muestra un MapView real de MapLibre;
 * en iOS (y previews) muestra un placeholder simple.
 */
@Composable
expect fun XalapaIncidentMap(
    viewModel: IncidentViewModel,
    isDarkMode: Boolean,
    mapStylePath: String?,
    modifier: Modifier = Modifier,
)
