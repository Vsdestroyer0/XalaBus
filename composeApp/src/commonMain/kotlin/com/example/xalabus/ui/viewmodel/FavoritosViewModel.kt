package com.example.xalabus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.core.util.ErrorMapper
import com.example.xalabus.data.SupabaseClientProvider
import com.example.xalabus.data.favoritos.Favorito
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** CU-10: Estados de la UI de favoritos */
sealed class FavoritosUiState {
    object Idle    : FavoritosUiState()
    object Loading : FavoritosUiState()
    data class Success(val favoritos: List<Favorito>) : FavoritosUiState()
    data class Error(val message: String) : FavoritosUiState()
}

class FavoritosViewModel : ViewModel() {

    private val supabase = SupabaseClientProvider.client

    private val _uiState = MutableStateFlow<FavoritosUiState>(FavoritosUiState.Idle)
    val uiState: StateFlow<FavoritosUiState> = _uiState.asStateFlow()

    /** true si la ruta actualmente seleccionada está en favoritos */
    private val _isCurrentRouteFavorite = MutableStateFlow(false)
    val isCurrentRouteFavorite: StateFlow<Boolean> = _isCurrentRouteFavorite.asStateFlow()

    /** Carga todos los favoritos del usuario autenticado */
    fun loadUserFavorites() {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: run {
            _uiState.value = FavoritosUiState.Error("Inicia sesión para ver tus favoritos.")
            return
        }
        _uiState.value = FavoritosUiState.Loading
        viewModelScope.launch {
            try {
                val result = supabase.postgrest
                    .from("favoritos")
                    .select(Columns.ALL) {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<Favorito>()
                _uiState.value = FavoritosUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = FavoritosUiState.Error(
                    ErrorMapper.toUserMessage(e, "al cargar favoritos")
                )
            }
        }
    }

    /** Verifica si una ruta específica ya está en favoritos */
    fun checkIfFavorite(routeId: String) {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: run {
            _isCurrentRouteFavorite.value = false
            return
        }
        viewModelScope.launch {
            try {
                val result = supabase.postgrest
                    .from("favoritos")
                    .select(Columns.ALL) {
                        filter {
                            eq("user_id", userId)
                            eq("route_id", routeId)
                        }
                    }
                    .decodeList<Favorito>()
                _isCurrentRouteFavorite.value = result.isNotEmpty()
            } catch (_: Exception) {
                _isCurrentRouteFavorite.value = false
            }
        }
    }

    /** Agrega una ruta a favoritos */
    fun addToFavorites(routeId: String) {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        viewModelScope.launch {
            try {
                supabase.postgrest
                    .from("favoritos")
                    .insert(mapOf("user_id" to userId, "route_id" to routeId))
                _isCurrentRouteFavorite.value = true
                // Refrescar la lista si estaba abierta
                if (_uiState.value is FavoritosUiState.Success) loadUserFavorites()
            } catch (e: Exception) {
                // Duplicado silencioso: no mostramos error si ya existe
                val msg = ErrorMapper.toUserMessage(e)
                if (!msg.contains("ya existe")) {
                    _uiState.value = FavoritosUiState.Error(msg)
                }
            }
        }
    }

    /** Elimina una ruta de favoritos */
    fun removeFromFavorites(routeId: String) {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        viewModelScope.launch {
            try {
                supabase.postgrest
                    .from("favoritos")
                    .delete {
                        filter {
                            eq("user_id", userId)
                            eq("route_id", routeId)
                        }
                    }
                _isCurrentRouteFavorite.value = false
                // Refrescar la lista si estaba abierta
                if (_uiState.value is FavoritosUiState.Success) loadUserFavorites()
            } catch (e: Exception) {
                _uiState.value = FavoritosUiState.Error(
                    ErrorMapper.toUserMessage(e, "al quitar favorito")
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = FavoritosUiState.Idle
        _isCurrentRouteFavorite.value = false
    }
}
