package com.app.secretmessenger

data class Message(
    val username: String,
    val content: String,
    val time: String,
    val timestamp: Long = 0L
)
