package com.cosmica.cafeteria.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cosmica.cafeteria.CafeViewModel
import com.cosmica.cafeteria.data.Venta
import com.cosmica.cafeteria.ui.Formato
import com.cosmica.cafeteria.ui.SelectorMes
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun VentasScreen(vm: CafeViewModel, modifier: Modifier) {
    val ventas by vm.ventas.collectAsState()
    var aBorrar by remember { mutableStateOf<Venta?>(null) }

    val inicioHoy = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val hoy = ventas.filter { it.fecha >= inicioHoy }

    Column(modifier.fillMaxSize()) {
        SelectorMes(vm)
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Dato("Hoy", Formato.dinero(hoy.sumOf { it.total }), "${hoy.size} ventas", Modifier.weight(1f))
            Dato("En el mes", Formato.dinero(ventas.sumOf { it.total }), "${ventas.size} ventas", Modifier.weight(1f))
        }
        if (ventas.isEmpty()) {
            Text(
                "No hay ventas en este mes.",
                Modifier.fillMaxWidth().padding(32.dp),
                textAlign = TextAlign.Center,
            )
        }
        LazyColumn(contentPadding = PaddingValues(12.dp)) {
            items(ventas, key = { it.id }) { v ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                listOf(Formato.fechaHora(v.fecha), v.metodoPago, v.vendedor).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Text(v.items.joinToString { "${it.cantidad}× ${it.nombreProducto}" })
                        }
                        Text(Formato.dinero(v.total), fontWeight = FontWeight.Bold)
                        IconButton(onClick = { aBorrar = v }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Borrar venta")
                        }
                    }
                }
            }
        }
    }

    aBorrar?.let { v ->
        AlertDialog(
            onDismissRequest = { aBorrar = null },
            title = { Text("¿Borrar esta venta?") },
            text = { Text("${Formato.fechaHora(v.fecha)} por ${Formato.dinero(v.total)}. Se descuenta del balance.") },
            confirmButton = {
                TextButton(onClick = { vm.borrarVenta(v); aBorrar = null }) { Text("Borrar") }
            },
            dismissButton = { TextButton(onClick = { aBorrar = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
fun Dato(titulo: String, valor: String, detalle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 6.dp)) {
        Text(titulo, style = MaterialTheme.typography.labelMedium)
        Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (detalle != null) Text(detalle, style = MaterialTheme.typography.bodySmall)
    }
}
