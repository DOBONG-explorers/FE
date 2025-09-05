package kr.ac.duksung.dobongzip.ui.chat

data class ChatMessage(
    val text: String,
    val isUser: Boolean // true = 사용자, false = 챗봇
)
