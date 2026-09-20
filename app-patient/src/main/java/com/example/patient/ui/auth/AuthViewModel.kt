package com.example.patient.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import com.example.data.firebase.FirebaseAuthService
import com.example.data.firebase.FirestoreService
import com.example.data.firebase.FirestoreUserProfile
import com.example.data.repository.AuthRepository
import com.example.domain.model.User
import com.example.domain.model.Role
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val user: User? = null,
    val firebaseUser: FirebaseUser? = null,
    val firestoreProfile: FirestoreUserProfile? = null,
    val isFirestoreConnected: Boolean = true
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val firebaseAuthService: FirebaseAuthService,
    private val firestoreService: FirestoreService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        val user = authRepository.getStoredUser()
        if (user != null && user.role == Role.PATIENT) {
            _uiState.update { it.copy(user = user) }
        } else if (user != null) {
            // Wrong app, clear session
            logout()
        }

        viewModelScope.launch {
            authRepository.currentUser.collect { currentUser ->
                if (currentUser != null && currentUser.role != Role.PATIENT) {
                    logout()
                } else {
                    _uiState.update { it.copy(user = currentUser) }
                }
            }
        }

        viewModelScope.launch {
            firebaseAuthService.currentUser.collect { fbUser ->
                _uiState.update { it.copy(firebaseUser = fbUser) }
            }
        }

        viewModelScope.launch {
            firebaseAuthService.userProfile.collect { profile ->
                _uiState.update { it.copy(firestoreProfile = profile) }
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
            val result = authRepository.login(mapOf("username" to username, "password" to pin))
            result.onSuccess { user ->
                if (user.role != Role.PATIENT) {
                    authRepository.logout()
                    _uiState.update { it.copy(isLoading = false, error = "This account belongs to the Staff application. Access denied.") }
                } else {
                    // Sync user profile to Firestore database
                    val profile = FirestoreUserProfile(
                        uid = "patient_${user.id}",
                        email = "${user.name.lowercase().replace(" ", "")}@chettinadcare.local",
                        displayName = user.name,
                        role = "patient",
                        staffOrPatientId = user.id,
                        lastLoginAt = System.currentTimeMillis()
                    )
                    firestoreService.saveUserProfile(profile)
                    _uiState.update { it.copy(isLoading = false, user = user, firestoreProfile = profile) }
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, error = "Login failed. You can sign in securely with Google or Demo Access.") }
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = firebaseAuthService.signInWithGoogleCredential(idToken, role = "patient")
            result.onSuccess { fbUser ->
                val patientUser = User(
                    id = "PT-${fbUser.uid.take(6).uppercase()}",
                    name = fbUser.displayName ?: "Patient User",
                    role = Role.PATIENT
                )
                authRepository.saveUserSession(patientUser, "firebase_token_${fbUser.uid}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = patientUser,
                        firebaseUser = fbUser
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, error = "Google Sign-In failed: ${error.localizedMessage ?: "Unknown error"}")
                }
            }
        }
    }

    fun loginDemoGoogle(
        email: String = "patient.priya@gmail.com",
        name: String = "Priya Ramanathan (Verified Google)"
    ) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = firebaseAuthService.signInDemoGoogleUser(email, name, role = "patient")
            result.onSuccess { profile ->
                val patientUser = User(
                    id = "PT-1001",
                    name = name,
                    role = Role.PATIENT
                )
                authRepository.saveUserSession(patientUser, "demo_google_token_${profile.uid}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = patientUser,
                        firestoreProfile = profile
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, error = "Google sign-in demo failed: ${error.message}")
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            firebaseAuthService.signOut()
            _uiState.update { it.copy(user = null, firebaseUser = null, firestoreProfile = null) }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        fun provideFactory(
            authRepository: AuthRepository,
            firebaseAuthService: FirebaseAuthService,
            firestoreService: FirestoreService
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(authRepository, firebaseAuthService, firestoreService) as T
                }
            }
    }
}
