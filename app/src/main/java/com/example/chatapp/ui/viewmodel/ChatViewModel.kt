package com.example.chatapp.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.ChatRepository
import com.example.chatapp.data.model.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val chatRepo = ChatRepository()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun observe(chatId: String) {
        viewModelScope.launch {
            chatRepo.observeMessages(chatId).collectLatest { _messages.value = it }
        }
    }

    fun sendText(chatId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                chatRepo.sendText(chatId, text)
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
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _sending.value = false
            }
        }
    }

    fun clearError() { _error.value = null }
}
