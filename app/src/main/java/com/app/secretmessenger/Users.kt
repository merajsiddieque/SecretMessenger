package com.app.secretmessenger

data class Users(
    val profilePicBase64: String,
    val name: String,
    val field: String // This could be full name or email
)
