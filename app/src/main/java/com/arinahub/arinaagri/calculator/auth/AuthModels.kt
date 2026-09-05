package com.arinahub.arinaagri.calculator.auth

data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
)
