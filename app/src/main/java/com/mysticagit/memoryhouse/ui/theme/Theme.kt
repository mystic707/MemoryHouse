package com.mysticagit.memoryhouse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 사진이 주인공인 화면이라 배경은 어둡게 고정한다.
val AlbumBackground = Color(0xFF101013)
val AlbumSurface = Color(0xFF1B1B20)
val AlbumAccent = Color(0xFFE8C39E)

private val MemoryHouseColors = darkColorScheme(
    primary = AlbumAccent,
    onPrimary = Color(0xFF2A1F14),
    background = AlbumBackground,
    onBackground = Color(0xFFF2F2F5),
    surface = AlbumSurface,
    onSurface = Color(0xFFF2F2F5),
    surfaceVariant = Color(0xFF2A2A31),
    onSurfaceVariant = Color(0xFFC3C3CC),
    error = Color(0xFFE28D8D),
)

@Composable
fun MemoryHouseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MemoryHouseColors, content = content)
}
