package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirebaseAuthService(
    private val firestoreService: FirestoreService
) {
    private val tag = "FirebaseAuthService"

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(tag, "Failed to get FirebaseAuth instance: ${e.message}")
            null
        }
    }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _userProfile = MutableStateFlow<FirestoreUserProfile?>(null)
    val userProfile: StateFlow<FirestoreUserProfile?> = _userProfile.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        auth?.let { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            firebaseAuth.addAuthStateListener { updatedAuth ->
                val user = updatedAuth.currentUser
                _currentUser.value = user
                if (user != null) {
                    scope.launch {
                        val profile = FirestoreUserProfile(
                            uid = user.uid,
                            email = user.email ?: "",
                            displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Authenticated User",
                            photoUrl = user.photoUrl?.toString() ?: "",
                            lastLoginAt = System.currentTimeMillis()
                        )
                        _userProfile.value = profile
                        firestoreService.saveUserProfile(profile)
                    }
                } else {
                    _userProfile.value = null
                }
            }
        }
    }

    suspend fun signInWithGoogleCredential(idToken: String, role: String = "patient"): Result<FirebaseUser> {
        val firebaseAuth = auth ?: return Result.failure(IllegalStateException("FirebaseAuth is not initialized"))
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw IllegalStateException("FirebaseUser is null after Google sign-in")
            _currentUser.value = user

            val profile = FirestoreUserProfile(
                uid = user.uid,
                email = user.email ?: "",
                displayName = user.displayName ?: "Google User",
                photoUrl = user.photoUrl?.toString() ?: "",
                role = role,
                lastLoginAt = System.currentTimeMillis()
            )
            _userProfile.value = profile
            firestoreService.saveUserProfile(profile)

            Log.d(tag, "Google Sign-In successful for user ${user.uid}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(tag, "Google Sign-In failed", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithEmailPassword(email: String, password: String, role: String): Result<FirebaseUser> {
        val firebaseAuth = auth ?: return Result.failure(IllegalStateException("FirebaseAuth is not initialized"))
        return try {
            val authResult = try {
                firebaseAuth.signInWithEmailAndPassword(email, password).await()
            } catch (e: Exception) {
                // If user doesn't exist, try creating account
                firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            }
            val user = authResult.user ?: throw IllegalStateException("FirebaseUser is null")
            _currentUser.value = user

            val profile = FirestoreUserProfile(
                uid = user.uid,
                email = email,
                displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                role = role,
                lastLoginAt = System.currentTimeMillis()
            )
            _userProfile.value = profile
            firestoreService.saveUserProfile(profile)

            Result.success(user)
        } catch (e: Exception) {
            Log.e(tag, "Email/Password auth failed", e)
            Result.failure(e)
        }
    }

    suspend fun signInAnonymously(role: String = "patient"): Result<FirebaseUser> {
        val firebaseAuth = auth ?: return Result.failure(IllegalStateException("FirebaseAuth is not initialized"))
        return try {
            val authResult = firebaseAuth.signInAnonymously().await()
            val user = authResult.user ?: throw IllegalStateException("FirebaseUser is null")
            _currentUser.value = user

            val profile = FirestoreUserProfile(
                uid = user.uid,
                email = "guest_${user.uid.take(6)}@chettinadcare.local",
                displayName = "Guest ${role.replaceFirstChar { it.uppercase() }}",
                role = role,
                lastLoginAt = System.currentTimeMillis()
            )
            _userProfile.value = profile
            firestoreService.saveUserProfile(profile)

            Result.success(user)
        } catch (e: Exception) {
            Log.e(tag, "Anonymous sign-in failed", e)
            Result.failure(e)
        }
    }

    suspend fun signInDemoGoogleUser(
        email: String,
        displayName: String,
        role: String
    ): Result<FirestoreUserProfile> {
        // Creates a simulated / offline-first synchronized Google profile stored in Firestore
        return try {
            val uid = "google_${email.replace("[^a-zA-Z0-9]".toRegex(), "_")}"
            val profile = FirestoreUserProfile(
                uid = uid,
                email = email,
                displayName = displayName,
                photoUrl = "",
                role = role,
                lastLoginAt = System.currentTimeMillis()
            )
            _userProfile.value = profile
            firestoreService.saveUserProfile(profile)
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(tag, "Demo sign-in failed", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.e(tag, "Sign out error", e)
        }
        _currentUser.value = null
        _userProfile.value = null
    }
}
