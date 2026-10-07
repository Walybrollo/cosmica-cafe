package com.cosmica.cafeteria.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmica.cafeteria.R
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private val Espacio1 = Color(0xFF03040B)
private val Espacio2 = Color(0xFF12153A)

private class Estrella(val x: Float, val y: Float, val radio: Float, val brillo: Float, val fase: Float)

/** Fondo negro con estrellas que titilan, un cometa que cruza, planetas y una luna. */
@Composable
fun CieloEspacial(modifier: Modifier = Modifier, cantidadEstrellas: Int = 110) {
    val estrellas = remember(cantidadEstrellas) {
        val r = Random(42)
        List(cantidadEstrellas) {
            Estrella(r.nextFloat(), r.nextFloat(), 0.4f + r.nextFloat() * 1.3f, 0.4f + r.nextFloat() * 0.6f, r.nextFloat() * 2f * PI.toFloat())
        }
    }
    val tiempo = rememberInfiniteTransition(label = "cielo")
    val titileo by tiempo.animateFloat(
        0f, 2f * PI.toFloat(),
        infiniteRepeatable(tween(4_000, easing = LinearEasing), RepeatMode.Restart), label = "titileo",
    )
    val cometa by tiempo.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(11_000, easing = LinearEasing), RepeatMode.Restart), label = "cometa",
    )

    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(Espacio1, Espacio2)))
        // Nebulosa suave
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0x335B3FA8), Color.Transparent),
                center = Offset(size.width * 0.45f, size.height * 0.9f), radius = size.width * 0.45f,
            ),
            radius = size.width * 0.45f, center = Offset(size.width * 0.45f, size.height * 0.9f),
        )
        estrellas.forEach { e ->
            val alpha = (e.brillo * (0.65f + 0.35f * sin(titileo + e.fase))).coerceIn(0f, 1f)
            drawCircle(Color.White.copy(alpha = alpha), radius = e.radio.dp.toPx(), center = Offset(e.x * size.width, e.y * size.height))
        }
        dibujarCometa(cometa)
        dibujarPlaneta(
            centro = Offset(size.width * 0.80f, size.height * 0.42f), radio = 15.dp.toPx(),
            claro = Color(0xFFE8B07A), oscuro = Color(0xFF6B3A1E), anillo = Color(0xCCF3D9B1),
        )
        dibujarPlaneta(
            centro = Offset(size.width * 0.60f, size.height * 0.82f), radio = 5.dp.toPx(),
            claro = Color(0xFF8EC5FF), oscuro = Color(0xFF1D3F7A), anillo = null,
        )
        // Luna
        val luna = Offset(size.width * 0.94f, size.height * 0.85f)
        drawCircle(Color(0xFFDADDE6), radius = 7.dp.toPx(), center = luna)
        drawCircle(Color(0xFFB4B8C6), radius = 1.6.dp.toPx(), center = luna + Offset(-2.dp.toPx(), -1.5.dp.toPx()))
        drawCircle(Color(0xFFB4B8C6), radius = 1.dp.toPx(), center = luna + Offset(2.5.dp.toPx(), 2.dp.toPx()))
    }
}

private fun DrawScope.dibujarPlaneta(centro: Offset, radio: Float, claro: Color, oscuro: Color, anillo: Color?) {
    val anchoAnillo = radio * 2.1f
    val altoAnillo = radio * 0.55f
    if (anillo != null) {
        // Parte de atrás del anillo
        rotate(-18f, centro) {
            drawArc(
                anillo.copy(alpha = 0.5f), 180f, 180f, false,
                topLeft = Offset(centro.x - anchoAnillo, centro.y - altoAnillo),
                size = Size(anchoAnillo * 2, altoAnillo * 2), style = Stroke(2.dp.toPx()),
            )
        }
    }
    drawCircle(
        Brush.radialGradient(listOf(claro, oscuro), center = centro - Offset(radio * 0.4f, radio * 0.4f), radius = radio * 1.6f),
        radius = radio, center = centro,
    )
    if (anillo != null) {
        rotate(-18f, centro) {
            drawArc(
                anillo, 0f, 180f, false,
                topLeft = Offset(centro.x - anchoAnillo, centro.y - altoAnillo),
                size = Size(anchoAnillo * 2, altoAnillo * 2), style = Stroke(2.dp.toPx()),
            )
        }
    }
}

private fun DrawScope.dibujarCometa(progreso: Float) {
    // El cometa cruza durante la primera mitad del ciclo y después descansa.
    val p = progreso * 2f
    if (p > 1f) return
    val inicio = Offset(size.width * -0.05f, size.height * 0.15f)
    val fin = Offset(size.width * 0.55f, size.height * 0.75f)
    val cabeza = Offset(inicio.x + (fin.x - inicio.x) * p, inicio.y + (fin.y - inicio.y) * p)
    val largo = 70.dp.toPx()
    val dir = (fin - inicio).let { it / it.getDistance() }
    val cola = cabeza - dir * largo
    drawLine(
        Brush.linearGradient(listOf(Color.Transparent, Color(0xCCBFE3FF)), start = cola, end = cabeza),
        start = cola, end = cabeza, strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round,
    )
    drawCircle(Color.White, radius = 2.2.dp.toPx(), center = cabeza)
    drawCircle(Color(0x55BFE3FF), radius = 5.dp.toPx(), center = cabeza)
}

/** Encabezado de la app con el cielo de fondo, el logo, el nombre del local y el botón de salir. */
@Composable
fun EncabezadoEspacial(seccion: String, usuario: String, alSalir: () -> Unit) {
    var confirmarSalida by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        CieloEspacial(Modifier.matchParentSize())
        Row(
            Modifier.statusBarsPadding().fillMaxWidth().height(76.dp).padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = null,
                modifier = Modifier.height(60.dp),
            )
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(
                    "Cósmica Coffee",
                    color = Color.White,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                )
                Text(seccion, color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
            }
            IconButton(onClick = { confirmarSalida = true }) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión", tint = Color.White)
            }
        }
    }
    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Estás usando la app como $usuario.") },
            confirmButton = { TextButton(onClick = { confirmarSalida = false; alSalir() }) { Text("Cerrar sesión") } },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") } },
        )
    }
}
