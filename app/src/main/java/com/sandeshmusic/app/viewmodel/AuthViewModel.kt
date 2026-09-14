package com.sandeshmusic.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sandeshmusic.app.data.auth.AuthRepository
import com.sandeshmusic.app.data.auth.AuthUser
import com.sandeshmusic.app.data.auth.GoogleAuthManager
import com.sandeshmusic.app.data.firestore.SyncState
import com.sandeshmusic.app.data.repository.MusicRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val musicRepository: MusicRepository,
    private val googleAuthManager: GoogleAuthManager? = null
) : ViewModel() {

    val currentUser: StateFlow<AuthUser?> = authRepository.currentUserFlow

    val syncState: StateFlow<SyncState> = musicRepository.syncState

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isSignUpMode = MutableStateFlow(false)
    val isSignUpMode: StateFlow<Boolean> = _isSignUpMode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        reloadUserSilently()
    }

    fun reloadUserSilently() {
        viewModelScope.launch {
            authRepository.reloadUser()
        }
    }

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
        _errorMessage.value = null
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
        _errorMessage.value = null
    }

    fun setSignUpMode(isSignUp: Boolean) {
        _isSignUpMode.value = isSignUp
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun toggleAuthMode() {
        _isSignUpMode.value = !_isSignUpMode.value
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun authenticate(onSuccess: () -> Unit = {}) {
        val emailVal = _email.value.trim()
        val passVal = _password.value

        if (emailVal.isBlank()) {
            _errorMessage.value = "Please enter your email address."
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailVal).matches()) {
            _errorMessage.value = "Please enter a valid email address."
            return
        }

        if (passVal.isBlank()) {
            _errorMessage.value = "Please enter your password."
            return
        }

        if (_isSignUpMode.value && passVal.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = if (_isSignUpMode.value) {
                authRepository.signUpWithEmail(emailVal, passVal)
            } else {
                authRepository.signInWithEmail(emailVal, passVal)
            }

            result.fold(
                onSuccess = { user ->
                    _isLoading.value = false
                    _email.value = ""
                    _password.value = ""
                    _successMessage.value = if (_isSignUpMode.value) {
                        "Account created! Verification email has been sent. Please check your inbox."
                    } else {
                        "Welcome back, ${user.email}!"
                    }
                    viewModelScope.launch {
                        try {
                            musicRepository.syncFavoritesWithCloud()
                        } catch (_: Exception) {
                        }
                    }
                    onSuccess()
                },
                onFailure = { error ->
                    _isLoading.value = false
                    _errorMessage.value = error.message ?: "Authentication failed. Please try again."
                }
            )
        }
    }

    fun sendVerificationEmail() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = authRepository.sendVerificationEmail()
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    _successMessage.value = "Verification email sent! Please check your inbox or spam folder."
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Could not send verification email."
                }
            )
        }
    }

    fun reloadUser() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.reloadUser()
            _isLoading.value = false
            result.fold(
                onSuccess = { user ->
                    if (user?.isEmailVerified == true) {
                        _errorMessage.value = null
                        _successMessage.value = "Email verified successfully!"
                    } else {
                        _errorMessage.value = "Email not verified yet. Click the link in your email and tap Refresh."
                    }
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Could not refresh user status."
                }
            )
        }
    }

    fun resetPassword() {
        val emailVal = _email.value.trim()
        if (emailVal.isBlank()) {
            _errorMessage.value = "Enter your email above and tap 'Forgot password' again."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = authRepository.sendPasswordReset(emailVal)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    _successMessage.value = "Password reset link sent to $emailVal. Check your inbox!"
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Could not send reset email. Verify your email."
                }
            )
        }
    }

    fun signInWithGoogle(context: Context, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            val authManager = googleAuthManager ?: GoogleAuthManager(context.applicationContext)
            val tokenResult = authManager.retrieveGoogleIdToken(context)
            tokenResult.fold(
                onSuccess = { idToken ->
                    val authResult = authRepository.signInWithGoogle(idToken)
                    _isLoading.value = false
                    authResult.fold(
                        onSuccess = { user ->
                            _errorMessage.value = null
                            _successMessage.value = "Welcome back, ${user.displayName ?: user.email}!"
                            viewModelScope.launch {
                                try {
                                    musicRepository.syncFavoritesWithCloud()
                                } catch (_: Exception) {
                                }
                            }
                            onSuccess()
                        },
                        onFailure = { error ->
                            _errorMessage.value = error.message ?: "Google Sign-In failed with Firebase."
                        }
                    )
                },
                onFailure = { error ->
                    _isLoading.value = false
                    val msg = error.message ?: "Google Sign-In was not completed."
                    if (error !is CancellationException && !msg.contains("cancelled", ignoreCase = true)) {
                        _errorMessage.value = msg
                    } else {
                        _errorMessage.value = "Sign-In was dismissed. You can try again or use email login."
                    }
                }
            )
        }
    }

    fun signOut() {
        authRepository.signOut()
        _email.value = ""
        _password.value = ""
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun syncFavoritesNow() {
        viewModelScope.launch {
            musicRepository.syncFavoritesWithCloud()
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val musicRepository: MusicRepository,
        private val googleAuthManager: GoogleAuthManager? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                return AuthViewModel(authRepository, musicRepository, googleAuthManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class ${modelClass.name}")
        }
    }
}
