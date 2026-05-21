package org.example.project.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val AppLightColors = lightColorScheme(
    primary = Color(0xFF5B4BC4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5E0FF),
    onPrimaryContainer = Color(0xFF150067),
    secondary = Color(0xFF5D5C72),
    tertiary = Color(0xFF7D5260),
    background = Color(0xFFFDFBFF),
    surface = Color(0xFFFDFBFF),
    error = Color(0xFFBA1A1A)
)

val AppDarkColors = darkColorScheme(
    primary = Color(0xFFC6BFFF),
    onPrimary = Color(0xFF270089),
    primaryContainer = Color(0xFF4334AB),
    onPrimaryContainer = Color(0xFFE5E0FF),
    secondary = Color(0xFFC6C4DD),
    tertiary = Color(0xFFEFB8C8),
    background = Color(0xFF1B1B1F),
    surface = Color(0xFF1B1B1F),
    error = Color(0xFFFFB4AB)
)
