package com.app.secretmessenger

data class Message(
    val username: String,
    val content: String,
    val time: String,
    val timestamp: Long = 0L,
    val fileData: String? = null,
    var documentId: String? = null // Added to store Firestore document ID
)