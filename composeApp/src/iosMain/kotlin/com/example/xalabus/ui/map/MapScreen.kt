package com.example.xalabus.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.xalabus.ui.viewmodel.RouteViewModel

@Composable
actual fun MapScreen(viewModel: RouteViewModel) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Map not yet implemented for iOS")
    }
}
