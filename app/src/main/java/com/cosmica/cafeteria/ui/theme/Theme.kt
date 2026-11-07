package com.cosmica.cafeteria.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.cosmica.cafeteria.R

// Estilo "Órbita": noche espacial con acento ámbar.
val Fondo = Color(0xFF070A16)
val Tarjeta = Color(0xFF121733)
val Tarjeta2 = Color(0xFF1B2246)
val Borde = Color(0xFF232A52)
val Texto = Color(0xFFEEF0FA)
val TextoSuave = Color(0xFF9AA3C7)
val Ambar = Color(0xFFF2B35B)
val SobreAmbar = Color(0xFF1A1204)
val BarraInferior = Color(0xFF0B0F24)
/** Color del consumo del equipo. */
val Celeste = Color(0xFF8EC5FF)
val SobreCeleste = Color(0xFF071230)

val Ganancia = Color(0xFF6FD08C)
val Perdida = Color(0xFFFF7A7A)

/** Letra del nombre del local. */
val Marca = FontFamily(Font(R.font.cinzel_bold, FontWeight.Bold))

/** Letra de toda la app. */
val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_regular, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_extrabold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

private val Orbita = darkColorScheme(
    primary = Ambar,
    onPrimary = SobreAmbar,
    primaryContainer = Color(0xFF3A2E1C),
    onPrimaryContainer = Color(0xFFFFE2B8),
    secondary = Color(0xFFAFB8E6),
    onSecondary = Fondo,
    secondaryContainer = Tarjeta2,
    onSecondaryContainer = Texto,
    tertiary = Color(0xFF8EC5FF),
    background = Fondo,
    onBackground = Texto,
    surface = Fondo,
    onSurface = Texto,
    surfaceVariant = Tarjeta,
    onSurfaceVariant = TextoSuave,
    surfaceContainerLowest = Fondo,
    surfaceContainerLow = Tarjeta,
    surfaceContainer = Tarjeta,
    surfaceContainerHigh = Tarjeta2,
    surfaceContainerHighest = Tarjeta2,
    surfaceBright = Tarjeta2,
    surfaceDim = Fondo,
    outline = Color(0xFF3A4480),
    outlineVariant = Borde,
    error = Perdida,
    onError = Fondo,
    errorContainer = Color(0xFF4A1820),
    onErrorContainer = Color(0xFFFFDADA),
)

private fun Typography.conLetra(f: FontFamily): Typography {
    fun TextStyle.c() = copy(fontFamily = f)
    return copy(
        displayLarge = displayLarge.c(), displayMedium = displayMedium.c(), displaySmall = displaySmall.c(),
        headlineLarge = headlineLarge.c(), headlineMedium = headlineMedium.c(), headlineSmall = headlineSmall.c(),
        titleLarge = titleLarge.c(), titleMedium = titleMedium.c(), titleSmall = titleSmall.c(),
        bodyLarge = bodyLarge.c(), bodyMedium = bodyMedium.c(), bodySmall = bodySmall.c(),
        labelLarge = labelLarge.c(), labelMedium = labelMedium.c(), labelSmall = labelSmall.c(),
    )
}

@Composable
fun CafeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Orbita,
        typography = Typography().conLetra(Manrope),
        content = content,
    )
}
