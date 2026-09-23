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
    private val outbox = OutboxStore()

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
        try {
            api.sendText(chatId, TextRequest(text.trim()))
        } catch (e: Exception) {
            // offline: queue for automatic retry
            outbox.add(PendingSend(chatId = chatId, text = text.trim()))
            throw OfflineQueued(e)
        }
    }

    suspend fun sendImage(chatId: String, uri: Uri) {
        try {
            uploadImage(chatId, uri.toString())
        } catch (e: Exception) {
            outbox.add(PendingSend(chatId = chatId, imageUri = uri.toString()))
            throw OfflineQueued(e)
        }
    }

    suspend fun sendFile(chatId: String, uri: Uri, displayName: String) {
        try {
            uploadFile(chatId, uri.toString(), displayName)
        } catch (e: Exception) {
            outbox.add(PendingSend(chatId = chatId, fileUri = uri.toString(), fileName = displayName))
            throw OfflineQueued(e)
        }
    }

    /** Try to flush all queued messages. Returns remaining count. */
    suspend fun flushOutbox(): Int {
        val pending = outbox.list()
        for (p in pending) {
            try {
                if (p.fileUri.isNotBlank()) {
                    uploadFile(p.chatId, p.fileUri, p.fileName)
                } else if (p.imageUri.isNotBlank()) {
                    uploadImage(p.chatId, p.imageUri)
                } else {
                    api.sendText(p.chatId, TextRequest(p.text))
                }
                outbox.remove(p.tempId)
            } catch (_: Exception) {
                // stop at first failure, keep order
                break
            }
        }
        return outbox.list().size
    }

    fun pendingFor(chatId: String): List<PendingSend> = outbox.forChat(chatId)

    private suspend fun uploadImage(chatId: String, uriString: String) {
        val ctx = ChatApp.instance
        val uri = Uri.parse(uriString)
        val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Cannot read image")
        val mime = ctx.contentResolver.getType(uri) ?: "image/jpeg"
        val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "photo.jpg", body)
        api.sendImage(chatId, part)
    }

    private suspend fun uploadFile(chatId: String, uriString: String, displayName: String) {
        val ctx = ChatApp.instance
        val uri = Uri.parse(uriString)
        val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Cannot read file")
        if (bytes.size > 25 * 1024 * 1024) throw IllegalArgumentException("File over 25 MB")
        val mime = ctx.contentResolver.getType(uri) ?: "application/octet-stream"
        val safeName = displayName.take(80).ifBlank { "file" }
        val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", safeName, body)
        api.sendFile(chatId, part)
    }

    class OfflineQueued(cause: Exception) : Exception("No connection — message queued, will send automatically")
}
