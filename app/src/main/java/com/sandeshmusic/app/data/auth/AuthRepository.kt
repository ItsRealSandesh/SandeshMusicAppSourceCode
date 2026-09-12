package com.sandeshmusic.app.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _currentUserFlow = MutableStateFlow<AuthUser?>(getCurrentUser())
    val currentUserFlow: StateFlow<AuthUser?> = _currentUserFlow.asStateFlow()

    init {
        // Listen to Auth state and IdToken changes from Firebase
        val authListener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser?.let {
                AuthUser(
                    uid = it.uid,
                    email = it.email ?: "User",
                    displayName = it.displayName,
                    isEmailVerified = it.isEmailVerified
                )
            }
            _currentUserFlow.value = user
        }
        val idTokenListener = FirebaseAuth.IdTokenListener { auth ->
            val user = auth.currentUser?.let {
                AuthUser(
                    uid = it.uid,
                    email = it.email ?: "User",
                    displayName = it.displayName,
                    isEmailVerified = it.isEmailVerified
                )
            }
            _currentUserFlow.value = user
        }
        firebaseAuth.addAuthStateListener(authListener)
        firebaseAuth.addIdTokenListener(idTokenListener)

        // Asynchronously reload the current user from Firebase server to fetch fresh isEmailVerified status
        scope.launch {
            refreshUserState(forceReload = true)
        }
    }

    suspend fun refreshUserState(forceReload: Boolean = false): AuthUser? = withContext(Dispatchers.IO) {
        try {
            val user = firebaseAuth.currentUser
            if (user != null) {
                if (forceReload) {
                    try {
                        user.reload().await()
                    } catch (_: Exception) {
                    }
                }
                val updated = AuthUser(
                    uid = user.uid,
                    email = user.email ?: "User",
                    displayName = user.displayName,
                    isEmailVerified = user.isEmailVerified
                )
                _currentUserFlow.value = updated
                updated
            } else {
                _currentUserFlow.value = null
                null
            }
        } catch (_: Exception) {
            _currentUserFlow.value
        }
    }

    fun getCurrentUser(): AuthUser? {
        return firebaseAuth.currentUser?.let {
            AuthUser(
                uid = it.uid,
                email = it.email ?: "User",
                displayName = it.displayName,
                isEmailVerified = it.isEmailVerified
            )
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> = withContext(Dispatchers.IO) {
        try {
            val trimmedEmail = email.trim()
            if (trimmedEmail.isBlank() || password.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Email and password cannot be empty."))
            }

            val authResult = firebaseAuth.signInWithEmailAndPassword(trimmedEmail, password).await()
            val user = authResult.user
            if (user != null) {
                try {
                    // Force reload immediately on sign-in so verified status is refreshed from server
                    user.reload().await()
                } catch (_: Exception) {
                }

                val authUser = AuthUser(
                    uid = user.uid,
                    email = user.email ?: trimmedEmail,
                    displayName = user.displayName,
                    isEmailVerified = user.isEmailVerified
                )
                _currentUserFlow.value = authUser
                Result.success(authUser)
            } else {
                Result.failure(IllegalStateException("Sign in failed. No user found."))
            }
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<AuthUser> = withContext(Dispatchers.IO) {
        try {
            val trimmedEmail = email.trim()
            if (trimmedEmail.isBlank() || password.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Email and password cannot be empty."))
            }
            if (password.length < 6) {
                return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
            }

            val authResult = firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, password).await()
            val user = authResult.user
            if (user != null) {
                // Send verification email upon registration
                try {
                    user.sendEmailVerification().await()
                } catch (_: Exception) {
                }

                val authUser = AuthUser(
                    uid = user.uid,
                    email = user.email ?: trimmedEmail,
                    displayName = user.displayName,
                    isEmailVerified = user.isEmailVerified
                )
                _currentUserFlow.value = authUser
                Result.success(authUser)
            } else {
                Result.failure(IllegalStateException("Sign up failed. User could not be created."))
            }
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    suspend fun sendVerificationEmail(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = firebaseAuth.currentUser
                ?: return@withContext Result.failure(IllegalStateException("No user currently logged in."))
            user.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    suspend fun reloadUser(): Result<AuthUser?> = withContext(Dispatchers.IO) {
        try {
            val updated = refreshUserState(forceReload = true)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val trimmed = email.trim()
            if (trimmed.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Please enter your email address."))
            }
            firebaseAuth.sendPasswordResetEmail(trimmed).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    fun signOut() {
        try {
            firebaseAuth.signOut()
            _currentUserFlow.value = null
        } catch (_: Exception) {
            _currentUserFlow.value = null
        }
    }

    private fun mapAuthException(e: Exception): Exception {
        val message = when (e) {
            is FirebaseAuthInvalidCredentialsException -> "Invalid email or password. Please check and try again."
            is FirebaseAuthUserCollisionException -> "An account with this email already exists. Try signing in instead."
            is FirebaseAuthWeakPasswordException -> "Password is too weak. Please use at least 6 characters."
            is FirebaseAuthException -> e.message ?: "Authentication failed."
            else -> e.localizedMessage ?: "An unexpected error occurred."
        }
        return Exception(message, e)
    }
}
