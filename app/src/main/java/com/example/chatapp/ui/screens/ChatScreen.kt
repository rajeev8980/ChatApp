package com.example.chatapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.TagFaces
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.chatapp.data.SessionManager
import com.example.chatapp.data.model.Message
import com.example.chatapp.data.model.shortTime
import com.example.chatapp.ui.theme.WaBackground
import com.example.chatapp.ui.theme.WaBubbleMine
import com.example.chatapp.ui.theme.WaBubbleTheirs
import com.example.chatapp.ui.theme.WaGreen
import com.example.chatapp.ui.theme.WaHeader
import com.example.chatapp.ui.theme.WaText
import com.example.chatapp.ui.theme.WaTextDim
import com.example.chatapp.ui.viewmodel.ChatViewModel

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
    val myUid = try { SessionManager.get().uid } catch (_: Exception) { "" }
    val snack = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) vm.sendImage(chatId, uri)
    }

    LaunchedEffect(chatId) { vm.observe(chatId) }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }
    LaunchedEffect(error) {
        if (error != null) {
            snack.showSnackbar(error!!)
            vm.clearError()
        }
    }

    Scaffold(
        topBar = { WaChatHeader(chatName = chatName, onBack = onBack) },
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            WaInputBar(
                input = input,
                onInput = { input = it },
                sending = sending,
                onPickImage = { picker.launch("image/*") },
                onSend = {
                    vm.sendText(chatId, input)
                    input = ""
                }
            )
        },
        containerColor = WaBackground
    ) { padding ->
        if (messages.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No messages yet. Say hello!", color = WaTextDim)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item(key = "date-chip") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = WaHeader
                        ) {
                            Text(
                                "Today",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                color = WaTextDim
                            )
                        }
                    }
                }
                items(messages, key = { it.messageId }) { msg ->
                    WaBubble(msg = msg, isMine = msg.senderId == myUid)
                }
            }
        }
    }
}

@Composable
fun WaChatHeader(chatName: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(WaHeader)
            .padding(top = 40.dp, bottom = 8.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WaText)
        }
        Icon(
            Icons.Default.AccountCircle, contentDescription = null,
            modifier = Modifier.size(40.dp), tint = WaTextDim
        )
        Spacer(Modifier.width(8.dp))
        Text(
            chatName.ifBlank { "Chat" },
            color = WaText,
            fontSize = 17.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        IconButton(onClick = {}) {
            Icon(Icons.Default.Videocam, contentDescription = "Video", tint = WaText)
        }
        IconButton(onClick = {}) {
            Icon(Icons.Default.Call, contentDescription = "Call", tint = WaText)
        }
        IconButton(onClick = {}) {
            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = WaText)
        }
    }
}

@Composable
fun WaBubble(msg: Message, isMine: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 300.dp)
                .clip(
                    if (isMine) RoundedCornerShape(12.dp, 4.dp, 12.dp, 12.dp)
                    else RoundedCornerShape(4.dp, 12.dp, 12.dp, 12.dp)
                )
                .background(if (isMine) WaBubbleMine else WaBubbleTheirs)
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            if (!isMine && msg.senderName.isNotBlank()) {
                Text(msg.senderName, fontSize = 13.sp, color = WaGreen)
            }
            if (msg.isImage) {
                AsyncImage(
                    model = msg.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(220.dp, 180.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            if (msg.text.isNotBlank()) {
                Text(msg.text, fontSize = 16.sp, color = WaText)
            }
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (msg.timestamp.isNotBlank()) {
                    Text(shortTime(msg.timestamp), fontSize = 11.sp, color = WaTextDim)
                }
                if (isMine) {
                    Text("  ✓", fontSize = 11.sp, color = WaTextDim)
                }
            }
        }
    }
}

@Composable
fun WaInputBar(
    input: String,
    onInput: (String) -> Unit,
    sending: Boolean,
    onPickImage: () -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().imePadding()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Row(
            modifier = Modifier.weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(WaHeader)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {}) {
                Icon(Icons.Default.TagFaces, contentDescription = "Emoji", tint = WaTextDim)
            }
            Box(Modifier.weight(1f)) {
                BasicTextField(
                    value = input,
                    onValueChange = onInput,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    textStyle = TextStyle(color = WaText, fontSize = 17.sp),
                    cursorBrush = SolidColor(WaGreen),
                    maxLines = 5
                )
                if (input.isEmpty()) {
                    Text("Message", color = WaTextDim, fontSize = 17.sp,
                        modifier = Modifier.padding(vertical = 10.dp))
                }
            }
            IconButton(onClick = onPickImage, enabled = !sending) {
                if (sending) CircularProgressIndicator(Modifier.size(22.dp))
                else Icon(Icons.Default.Add, contentDescription = "Attach", tint = WaTextDim)
            }
            IconButton(onClick = onPickImage) {
                Icon(Icons.Default.PhotoCamera, contentDescription = "Camera", tint = WaTextDim)
            }
        }
        Spacer(Modifier.width(6.dp))
        Surface(
            shape = CircleShape,
            color = WaGreen,
            modifier = Modifier.size(50.dp)
        ) {
            IconButton(
                onClick = { if (input.isNotBlank()) onSend() },
                enabled = input.isNotBlank()
            ) {
                Icon(
                    if (input.isNotBlank()) Icons.AutoMirrored.Filled.Send
                    else Icons.Default.KeyboardVoice,
                    contentDescription = "Send",
                    tint = androidx.compose.ui.graphics.Color.White
                )
            }
        }
    }
}
