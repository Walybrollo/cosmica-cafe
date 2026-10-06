package com.cosmica.cafeteria.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
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

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SelectorMes(vm)

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
