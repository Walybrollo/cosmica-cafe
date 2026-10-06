package com.cosmica.cafeteria.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ganancia = Color(0xFF2E7D32)
val Perdida = Color(0xFFC62828)

private val Claro = lightColorScheme(
    primary = Color(0xFF6D4C41),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC8),
    onPrimaryContainer = Color(0xFF2B160D),
    secondary = Color(0xFFB27B00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE08F),
    onSecondaryContainer = Color(0xFF261A00),
    background = Color(0xFFFFF8F3),
    surface = Color(0xFFFFF8F3),
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFFFFB68F),
    onPrimary = Color(0xFF4A2512),
    primaryContainer = Color(0xFF5D3A26),
    onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = Color(0xFFF5BF48),
    onSecondary = Color(0xFF402D00),
    secondaryContainer = Color(0xFF5C4200),
    onSecondaryContainer = Color(0xFFFFDF9E),
)

@Composable
fun CafeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Oscuro else Claro,
        content = content,
    )
}
