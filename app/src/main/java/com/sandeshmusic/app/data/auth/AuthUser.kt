package com.sandeshmusic.app.data.auth

data class AuthUser(
    val uid: String,
    val email: String,
    val displayName: String? = null,
    val isEmailVerified: Boolean = false
)
