package com.cosmica.cafeteria.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.cosmica.cafeteria.ui.Excel
import com.cosmica.cafeteria.ui.theme.Ambar
import com.cosmica.cafeteria.ui.theme.TextoSuave
import java.time.LocalDate
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cosmica.cafeteria.CafeViewModel
import com.cosmica.cafeteria.ui.Formato
import com.cosmica.cafeteria.ui.SelectorMes
import com.cosmica.cafeteria.ui.theme.Ganancia
import com.cosmica.cafeteria.ui.theme.Perdida

@Composable
fun BalanceScreen(vm: CafeViewModel, modifier: Modifier) {
    val b by vm.balance.collectAsState()
    val antes by vm.balanceAnterior.collectAsState()
    val parcial by vm.comparaPeriodoParcial.collectAsState()
    val mes by vm.mes.collectAsState()
    val ventas by vm.ventas.collectAsState()
    val gastos by vm.gastos.collectAsState()
    val cierres by vm.cierres.collectAsState()
    val context = LocalContext.current

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SelectorMes(vm)

        OutlinedButton(
            onClick = {
                Excel.compartir(
                    context,
                    "Cosmica-Coffee-$mes.xlsx",
                    Excel.hojasDelMes(mes, b, ventas, gastos, cierres),
                )
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        ) {
            Icon(Icons.Filled.Share, contentDescription = null)
            Text("  Exportar el mes a Excel")
        }

        Seccion("Resultado del mes") {
            Linea("Ventas (${b.cantidadVentas})", b.ingresos)
            Linea("Costo de lo vendido", -b.costoMercaderia)
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Linea("Ganancia bruta", b.gananciaBruta, destacada = true)
            Linea("Gastos", -b.gastos)
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            val neta = b.gananciaNeta
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text("Ganancia neta", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    Formato.dinero(neta),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (neta >= 0) Ganancia else Perdida,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text("Ticket promedio: ${Formato.dinero(b.ticketPromedio)}", style = MaterialTheme.typography.bodySmall)
            if (b.descuentos > 0) {
                Text("Descuentos dados: ${Formato.dinero(b.descuentos)}", style = MaterialTheme.typography.bodySmall)
            }
        }

        if (antes.cantidadVentas > 0) {
            val nombreAnterior = Formato.mes(mes.minusMonths(1)).substringBefore(" ").lowercase()
            Seccion(if (parcial) "Comparado con el mismo período de $nombreAnterior" else "Comparado con $nombreAnterior") {
                Comparacion("Ventas", b.ingresos, antes.ingresos)
                Comparacion("Cantidad de ventas", b.cantidadVentas.toDouble(), antes.cantidadVentas.toDouble(), esPlata = false)
                Comparacion("Ticket promedio", b.ticketPromedio, antes.ticketPromedio)
                Comparacion("Gastos", b.gastos, antes.gastos, subirEsBueno = false)
                Comparacion("Ganancia neta", b.gananciaNeta, antes.gananciaNeta)
            }
        }

        if (b.porDia.isNotEmpty()) {
            val mejor = b.porDia.maxBy { it.value }
            Seccion("Ventas por día") {
                Barras((1..mes.lengthOfMonth()).map { b.porDia[it] ?: 0.0 }, etiquetaCada = 5, primera = 1)
                Text(
                    "Mejor día: ${Formato.dia(mes.atDay(mejor.key))} con ${Formato.dinero(mejor.value)}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        if (b.porHora.isNotEmpty()) {
            val desde = b.porHora.keys.min()
            val hasta = b.porHora.keys.max()
            val pico = b.porHora.maxBy { it.value }
            Seccion("Horarios con más movimiento") {
                Barras((desde..hasta).map { (b.porHora[it] ?: 0).toDouble() }, etiquetaCada = 2, primera = desde, sufijo = "h")
                Text(
                    "Más ventas entre las ${pico.key} y las ${pico.key + 1} h (${pico.value} ventas en el mes)",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        if (cierres.isNotEmpty()) {
            Seccion("Cierres de caja") {
                cierres.forEach { c ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(Formato.dia(LocalDate.parse(c.dia)), Modifier.weight(1f))
                        Text(textoDiferencia(c.diferencia), color = colorDiferencia(c.diferencia))
                    }
                }
                val total = cierres.sumOf { it.diferencia }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text("En el mes", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text(textoDiferencia(total), color = colorDiferencia(total), fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (b.porMetodoPago.isNotEmpty()) {
            Seccion("Cobrado por medio de pago") {
                b.porMetodoPago.entries.sortedByDescending { it.value }.forEach { (m, v) ->
                    Barra(m, v, b.ingresos, MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (b.porVendedor.size > 1) {
            Seccion("Vendido por persona") {
                b.porVendedor.entries.sortedByDescending { it.value }.forEach { (n, v) ->
                    Barra(n, v, b.ingresos, MaterialTheme.colorScheme.tertiary)
                }
            }
        }

        if (b.gastosPorCategoria.isNotEmpty()) {
            Seccion("Gastos por categoría") {
                b.gastosPorCategoria.entries.sortedByDescending { it.value }.forEach { (c, v) ->
                    Barra(c, v, b.gastos, MaterialTheme.colorScheme.secondary)
                }
            }
        }

        if (b.productos.isNotEmpty()) {
            Seccion("Productos más vendidos") {
                b.productos.take(10).forEach { p ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("${p.cantidad}× ${p.nombre}")
                            Text(
                                "Ganancia ${Formato.dinero(p.ganancia)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Ganancia,
                            )
                        }
                        Text(Formato.dinero(p.ingresos))
                    }
                }
            }
        }

        if (b.cantidadVentas == 0 && b.gastos == 0.0) {
            Text(
                "Todavía no hay movimientos en este mes.",
                Modifier.padding(16.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                titulo,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            contenido()
        }
    }
}

@Composable
private fun Linea(texto: String, valor: Double, destacada: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(texto, Modifier.weight(1f), fontWeight = if (destacada) FontWeight.SemiBold else null)
        Text(Formato.dinero(valor), fontWeight = if (destacada) FontWeight.SemiBold else null)
    }
}

@Composable
private fun Barra(texto: String, valor: Double, total: Double, color: Color) {
    val fraccion = if (total > 0) (valor / total).toFloat().coerceIn(0f, 1f) else 0f
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(texto, Modifier.weight(1f))
            Text("${Formato.dinero(valor)} · ${Formato.porcentaje(fraccion.toDouble())}")
        }
        Box(
            Modifier.fillMaxWidth().height(8.dp)
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
        ) {
            Box(
                Modifier.fillMaxWidth(fraccion).height(8.dp)
                    .background(color, RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun Comparacion(texto: String, ahora: Double, antes: Double, esPlata: Boolean = true, subirEsBueno: Boolean = true) {
    val cambio = if (antes != 0.0) (ahora - antes) / kotlin.math.abs(antes) else null
    val bueno = cambio != null && (cambio >= 0) == subirEsBueno
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(texto)
            Text(
                "Antes: " + if (esPlata) Formato.dinero(antes) else antes.toInt().toString(),
                style = MaterialTheme.typography.bodySmall,
                color = TextoSuave,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(if (esPlata) Formato.dinero(ahora) else ahora.toInt().toString(), fontWeight = FontWeight.SemiBold)
            if (cambio != null) {
                Text(
                    (if (cambio >= 0) "▲ " else "▼ ") + Formato.porcentaje(kotlin.math.abs(cambio)),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (bueno) Ganancia else Perdida,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** Gráfico de barras simple; marca la barra más alta en ámbar. */
@Composable
private fun Barras(valores: List<Double>, etiquetaCada: Int, primera: Int, sufijo: String = "") {
    val max = valores.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    Row(
        Modifier.fillMaxWidth().height(110.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        valores.forEach { v ->
            val f = (v / max).toFloat()
            Box(
                Modifier.weight(1f).fillMaxHeight(f.coerceAtLeast(0.02f))
                    .background(
                        if (v == max) Ambar else Ambar.copy(alpha = 0.35f),
                        RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
                    )
            )
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        valores.indices.forEach { i ->
            val n = primera + i
            Text(
                if ((n - primera) % etiquetaCada == 0) "$n$sufijo" else "",
                Modifier.weight(1f).wrapContentWidth(Alignment.Start, unbounded = true),
                style = MaterialTheme.typography.labelSmall,
                color = TextoSuave,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}
