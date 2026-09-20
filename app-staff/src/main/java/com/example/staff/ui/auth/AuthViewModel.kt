package com.example.staff.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.AuthRepository
import com.example.domain.model.User
import com.example.domain.model.Role
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val user: User? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        val user = authRepository.getStoredUser()
        if (user != null && user.role != Role.PATIENT) {
            _uiState.update { it.copy(user = user) }
        } else if (user != null) {
            logout()
        }

        viewModelScope.launch {
            authRepository.currentUser.collect { currentUser ->
                if (currentUser != null && currentUser.role == Role.PATIENT) {
                    logout()
                } else {
                    _uiState.update { it.copy(user = currentUser) }
                }
            }
        }
    }

    fun login(username: String, pin: String) {
        if (username.isBlank() || pin.isBlank()) {
            _uiState.update { it.copy(error = "Please enter credentials.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val result = authRepository.loginStaff(mapOf("username" to username, "password" to pin))
            result.onSuccess { user ->
                _uiState.update { it.copy(isLoading = false, user = user) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message ?: "Authentication failed.") }
            }
        }
    }

    fun loginDemo(role: Role) {
        val (username, password) = when (role) {
            Role.DOCTOR -> "dr_smith" to "doctor123"
            Role.NURSE -> "nurse_joy" to "nurse123"
            Role.ADMIN -> "admin_01" to "admin123"
            Role.PATIENT -> return
        }
        login(username, password)
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { it.copy(user = null) }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        fun provideFactory(
            authRepository: AuthRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(authRepository) as T
                }
            }
    }
}
