package com.example.chatapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.AuthRepository
import com.example.chatapp.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val profile: User? = null
)

class AuthViewModel : ViewModel() {
    private val repo = AuthRepository()
    private val _state = MutableStateFlow(AuthUiState(isLoggedIn = repo.isLoggedIn))
    val state: StateFlow<AuthUiState> = _state

    init {
        if (repo.isLoggedIn) {
            repo.connectSocket()
            refreshProfile()
        }
    }

    fun login(email: String, password: String, server: String = "") {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                if (server.isNotBlank()) repo.setServer(server)
                val profile = repo.loadOrCreateProfile()
                _state.value = AuthUiState(isLoggedIn = true, profile = profile)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = friendly(e)
                )
            }
        }
    }

    fun register(name: String, email: String, password: String, server: String = "") {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                if (server.isNotBlank()) repo.setServer(server)
                repo.register(name, email, password)
                val profile = repo.loadOrCreateProfile()
                _state.value = AuthUiState(isLoggedIn = true, profile = profile)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = friendly(e)
                )
            }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            try {
                val profile = repo.loadOrCreateProfile()
                _state.value = _state.value.copy(profile = profile, isLoggedIn = true)
            } catch (_: Exception) {}
        }
    }

    fun setOnline(online: Boolean) {
        viewModelScope.launch { repo.setOnline(online) }
    }

    fun logout() {
        viewModelScope.launch { repo.setOnline(false) }
        repo.logout()
        _state.value = AuthUiState(isLoggedIn = false)
    }

    fun currentServer(): String = repo.currentServer()

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun friendly(e: Exception): String {
        val msg = e.localizedMessage ?: "Request failed"
        return when {
            "Unable to resolve host" in msg || "Failed to connect" in msg ->
                "Cannot reach server. Check the Server address and that the backend is running."
            msg.startsWith("HTTP 401") -> "Invalid email or password"
            msg.startsWith("HTTP 400") -> msg.substringAfter(" ", msg)
            else -> msg
        }
    }
}
