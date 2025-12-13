package com.example.iotapp.model

import android.net.Uri

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val imageUri: Uri? = null,
    val fileUri: Uri? = null,
    val fileName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

