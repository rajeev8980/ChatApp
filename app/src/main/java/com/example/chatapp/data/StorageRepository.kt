package com.example.chatapp.data

import android.net.Uri

/** Image sending now goes through ChatRepository (multipart to FastAPI). */
class StorageRepository {
    private val chats = ChatRepository()

    suspend fun uploadChatImage(chatId: String, uri: Uri) {
        chats.sendImage(chatId, uri)
    }
}
