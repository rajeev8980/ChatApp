package com.example.chatapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.ChatRepository
import com.example.chatapp.data.SessionManager
import com.example.chatapp.data.model.Chat
import com.example.chatapp.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val repo = ChatRepository()
    private val myUid: String get() = SessionManager.get().uid

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    init {
        viewModelScope.launch {
            if (myUid.isNotEmpty()) {
                launch { repo.observeMyChats(myUid).collectLatest { _chats.value = it } }
                launch { repo.observeUsers(myUid).collectLatest { _users.value = it } }
            }
        }
    }

    suspend fun openDirectChat(other: User): String {
        _busy.value = true
        try {
            return repo.getOrCreateDirectChat(other)
        } finally {
            _busy.value = false
        }
    }

    suspend fun createGroup(name: String, members: List<String>): String {
        _busy.value = true
        try {
            return repo.createGroupChat(name, myUid, members)
        } finally {
            _busy.value = false
        }
    }
}
