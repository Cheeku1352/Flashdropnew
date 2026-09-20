package com.flashdrop.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FlashDropDark = darkColorScheme(
    primary = Accent,
    onPrimary = AccentFg,
    secondary = Volt,
    background = InkBg,
    surface = Surface,
    onBackground = Fg,
    onSurface = Fg,
    outline = Border
)

@Composable
fun FlashDropTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FlashDropDark,
        content = content
    )
}