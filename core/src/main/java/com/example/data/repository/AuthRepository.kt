package com.example.data.repository

import com.example.data.api.ChettinadApiService
import com.example.data.api.PersistentCookieJar
import com.example.domain.model.Role
import com.example.domain.model.User
import com.example.util.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

open class AuthRepository(
    private val apiService: ChettinadApiService,
    private val tokenManager: TokenManager,
    private val cookieJar: PersistentCookieJar
) {
    private val _currentUser = MutableStateFlow<User?>(tokenManager.getUser())
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    open suspend fun login(credentials: Map<String, String>): Result<User> {
        return loginStaff(credentials)
    }

    open suspend fun loginStaff(credentials: Map<String, String>): Result<User> {
        return try {
            val response = apiService.loginStaff(credentials)
            val role = try {
                Role.valueOf(response.role.uppercase())
            } catch (e: Exception) {
                clearLocalSession()
                return Result.failure(SecurityException("Unknown staff role: ${response.role}"))
            }

            if (role == Role.PATIENT) {
                clearLocalSession()
                return Result.failure(SecurityException("Patient accounts cannot access the Staff application"))
            }

            tokenManager.saveToken(response.access_token)
            val user = User(
                id = response.userId,
                name = response.name ?: response.userId,
                role = role,
                department = null,
                isActive = true
            )
            tokenManager.saveUser(user)
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            clearLocalSession()
            Result.failure(e)
        }
    }

    open suspend fun loginPatient(credentials: Map<String, String>): Result<User> {
        return try {
            val response = try {
                apiService.login(credentials)
            } catch (e: Exception) {
                apiService.loginStaff(credentials)
            }

            val role = try {
                Role.valueOf(response.role.uppercase())
            } catch (e: Exception) {
                clearLocalSession()
                return Result.failure(SecurityException("Unknown patient role: ${response.role}"))
            }

            if (role != Role.PATIENT) {
                clearLocalSession()
                return Result.failure(SecurityException("Staff accounts cannot access the Patient application"))
            }

            tokenManager.saveToken(response.access_token)
            val user = User(
                id = response.userId,
                name = response.name ?: response.userId,
                role = role,
                department = null,
                isActive = true
            )
            tokenManager.saveUser(user)
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            clearLocalSession()
            Result.failure(e)
        }
    }
    
    open suspend fun logout(): Result<Unit> {
        return try {
            apiService.logout()
            clearLocalSession()
            Result.success(Unit)
        } catch (e: Exception) {
            clearLocalSession()
            Result.failure(e)
        }
    }

    open fun clearLocalSession() {
        tokenManager.clear()
        cookieJar.clear()
        _currentUser.value = null
    }

    open fun saveUserSession(user: User, token: String) {
        require(!token.startsWith("firebase_token_") && !token.startsWith("demo_google_token_")) {
            "Synthetic tokens cannot be stored in TokenManager. Only backend-issued JWTs are accepted."
        }
        tokenManager.saveToken(token)
        tokenManager.saveUser(user)
        _currentUser.value = user
    }

    open fun getStoredUser(): User? {
        return tokenManager.getUser()
    }
}
