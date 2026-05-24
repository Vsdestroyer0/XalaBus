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

/** Estados posibles de las operaciones de favoritos (CU-10) */
sealed class FavoritosUiState {
    object Idle : FavoritosUiState()
    object Loading : FavoritosUiState()
    data class Success(val favoritos: List<Favorito> = emptyList()) : FavoritosUiState()
    data class Error(val message: String) : FavoritosUiState()
}

/**
 * CU-10: ViewModel para guardar y gestionar rutas favoritas del usuario.
 * Expone:
 * - [uiState]: estado general de la operación (Idle/Loading/Success/Error)
 * - [isCurrentRouteFavorite]: si la ruta actualmente seleccionada está en favoritos
 */
class FavoritosViewModel : ViewModel() {

    private val repository = FavoritosRepository()
    private val supabase   = SupabaseClientProvider.client

    private val _uiState = MutableStateFlow<FavoritosUiState>(FavoritosUiState.Idle)
    val uiState: StateFlow<FavoritosUiState> = _uiState.asStateFlow()

    private val _isCurrentRouteFavorite = MutableStateFlow(false)
    val isCurrentRouteFavorite: StateFlow<Boolean> = _isCurrentRouteFavorite.asStateFlow()

    /** Devuelve el userId del usuario autenticado, o null si no hay sesión */
    private fun currentUserId(): String? =
        supabase.auth.currentSessionOrNull()?.user?.id

    /**
     * Verifica si la [routeId] dada ya está en favoritos del usuario actual.
     * Actualiza [isCurrentRouteFavorite] en consecuencia.
     */
    fun checkIfFavorite(routeId: String) {
        val userId = currentUserId() ?: return
        viewModelScope.launch {
            try {
                _isCurrentRouteFavorite.value = repository.isFavorito(userId, routeId)
            } catch (e: Exception) {
                // Falla silenciosa: el botón mostrará estado "no favorito" por defecto
                _isCurrentRouteFavorite.value = false
            }
        }
    }

    /**
     * CU-10 flujo principal: Agrega [routeId] a la lista de favoritos del usuario.
     * Ex-01: error al guardar → estado Error con mensaje.
     */
    fun addToFavorites(routeId: String) {
        val userId = currentUserId()
        if (userId == null) {
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
     * Elimina [routeId] de los favoritos del usuario.
     * Ex-01: error al eliminar → estado Error con mensaje.
     */
    fun removeFromFavorites(routeId: String) {
        val userId = currentUserId()
        if (userId == null) {
            _uiState.value = FavoritosUiState.Error("No hay sesión activa.")
            return
        }
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
     * Carga todos los favoritos del usuario (para pantalla de perfil/favoritos).
     */
    fun loadFavorites() {
        val userId = currentUserId()
        if (userId == null) {
            _uiState.value = FavoritosUiState.Error("Debes iniciar sesión para ver tus favoritos.")
            return
        }
        _uiState.value = FavoritosUiState.Loading
        viewModelScope.launch {
            try {
                val lista = repository.getFavoritosByUser(userId)
                _uiState.value = FavoritosUiState.Success(lista)
            } catch (e: Exception) {
                _uiState.value = FavoritosUiState.Error(
                    "Error al cargar favoritos: ${e.message}"
                )
            }
        }
    }

    fun resetState() { _uiState.value = FavoritosUiState.Idle }
}
