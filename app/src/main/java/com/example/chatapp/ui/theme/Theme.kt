package com.example.chatapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// iMessage-style light palette
val ChatBackground = Color(0xFFFFFFFF)
val ChatHeaderBg = Color(0xFFF6F6F6)
val ChatDivider = Color(0xFFE5E5EA)
val BubbleMine = Color(0xFF000000)
val BubbleTheirs = Color(0xFFE9E9EB)
val BubbleTextMine = Color(0xFFFFFFFF)
val BubbleTextTheirs = Color(0xFF000000)
val AccentBlue = Color(0xFF007AFF)
val ChatText = Color(0xFF000000)
val ChatTextDim = Color(0xFF8E8E93)
val InputBg = Color(0xFFF0F0F2)

private val Scheme = lightColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    secondary = AccentBlue,
    background = ChatBackground,
    onBackground = ChatText,
    surface = ChatBackground,
    onSurface = ChatText,
    surfaceVariant = ChatHeaderBg,
    onSurfaceVariant = ChatTextDim,
    primaryContainer = BubbleMine,
    onPrimaryContainer = BubbleTextMine
)

@Composable
fun ChatAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
