package com.example.chatapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.TagFaces
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.chatapp.data.ChatRepository
import com.example.chatapp.data.SessionManager
import com.example.chatapp.data.model.Chat
import com.example.chatapp.data.model.Message
import com.example.chatapp.data.model.User
import com.example.chatapp.data.model.activeText
import com.example.chatapp.data.model.stampText
import com.example.chatapp.data.remote.SocketEvent
import com.example.chatapp.data.remote.SocketManager
import com.example.chatapp.ui.theme.AccentBlue
import com.example.chatapp.ui.theme.BubbleMine
import com.example.chatapp.ui.theme.BubbleTheirs
import com.example.chatapp.ui.theme.BubbleTextMine
import com.example.chatapp.ui.theme.BubbleTextTheirs
import com.example.chatapp.ui.theme.ChatBackground
import com.example.chatapp.ui.theme.ChatDivider
import com.example.chatapp.ui.theme.ChatHeaderBg
import com.example.chatapp.ui.theme.ChatText
import com.example.chatapp.ui.theme.ChatTextDim
import com.example.chatapp.ui.theme.InputBg
import com.example.chatapp.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.delay

val EMOJIS = listOf(
    "😀", "😁", "😂", "🤣", "😊", "😍", "😘", "😎",
    "🤔", "😐", "🙄", "😴", "😷", "🤒", "🤕", "🤢",
    "😈", "👻", "💀", "🤖", "🎃", "😺", "😹", "🙈",
    "👍", "👎", "👏", "🙏", "💪", "👋", "✌️", "🤝",
    "❤️", "💔", "💯", "✨", "🔥", "🎉", "🎂", "🌹",
    "😢", "😭", "😡", "🥳", "🤯", "🥺", "😳", "🤗",
    "👀", "💤", "💩", "👑", "💎", "⚽", "🎵", "🚗",
    "🍕", "☕", "🌙", "☀️", "🌈", "⭐", "✅", "❌"
)

@Composable
fun ChatScreen(
    chatId: String,
    chatName: String,
    onBack: () -> Unit,
    vm: ChatViewModel = viewModel()
) {
    val messages by vm.messages.collectAsState()
    val sending by vm.sending.collectAsState()
    val error by vm.error.collectAsState()
    var input by remember { mutableStateOf("") }
    var showEmoji by remember { mutableStateOf(false) }
    var chat by remember { mutableStateOf<Chat?>(null) }
    var peer by remember { mutableStateOf<User?>(null) }
    val myUid = try { SessionManager.get().uid } catch (_: Exception) { "" }
    val snack = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val repo = remember { ChatRepository() }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) vm.sendImage(chatId, uri)
    }

    LaunchedEffect(chatId) {
        vm.observe(chatId)
        // load chat info + peer presence, refresh on socket events
        suspend fun loadInfo() {
            try {
                val c = repo.getChat(chatId)
                chat = c
                if (!c.isGroup) {
                    val other = c.members.firstOrNull { it != myUid }
                    peer = other?.let { repo.getUser(it) }
                }
            } catch (_: Exception) {}
        }
        loadInfo()
    }
    LaunchedEffect(chatId) {
        while (true) {
            delay(15_000)
            try {
                val c = repo.getChat(chatId)
                chat = c
                if (!c.isGroup) {
                    c.members.firstOrNull { it != myUid }?.let { peer = repo.getUser(it) }
                }
            } catch (_: Exception) {}
        }
    }
    LaunchedEffect(chatId) {
        SocketManager.events.collect {
            if (it is SocketEvent.Message && it.chatId == chatId ||
                it is SocketEvent.Refresh
            ) {
                try {
                    val c = repo.getChat(chatId)
                    chat = c
                    if (!c.isGroup) {
                        c.members.firstOrNull { it != myUid }?.let { peer = repo.getUser(it) }
                    }
                } catch (_: Exception) {}
            }
        }
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size) // +stamp item
    }
    LaunchedEffect(error) {
        if (error != null) {
            snack.showSnackbar(error!!)
            vm.clearError()
        }
    }

    val subtitle = when {
        (chat?.isGroup == true) -> "${chat?.members?.size ?: 0} members"
        peer != null -> activeText(peer!!.online, peer!!.lastSeen)
        else -> ""
    }

    Scaffold(
        topBar = { IosHeader(title = chatName, subtitle = subtitle, onBack = onBack) },
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            IosInputBar(
                input = input,
                onInput = { input = it },
                sending = sending,
                showEmoji = showEmoji,
                onToggleEmoji = { showEmoji = !showEmoji },
                onEmoji = { input += it },
                onPickImage = { picker.launch("image/*") },
                onSend = {
                    vm.sendText(chatId, input)
                    input = ""
                    showEmoji = false
                }
            )
        },
        containerColor = ChatBackground
    ) { padding ->
        if (messages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No messages yet", color = ChatTextDim, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                item(key = "stamp") {
                    Text(
                        stampText(messages.first().timestamp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = ChatTextDim
                    )
                }
                itemsIndexed(messages, key = { _, m -> m.messageId }) { index, msg ->
                    val next = messages.getOrNull(index + 1)
                    val showAvatar = msg.senderId != myUid &&
                        (next == null || next.senderId != msg.senderId)
                    IosBubble(msg = msg, isMine = msg.senderId == myUid, showAvatar = showAvatar)
                }
            }
        }
    }
}

