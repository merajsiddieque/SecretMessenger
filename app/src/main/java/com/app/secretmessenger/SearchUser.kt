package com.app.secretmessenger

data class SearchUser(
    val profilePicBase64: String,
    val username: String,
    val fullName: String,
    var isSelected: Boolean = false // Track selection state
)