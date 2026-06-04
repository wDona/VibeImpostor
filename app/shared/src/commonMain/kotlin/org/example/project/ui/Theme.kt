package org.example.project.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val playerBadgeColors = listOf(
    Color(0xFF8B5CF6),
    Color(0xFFEC4899),
    Color(0xFF3B82F6),
    Color(0xFF10B981),
    Color(0xFFF59E0B),
    Color(0xFFEF4444),
    Color(0xFF06B6D4),
    Color(0xFFF97316),
)

fun playerColor(colorIndex: Int): Color {
    return playerBadgeColors[colorIndex % playerBadgeColors.size]
}

fun playerColorIndex(playerId: String, players: List<org.example.project.model.PublicPlayer>): Int {
    return players.find { it.id == playerId }?.colorIndex ?: 0
}

val ImpostorRed = Color(0xFFEF4444)
val InnocentBlue = Color(0xFF3B82F6)
val WinnerGold = Color(0xFFF59E0B)
val WinnerGreen = Color(0xFF22C55E)
val GamePurple = Color(0xFF8B5CF6)
val GamePurpleLight = Color(0xFFAB8BFF)

val AppLightColors = lightColorScheme(
    primary = Color(0xFF5B21B6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = Color(0xFFBE185D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFCE7F3),
    onSecondaryContainer = Color(0xFF500724),
    tertiary = Color(0xFF0369A1),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE0F2FE),
    background = Color(0xFFF5F3FF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDE9FE),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF7C757F),
    error = Color(0xFFB91C1C)
)

val AppDarkColors = darkColorScheme(
    primary = GamePurpleLight,
    onPrimary = Color(0xFF2E1065),
    primaryContainer = Color(0xFF3B0C8A),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = Color(0xFFF472B6),
    onSecondary = Color(0xFF500724),
    secondaryContainer = Color(0xFF831843),
    onSecondaryContainer = Color(0xFFFCE7F3),
    tertiary = Color(0xFF38BDF8),
    onTertiary = Color(0xFF082F49),
    tertiaryContainer = Color(0xFF0C4A6E),
    background = Color(0xFF080815),
    surface = Color(0xFF0F0F1E),
    surfaceVariant = Color(0xFF1A1A2E),
    onSurface = Color(0xFFECE8F5),
    onSurfaceVariant = Color(0xFFCAC4D8),
    outline = Color(0xFF7A7589),
    error = Color(0xFFF87171)
)

fun roleColor(role: org.example.project.model.Role?): Color = when (role) {
    org.example.project.model.Role.IMPOSTOR -> ImpostorRed
    org.example.project.model.Role.INNOCENT -> InnocentBlue
    null -> Color(0xFF9CA3AF)
}
