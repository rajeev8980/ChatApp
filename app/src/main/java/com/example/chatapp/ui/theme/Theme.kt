package com.example.chatapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// WhatsApp dark palette
val WaBackground = Color(0xFF0B141A)
val WaSurface = Color(0xFF111B21)
val WaHeader = Color(0xFF1F2C34)
val WaBubbleMine = Color(0xFF005C4B)
val WaBubbleTheirs = Color(0xFF1F2C34)
val WaGreen = Color(0xFF00A884)
val WaText = Color(0xFFE9EDEF)
val WaTextDim = Color(0xFF8696A0)
val WaDivider = Color(0xFF222D34)

private val Scheme = darkColorScheme(
    primary = WaGreen,
    onPrimary = Color.White,
    secondary = WaGreen,
    background = WaBackground,
    onBackground = WaText,
    surface = WaSurface,
    onSurface = WaText,
    surfaceVariant = WaHeader,
    onSurfaceVariant = WaTextDim,
    primaryContainer = WaBubbleMine,
    onPrimaryContainer = WaText
)

@Composable
fun ChatAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
