package com.example.xalabus.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.xalabus.ui.viewmodel.IncidentViewModel

@Composable
actual fun XalapaIncidentMap(
    viewModel: IncidentViewModel,
    isDarkMode: Boolean,
    mapStylePath: String?,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text("Mapa no disponible en iOS", style = MaterialTheme.typography.bodySmall)
    }
}
