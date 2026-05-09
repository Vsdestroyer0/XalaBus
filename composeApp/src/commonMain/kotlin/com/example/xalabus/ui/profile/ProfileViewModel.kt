package com.example.xalabus.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.data.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

sealed class ProfileUiState {
    object Idle : ProfileUiState()
    object Loading : ProfileUiState()
    object Success : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel : ViewModel() {
    private val supabase = SupabaseClientProvider.client

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        val user = supabase.auth.currentUserOrNull()
        if (user != null) {
            val metadata: JsonObject? = user.userMetadata
            if (metadata != null) {
                _name.value = metadata["name"]?.jsonPrimitive?.content ?: ""
            } else {
                _name.value = ""
            }
        } else {
            _name.value = ""
        }
    }

    fun onNameChange(newName: String) {
        _name.value = newName
    }

    fun saveProfile() {
        _uiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            try {
                supabase.auth.updateUser {
                    data = buildJsonObject {
                        put("name", _name.value)
                    }
                }
                _uiState.value = ProfileUiState.Success
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Error al guardar el perfil")
            }
        }
    }

    fun resetState() {
        _uiState.value = ProfileUiState.Idle
    }
}
