package com.smu.daiary.model

/**
 * Domain model representing an authenticated user.
 * No Android or Firebase imports — safe to use in unit tests.
 */
data class User(
    val uid: String,
    val email: String,
    val displayName: String = ""
)
