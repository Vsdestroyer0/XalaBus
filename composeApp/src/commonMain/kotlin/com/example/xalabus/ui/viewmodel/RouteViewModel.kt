package com.example.xalabus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.data.repository.RouteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import xalabus.composeapp.generated.resources.Res

class RouteViewModel(private val repository: RouteRepository) : ViewModel() {

    private val _isDataLoaded = MutableStateFlow(false)
    val isDataLoaded: StateFlow<Boolean> = _isDataLoaded

    @OptIn(ExperimentalResourceApi::class)
    fun initializeData() {
        viewModelScope.launch {
            try {
                val bytes = Res.readBytes("files/master_routes_optimized.json")
                val jsonString = bytes.decodeToString()

                repository.checkAndPrepopulate(jsonString)

                _isDataLoaded.value = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}