@Composable
fun IosHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(ChatHeaderBg)) {
        Spacer(Modifier.height(44.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ChatText
                )
            }
            Icon(
                Icons.Default.AccountCircle, contentDescription = null,
                modifier = Modifier.size(38.dp), tint = ChatTextDim
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(title.ifBlank { "Chat" }, fontSize = 16.sp, color = ChatText, maxLines = 1)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, fontSize = 12.sp, color = ChatTextDim, maxLines = 1)
                }
            }
        }
        HorizontalDivider(color = ChatDivider, thickness = 0.5.dp)
    }
}

@Composable
fun IosBubble(msg: Message, isMine: Boolean, showAvatar: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMine) {
            if (showAvatar) {
                Icon(
                    Icons.Default.AccountCircle, contentDescription = null,
                    modifier = Modifier.size(30.dp), tint = ChatTextDim
                )
            } else {
                Spacer(Modifier.width(30.dp))
            }
            Spacer(Modifier.width(6.dp))
        }
        Column(
            modifier = Modifier.widthIn(max = 270.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (isMine) BubbleMine else BubbleTheirs)
                .padding(horizontal = 13.dp, vertical = 8.dp)
        ) {
            if (!isMine && msg.senderName.isNotBlank()) {
                Text(msg.senderName, fontSize = 12.sp, color = AccentBlue)
            }
            if (msg.isImage) {
                AsyncImage(
                    model = msg.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.widthIn(max = 220.dp)
                        .height(170.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            if (msg.text.isNotBlank()) {
                Text(
                    msg.text + if (msg.pending) "  ⏳" else "",
                    fontSize = 16.sp,
                    color = if (isMine) BubbleTextMine else BubbleTextTheirs
                )
            }
        }
    }
}

@Composable
fun IosInputBar(
    input: String,
    onInput: (String) -> Unit,
    sending: Boolean,
    showEmoji: Boolean,
    onToggleEmoji: () -> Unit,
    onEmoji: (String) -> Unit,
    onPickImage: () -> Unit,
    onSend: () -> Unit
) {
    Column(Modifier.fillMaxWidth().imePadding().background(ChatBackground)) {
        HorizontalDivider(color = ChatDivider, thickness = 0.5.dp)
        if (showEmoji) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                modifier = Modifier.fillMaxWidth().height(240.dp).padding(4.dp)
            ) {
                items(EMOJIS) { emoji ->
                    Box(
                        modifier = Modifier.size(44.dp).clickable { onEmoji(emoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 24.sp)
                    }
                }
            }
            HorizontalDivider(color = ChatDivider, thickness = 0.5.dp)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(InputBg)
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f)) {
                    BasicTextField(
                        value = input,
                        onValueChange = onInput,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
                        textStyle = TextStyle(color = ChatText, fontSize = 16.sp),
                        cursorBrush = SolidColor(AccentBlue),
                        maxLines = 5
                    )
                    if (input.isEmpty()) {
                        Text("Message...", color = ChatTextDim, fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 9.dp))
                    }
                }
                IconButton(onClick = onToggleEmoji, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.TagFaces, contentDescription = "Emoji", tint = ChatTextDim)
                }
                IconButton(onClick = onPickImage, enabled = !sending, modifier = Modifier.size(34.dp)) {
                    if (sending) CircularProgressIndicator(Modifier.size(20.dp))
                    else Icon(Icons.Default.Image, contentDescription = "Photo", tint = ChatTextDim)
                }
                IconButton(onClick = onPickImage, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "Camera", tint = ChatTextDim)
                }
            }
            if (input.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Surface(shape = CircleShape, color = AccentBlue, modifier = Modifier.size(34.dp)) {
                    IconButton(onClick = onSend, modifier = Modifier.size(34.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
