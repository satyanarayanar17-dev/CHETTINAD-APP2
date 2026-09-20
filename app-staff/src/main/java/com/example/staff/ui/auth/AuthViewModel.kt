package com.example.staff.ui.auth

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
        if (user != null && user.role != Role.PATIENT) {
            _uiState.update { it.copy(user = user) }
        } else if (user != null) {
            // Wrong app, clear session
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
                if (user.role == Role.PATIENT) {
                    authRepository.logout()
                    _uiState.update { it.copy(isLoading = false, error = "This account is registered for the Chettinad Care patient application.") }
                } else {
                    val profile = FirestoreUserProfile(
                        uid = "staff_${user.id}",
                        email = "${user.name.lowercase().replace(" ", "")}@chettinadhospital.com",
                        displayName = user.name,
                        role = user.role.name.lowercase(),
                        department = when (user.role) {
                            Role.DOCTOR -> "Cardiology / Internal Medicine"
                            Role.NURSE -> "Emergency & Triage"
                            Role.ADMIN -> "Hospital Administration"
                            else -> "Clinical Staff"
                        },
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

    fun loginWithGoogle(idToken: String, preferredRole: Role = Role.DOCTOR) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = firebaseAuthService.signInWithGoogleCredential(
                idToken = idToken,
                role = preferredRole.name.lowercase()
            )
            result.onSuccess { fbUser ->
                val staffUser = User(
                    id = "ST-${fbUser.uid.take(6).uppercase()}",
                    name = fbUser.displayName ?: "Dr. ${fbUser.email?.substringBefore("@") ?: "Staff"}",
                    role = preferredRole
                )
                authRepository.saveUserSession(staffUser, "firebase_token_${fbUser.uid}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = staffUser,
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

    fun loginDemoGoogle(role: Role = Role.DOCTOR) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val (email, name) = when (role) {
                Role.DOCTOR -> "dr.karthik.chettinad@gmail.com" to "Dr. Karthik S. (Verified Google)"
                Role.NURSE -> "nurse.anitha.chettinad@gmail.com" to "Nurse Anitha R. (Verified Google)"
                Role.ADMIN -> "admin.director.chettinad@gmail.com" to "Admin Director (Verified Google)"
                else -> "staff.demo@chettinadhospital.com" to "Staff User"
            }

            val result = firebaseAuthService.signInDemoGoogleUser(
                email = email,
                displayName = name,
                role = role.name.lowercase()
            )
            result.onSuccess { profile ->
                val staffUser = User(
                    id = when (role) {
                        Role.DOCTOR -> "dr_smith"
                        Role.NURSE -> "nurse_joy"
                        Role.ADMIN -> "admin_01"
                        else -> "staff_01"
                    },
                    name = name,
                    role = role
                )
                authRepository.saveUserSession(staffUser, "demo_google_token_${profile.uid}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = staffUser,
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
