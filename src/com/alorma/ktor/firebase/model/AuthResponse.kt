package com.alorma.ktor.firebase.model

data class AuthResponse(
    val uId: String,
    val email: String?,
    val phoneNumber: String?,
    val displayName: String?,
    val disabled: Boolean = false
)