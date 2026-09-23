package com.example.chatapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.chatapp.data.model.Chat
import com.example.chatapp.data.model.User
import com.example.chatapp.data.model.shortTime
import com.example.chatapp.ui.viewmodel.AuthViewModel
import com.example.chatapp.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenChat: (String, String) -> Unit,
    onCreateGroup: () -> Unit,
    onProfile: () -> Unit,
    onLoggedOut: () -> Unit,
    homeVm: HomeViewModel = viewModel(),
    authVm: AuthViewModel = viewModel()
) {
    val chats by homeVm.chats.collectAsState()
    val users by homeVm.users.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ChatApp") },
                actions = {
                    IconButton(onClick = onProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                    IconButton(onClick = {
                        authVm.logout()
                        onLoggedOut()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            if (tab == 1) {
                FloatingActionButton(onClick = onCreateGroup) {
                    Icon(Icons.Default.Add, contentDescription = "New group")
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Chats") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("People") })
            }
            if (tab == 0) {
                if (chats.isEmpty()) {
                    EmptyHint("No chats yet.\nGo to People tab to start chatting.")
                } else {
                    LazyColumn {
                        items(chats, key = { it.chatId }) { chat ->
                            ChatRow(chat = chat, onClick = {
                                onOpenChat(chat.chatId, chatDisplayName(chat))
                            })
                        }
                    }
                }
            } else {
                if (users.isEmpty()) {
                    EmptyHint("No other users found.\nAsk a friend to register.")
                } else {
                    LazyColumn {
                        items(users, key = { it.uid }) { user ->
                            UserRow(user = user, onClick = {
                                scope.launch {
                                    val chatId = homeVm.openDirectChat(user)
                                    onOpenChat(chatId, user.displayName)
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}

fun chatDisplayName(chat: Chat): String =
    if (chat.isGroup) chat.name.ifBlank { "Group" }
    else chat.name.ifBlank { "Chat" }

@Composable
fun ChatRow(chat: Chat, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(url = chat.photoUrl, fallback = chatDisplayName(chat))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(chatDisplayName(chat), style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    chat.lastMessage.ifBlank { if (chat.isGroup) "Group created" else "Say hi!" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            if (chat.lastMessageTime.isNotBlank()) {
                Text(
                    shortTime(chat.lastMessageTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun UserRow(user: User, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(url = user.photoUrl, fallback = user.displayName)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(user.displayName.ifBlank { user.email },
                    style = MaterialTheme.typography.titleMedium)
                Text(
                    if (user.online) "Online" else "Offline",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (user.online) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.Person, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun Avatar(url: String, fallback: String) {
    var failed by remember(url) { mutableStateOf(false) }
    if (url.isNotBlank() && !failed) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.size(48.dp).clip(CircleShape),
            contentScale = ContentScale.Crop,
            onError = { failed = true }
        )
    } else {
        Icon(
            Icons.Default.AccountCircle, contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun EmptyHint(text: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
