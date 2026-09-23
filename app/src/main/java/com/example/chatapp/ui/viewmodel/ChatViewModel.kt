package com.example.chatapp.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.ChatRepository
import com.example.chatapp.data.SessionManager
import com.example.chatapp.data.model.Message
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val chatRepo = ChatRepository()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _queued = MutableStateFlow(0)
    val queued: StateFlow<Int> = _queued

    fun observe(chatId: String) {
        viewModelScope.launch {
            chatRepo.observeMessages(chatId).collectLatest { server ->
                val me = try { SessionManager.get().uid } catch (_: Exception) { "" }
                val pending = chatRepo.pendingFor(chatId).map {
                    Message(
                        messageId = "pending-${it.tempId}",
                        chatId = chatId,
                        senderId = me,
                        senderName = "",
                        text = it.text.ifBlank { "\uD83D\uDCF7 Photo" },
                        timestamp = "",
                        pending = true
                    )
                }
                _messages.value = server + pending
                _queued.value = chatRepo.pendingFor(chatId).size
            }
        }
        // live read-receipts
        viewModelScope.launch {
            com.example.chatapp.data.remote.SocketManager.events.collect { event ->
                if (event is com.example.chatapp.data.remote.SocketEvent.Seen &&
                    event.chatId == chatId
                ) {
                    _messages.value = _messages.value.map { m ->
                        if (m.pending || event.byUid in m.seenBy) m
                        else m.copy(seenBy = m.seenBy + event.byUid)
                    }
                }
            }
        }
        // retry queued sends in background
        viewModelScope.launch {
            while (true) {
                delay(10_000)
                try {
                    val left = chatRepo.flushOutbox()
                    _queued.value = left
                } catch (_: Exception) {}
            }
        }
    }

    fun sendText(chatId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                chatRepo.sendText(chatId, text)
            } catch (e: ChatRepository.OfflineQueued) {
                _error.value = e.message
                refreshPending(chatId)
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            }
        }
    }

    fun sendImage(chatId: String, uri: Uri) {
        viewModelScope.launch {
            _sending.value = true
            try {
                chatRepo.sendImage(chatId, uri)
            } catch (e: ChatRepository.OfflineQueued) {
                _error.value = e.message
                refreshPending(chatId)
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _sending.value = false
            }
        }
    }

    private fun refreshPending(chatId: String) {
        viewModelScope.launch {
            val me = try { SessionManager.get().uid } catch (_: Exception) { "" }
            val pending = chatRepo.pendingFor(chatId).map {
                Message(
                    messageId = "pending-${it.tempId}",
                    chatId = chatId,
                    senderId = me,
                    text = it.text.ifBlank { "\uD83D\uDCF7 Photo" },
                    pending = true
                )
            }
            val server = _messages.value.filter { !it.pending }
            _messages.value = server + pending
            _queued.value = pending.size
        }
    }

    fun clearError() { _error.value = null }
}
