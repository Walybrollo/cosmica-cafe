package com.cosmica.cafeteria.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import com.cosmica.cafeteria.ui.theme.Ambar
import java.text.Normalizer

/**
 * Dibujo de línea de un producto, elegido por su nombre.
 * [fondo] tiene que ser el color de la tarjeta donde va, para que las partes de adelante tapen a las de atrás.
 */
@Composable
fun DibujoProducto(nombre: String, fondo: Color, modifier: Modifier = Modifier, color: Color = Ambar) {
    val trazos = remember(nombre) { trazosDe(claveDibujo(nombre)) }
    Canvas(modifier) {
        val s = size.minDimension / 48f
        translate((size.width - 48f * s) / 2f, (size.height - 48f * s) / 2f) {
            scale(s, s, pivot = Offset.Zero) {
                for ((camino, t) in trazos) {
                    when (t.relleno) {
                        1 -> drawPath(camino, fondo)
                        2 -> drawPath(camino, color)
                    }
                    drawPath(camino, color, style = Stroke(width = t.ancho, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
    }
}

private val cache = HashMap<String, List<Pair<Path, T>>>()

private fun trazosDe(clave: String): List<Pair<Path, T>> = synchronized(cache) {
    cache.getOrPut(clave) {
        (TRAZOS_DIBUJOS[clave] ?: TRAZOS_DIBUJOS.getValue("generico"))
            .map { PathParser().parsePathString(it.d).toPath() to it }
    }
}

private fun String.simple(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase().trim()

/** Qué dibujo le toca a un producto según su nombre. Los productos nuevos sin dibujo propio llevan uno genérico. */
internal fun claveDibujo(nombre: String): String {
    val n = nombre.simple()
    val palabras = n.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
    fun tiene(vararg x: String) = x.any { n.contains(it) }
    val grande = tiene("grande")
    return when {
        tiene("licuado", "batido") && tiene("sandw", "sanguch") -> "licuado-sandwich"
        tiene("capuch", "cappuc") -> if (grande) "capuchino-grande" else "capuchino-chico"
        "te" in palabras || "tea" in palabras -> if (grande) "te-grande" else "te"
        tiene("frap") -> "frappes"
        tiene("ice coffee", "iced", "cafe helado") -> "ice-coffee"
        tiene("amarula") -> "amarula"
        tiene("especial") -> "especiales"
        tiene("jarr") -> "cafe-jarro"
        tiene("cafe", "cortado", "lagrima") -> if (grande) "cafe-grande" else "cafe-chico"
        tiene("budin") -> "budin-de-pan"
        tiene("churro") -> "churros"
        tiene("cookie", "galleti") -> "cookies"
        tiene("medialuna") -> "medialunas"
        tiene("factura") -> "facturas"
        tiene("pebete") -> "pebete-chico"
        tiene("rasta") -> "rastas"
        tiene("sandw", "sanguch", "tostado") -> if (grande) "sandwich-grande" else "sandwich-chico"
        tiene("licuado", "batido", "smoothie") -> "licuados"
        tiene("raspado") -> "raspado"
        tiene("cerveza", "birra") -> "cerveza"
        else -> "generico"
    }
}
