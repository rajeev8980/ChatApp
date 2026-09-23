package com.example.chatapp.data.model

/** Formats backend ISO timestamps ("2026-09-22T11:20:33.123+00:00") as HH:mm. */
fun shortTime(iso: String): String {
    if (iso.isBlank()) return ""
    val m = Regex("T(\\d{2}:\\d{2})").find(iso)
    return m?.groupValues?.get(1) ?: iso.take(16)
}

data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val online: Boolean = false,
    val lastSeen: String = ""
)

data class Chat(
    val chatId: String = "",
    val name: String = "",
    val isGroup: Boolean = false,
    val members: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTime: String = "",
    val lastSenderId: String = "",
    val photoUrl: String = "",
    val createdBy: String = ""
)

data class Message(
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val timestamp: String = ""
) {
    val isImage: Boolean get() = imageUrl.isNotBlank()
}
