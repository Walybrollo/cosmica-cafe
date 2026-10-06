package com.cosmica.cafeteria.ui

import java.text.NumberFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formato {
    private val es = Locale.forLanguageTag("es")

    private val moneda: NumberFormat by lazy {
        val local = Locale.getDefault()
        val nf = if (local.country.isNotEmpty()) NumberFormat.getCurrencyInstance(local)
        else NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR"))
        nf.apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
    }

    fun dinero(valor: Double): String = moneda.format(valor)

    fun porcentaje(valor: Double): String = "${(valor * 100).toInt()}%"

    /** Acepta "1500", "1500.5", "1500,5" y "1.500,50". */
    fun leerNumero(texto: String): Double? {
        val t = texto.trim().replace(" ", "").replace("$", "")
        val normal = when {
            t.contains(',') && t.contains('.') -> t.replace(".", "").replace(',', '.')
            t.contains(',') -> t.replace(',', '.')
            else -> t
        }
        return normal.toDoubleOrNull()?.takeIf { it >= 0 }
    }

    /** Muestra un número para editarlo, sin ".0" al final. */
    fun paraEditar(valor: Double): String =
        if (valor == Math.floor(valor)) valor.toLong().toString() else valor.toString()

    private val fechaHora = DateTimeFormatter.ofPattern("EEE d/MM HH:mm", es)
    private val fecha = DateTimeFormatter.ofPattern("d/MM/yyyy", es)
    private val nombreMes = DateTimeFormatter.ofPattern("MMMM yyyy", es)

    fun fechaHora(ms: Long): String =
        Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(fechaHora)

    fun fecha(ms: Long): String =
        Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(fecha)

    fun mes(m: YearMonth): String = m.format(nombreMes).replaceFirstChar { it.uppercase() }
}
