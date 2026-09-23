package com.example.chatapp.data

import android.net.Uri
import com.example.chatapp.ChatApp
import com.example.chatapp.data.model.Chat
import com.example.chatapp.data.model.Message
import com.example.chatapp.data.model.User
import com.example.chatapp.data.remote.DirectRequest
import com.example.chatapp.data.remote.GroupRequest
import com.example.chatapp.data.remote.Network
import com.example.chatapp.data.remote.SocketEvent
import com.example.chatapp.data.remote.SocketManager
import com.example.chatapp.data.remote.TextRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ChatRepository {
    private val api get() = Network.api()

    fun observeMyChats(myUid: String): Flow<List<Chat>> = callbackFlow {
        suspend fun refresh() {
            try { trySend(api.chats().map { it.toDomain() }) } catch (_: Exception) {}
        }
        refresh()
        val poll = launch {
            while (true) {
                delay(10_000)
                refresh()
            }
        }
        val sock = launch {
            SocketManager.events.collect {
                if (it is SocketEvent.Message || it is SocketEvent.Refresh) refresh()
            }
        }
        awaitClose { poll.cancel(); sock.cancel() }
    }

    fun observeUsers(excludeUid: String): Flow<List<User>> = callbackFlow {
        suspend fun refresh() {
            try {
                trySend(api.users().map { it.toDomain() }.filter { it.uid != excludeUid })
            } catch (_: Exception) {}
        }
        refresh()
        val poll = launch {
            while (true) {
                delay(15_000)
                refresh()
            }
        }
        val sock = launch {
            SocketManager.events.collect { refresh() }
        }
        awaitClose { poll.cancel(); sock.cancel() }
    }

    fun observeMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val current = mutableListOf<Message>()
        suspend fun load() {
            try {
                current.clear()
                current.addAll(api.messages(chatId).map { it.toDomain() })
                trySend(current.toList())
            } catch (_: Exception) {}
        }
        load()
        val poll = launch {
            while (true) {
                delay(10_000)
                load()
            }
        }
        val sock = launch {
            SocketManager.events.collect { event ->
                if (event is SocketEvent.Message && event.chatId == chatId) {
                    val msg = event.message.toDomain()
                    if (current.none { it.messageId == msg.messageId }) {
                        current.add(msg)
                        trySend(current.toList())
                    }
                } else if (event is SocketEvent.Refresh) {
                    load()
                }
            }
        }
        awaitClose { poll.cancel(); sock.cancel() }
    }

    suspend fun getOrCreateDirectChat(other: User): String {
        return api.direct(DirectRequest(other.uid)).chatId
    }

    suspend fun getChat(chatId: String): Chat = api.chat(chatId).toDomain()

    suspend fun getUser(uid: String): User = api.user(uid).toDomain()

    suspend fun createGroupChat(name: String, myUid: String, memberUids: List<String>): String {
        return api.group(GroupRequest(name.trim(), memberUids)).chatId
    }

    suspend fun sendText(chatId: String, text: String) {
        if (text.isBlank()) return
        api.sendText(chatId, TextRequest(text.trim()))
    }

    suspend fun sendImage(chatId: String, uri: Uri) {
        val ctx = ChatApp.instance
        val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Cannot read image")
        val mime = ctx.contentResolver.getType(uri) ?: "image/jpeg"
        val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "photo.jpg", body)
        api.sendImage(chatId, part)
    }
}
