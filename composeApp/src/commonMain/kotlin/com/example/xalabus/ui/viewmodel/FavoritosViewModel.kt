package com.example.xalabus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.data.SupabaseClientProvider
import com.example.xalabus.data.favoritos.Favorito
import com.example.xalabus.data.favoritos.FavoritosRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estados de la operación de favoritos (CU-10) */
sealed class FavoritosUiState {
    object Idle : FavoritosUiState()
    object Loading : FavoritosUiState()
    data class Success(val favoritos: List<Favorito> = emptyList()) : FavoritosUiState()
    data class Error(val message: String) : FavoritosUiState()
}

class FavoritosViewModel : ViewModel() {

    private val repository = FavoritosRepository()
    private val supabase   = SupabaseClientProvider.client

    private val _uiState = MutableStateFlow<FavoritosUiState>(FavoritosUiState.Idle)
    val uiState: StateFlow<FavoritosUiState> = _uiState.asStateFlow()

    /** Indica si la ruta actual ya está en favoritos del usuario */
    private val _isCurrentRouteFavorite = MutableStateFlow(false)
    val isCurrentRouteFavorite: StateFlow<Boolean> = _isCurrentRouteFavorite.asStateFlow()

    /** Obtiene el userId de la sesión activa, o null si no hay sesión */
    private fun getCurrentUserId(): String? =
        supabase.auth.currentSessionOrNull()?.user?.id

    /**
     * CU-10 — Verifica si la ruta ya está en favoritos.
     * Llamar al seleccionar una ruta en MapDetailView.
     */
    fun checkIfFavorite(routeId: String) {
        val userId = getCurrentUserId() ?: run {
            _isCurrentRouteFavorite.value = false
            return
        }
        viewModelScope.launch {
            try {
                _isCurrentRouteFavorite.value = repository.isFavorito(userId, routeId)
            } catch (e: Exception) {
                _isCurrentRouteFavorite.value = false
            }
        }
    }

    /**
     * CU-10 — Agrega la ruta a favoritos (guardado persistente en Supabase).
     * Ex-01: error al guardar → estado Error.
     */
    fun addToFavorites(routeId: String) {
        val userId = getCurrentUserId() ?: run {
            _uiState.value = FavoritosUiState.Error("Debes iniciar sesión para guardar favoritos.")
            return
        }
        _uiState.value = FavoritosUiState.Loading
        viewModelScope.launch {
            try {
                repository.addFavorito(Favorito(userId = userId, routeId = routeId))
                _isCurrentRouteFavorite.value = true
                _uiState.value = FavoritosUiState.Success()
            } catch (e: Exception) {
                _uiState.value = FavoritosUiState.Error(
                    "Error al guardar en favoritos: ${e.message}"
                )
            }
        }
    }

    /**
     * CU-10 — Elimina la ruta de favoritos.
     */
    fun removeFromFavorites(routeId: String) {
        val userId = getCurrentUserId() ?: return
        _uiState.value = FavoritosUiState.Loading
        viewModelScope.launch {
            try {
                repository.removeFavorito(userId, routeId)
                _isCurrentRouteFavorite.value = false
                _uiState.value = FavoritosUiState.Success()
            } catch (e: Exception) {
                _uiState.value = FavoritosUiState.Error(
                    "Error al eliminar de favoritos: ${e.message}"
                )
            }
        }
    }

    /**
     * Carga todos los favoritos del usuario autenticado.
     * Útil para una futura pantalla de lista de favoritos.
     */
    fun loadUserFavorites() {
        val userId = getCurrentUserId() ?: run {
            _uiState.value = FavoritosUiState.Error("Inicia sesión para ver tus favoritos.")
            return
        }
        _uiState.value = FavoritosUiState.Loading
        viewModelScope.launch {
            try {
                val list = repository.getFavoritosByUser(userId)
                _uiState.value = FavoritosUiState.Success(list)
            } catch (e: Exception) {
                _uiState.value = FavoritosUiState.Error(
                    "Error al cargar favoritos: ${e.message}"
                )
            }
        }
    }

    fun resetState() { _uiState.value = FavoritosUiState.Idle }
}
