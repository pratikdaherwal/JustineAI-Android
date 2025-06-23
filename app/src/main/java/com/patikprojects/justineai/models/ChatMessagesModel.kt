package com.patikprojects.justineai.models

data class ChatMessagesModel (
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)