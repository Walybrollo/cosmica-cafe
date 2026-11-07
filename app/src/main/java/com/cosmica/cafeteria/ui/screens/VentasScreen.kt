package com.cosmica.cafeteria.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cosmica.cafeteria.CafeViewModel
import com.cosmica.cafeteria.data.Categorias
import com.cosmica.cafeteria.data.Cierre
import com.cosmica.cafeteria.data.Producto
import com.cosmica.cafeteria.data.Venta
import com.cosmica.cafeteria.data.VentaItem
import com.cosmica.cafeteria.data.subtotal
import com.cosmica.cafeteria.ui.CampoNumero
import com.cosmica.cafeteria.ui.Formato
import com.cosmica.cafeteria.ui.Opciones
import com.cosmica.cafeteria.ui.SelectorMes
import com.cosmica.cafeteria.ui.theme.Ambar
import com.cosmica.cafeteria.ui.theme.Ganancia
import com.cosmica.cafeteria.ui.theme.Perdida
import com.cosmica.cafeteria.ui.theme.TextoSuave
import java.text.Normalizer
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun VentasScreen(vm: CafeViewModel, modifier: Modifier) {
    val ventas by vm.ventas.collectAsState()
    val cierreHoy by vm.cierreHoy.collectAsState()
    val menu by vm.menu.collectAsState()
    var editando by remember { mutableStateOf<Venta?>(null) }
    var cerrandoCaja by remember { mutableStateOf(false) }

    val inicioHoy = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val hoy = ventas.filter { it.fecha >= inicioHoy }

    Column(modifier.fillMaxSize()) {
        SelectorMes(vm)
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Dato("Hoy", Formato.dinero(hoy.sumOf { it.total }), "${hoy.size} ventas", Modifier.weight(1f))
            Dato("En el mes", Formato.dinero(ventas.sumOf { it.total }), "${ventas.size} ventas", Modifier.weight(1f))
        }
        TarjetaCierre(cierreHoy) { cerrandoCaja = true }
        if (ventas.isEmpty()) {
            Text(
                "No hay ventas en este mes.",
                Modifier.fillMaxWidth().padding(32.dp),
                textAlign = TextAlign.Center,
            )
        }
        LazyColumn(contentPadding = PaddingValues(12.dp)) {
            items(ventas, key = { it.id }) { v ->
                Card(onClick = { editando = v }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                listOf(Formato.fechaHora(v.fecha), v.metodoPago, v.vendedor).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextoSuave,
                            )
                            Text(v.items.joinToString { "${it.cantidad}× ${it.nombreProducto}" })
                            if (v.descuento > 0) {
                                Text("Descuento ${Formato.dinero(v.descuento)}", style = MaterialTheme.typography.bodySmall, color = Ambar)
                            }
                        }
                        Text(Formato.dinero(v.total), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    editando?.let { v ->
        DialogoEditarVenta(
            venta = v,
            menu = menu,
            alCerrar = { editando = null },
            alGuardar = { vm.editarVenta(v, it); editando = null },
            alBorrar = { vm.borrarVenta(v); editando = null },
        )
    }

    if (cerrandoCaja) {
        DialogoCierre(vm, cierreHoy) { cerrandoCaja = false }
    }
}

@Composable
private fun TarjetaCierre(cierre: Cierre?, alTocar: () -> Unit) {
    Card(
        onClick = alTocar,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = Ambar)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                if (cierre == null) {
                    Text("Cerrar la caja de hoy", fontWeight = FontWeight.SemiBold)
                    Text("Contá el efectivo y anotá si sobra o falta", style = MaterialTheme.typography.bodySmall, color = TextoSuave)
                } else {
                    Text("Caja de hoy cerrada", fontWeight = FontWeight.SemiBold)
                    Text(textoDiferencia(cierre.diferencia), style = MaterialTheme.typography.bodySmall, color = colorDiferencia(cierre.diferencia))
                }
            }
            Text(if (cierre == null) "Cerrar" else "Rehacer", color = Ambar, fontWeight = FontWeight.Bold)
        }
    }
}

fun textoDiferencia(d: Double): String = when {
    d == 0.0 -> "La caja da justo"
    d > 0 -> "Sobran ${Formato.dinero(d)}"
    else -> "Faltan ${Formato.dinero(-d)}"
}

fun colorDiferencia(d: Double) = when {
    d == 0.0 -> Ganancia
    d > 0 -> Ambar
    else -> Perdida
}

@Composable
private fun DialogoCierre(vm: CafeViewModel, anterior: Cierre?, alCerrar: () -> Unit) {
    val hoy by vm.ventasHoy.collectAsState()
    var fondo by remember { mutableStateOf(anterior?.let { Formato.paraEditar(it.fondoInicial) } ?: "") }
    var contado by remember { mutableStateOf(anterior?.let { Formato.paraEditar(it.contado) } ?: "") }
    var nota by remember { mutableStateOf(anterior?.nota ?: "") }
    val porMedio = hoy.groupBy { it.metodoPago }.mapValues { (_, l) -> l.sumOf { it.total } }
    val efectivo = porMedio[Categorias.metodosPago.first()] ?: 0.0
    val fondoNum = if (fondo.isBlank()) 0.0 else Formato.leerNumero(fondo)
    val contadoNum = Formato.leerNumero(contado)
    val esperado = (fondoNum ?: 0.0) + efectivo

    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Cierre de caja de hoy") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${hoy.size} ventas por ${Formato.dinero(hoy.sumOf { it.total })}", fontWeight = FontWeight.SemiBold)
                Categorias.metodosPago.forEach { m ->
                    Row { Text(m, Modifier.weight(1f), color = TextoSuave); Text(Formato.dinero(porMedio[m] ?: 0.0)) }
                }
                HorizontalDivider()
                CampoNumero(fondo, "Efectivo con el que abrió la caja") { fondo = it }
                Row {
                    Text("Debería haber en efectivo", Modifier.weight(1f))
                    Text(Formato.dinero(esperado), fontWeight = FontWeight.Bold)
                }
                CampoNumero(contado, "Efectivo contado") { contado = it }
                contadoNum?.let {
                    val dif = it - esperado
                    Text(textoDiferencia(dif), color = colorDiferencia(dif), fontWeight = FontWeight.Bold)
                }
                OutlinedTextField(
                    value = nota,
                    onValueChange = { nota = it },
                    label = { Text("Nota (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = contadoNum != null && fondoNum != null,
                onClick = { vm.cerrarCaja(fondoNum ?: 0.0, contadoNum ?: 0.0, nota); alCerrar() },
            ) { Text("Guardar cierre") }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } },
    )
}

private fun String.simple(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()

@Composable
private fun DialogoEditarVenta(
    venta: Venta,
    menu: List<Producto>,
    alCerrar: () -> Unit,
    alGuardar: (Venta) -> Unit,
    alBorrar: () -> Unit,
) {
    val items = remember { mutableStateListOf<VentaItem>().apply { addAll(venta.items) } }
    var metodo by remember { mutableStateOf(venta.metodoPago.ifBlank { Categorias.metodosPago.first() }) }
    var descuento by remember { mutableStateOf(if (venta.descuento > 0) Formato.paraEditar(venta.descuento) else "") }
    var enPorcentaje by remember { mutableStateOf(false) }
    var buscando by remember { mutableStateOf("") }
    var confirmarBorrar by remember { mutableStateOf(false) }

    val editada = venta.copy(items = items.filter { it.cantidad > 0 }, metodoPago = metodo)
    val subtotal = editada.subtotal
    val descNum = if (descuento.isBlank()) 0.0 else Formato.leerNumero(descuento)
    val descPlata = when {
        descNum == null -> null
        enPorcentaje -> subtotal * descNum.coerceAtMost(100.0) / 100
        else -> descNum.coerceAtMost(subtotal)
    }

    fun cambiar(i: Int, delta: Int) {
        items[i] = items[i].copy(cantidad = (items[i].cantidad + delta).coerceAtLeast(0))
    }

    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Editar venta") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(Formato.fechaHora(venta.fecha) + if (venta.vendedor.isNotBlank()) " · ${venta.vendedor}" else "", color = TextoSuave)
                items.forEachIndexed { i, item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.nombreProducto, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        IconButton(onClick = { cambiar(i, -1) }) { Icon(Icons.Filled.Remove, contentDescription = "Quitar uno") }
                        Text("${item.cantidad}", Modifier.width(20.dp), textAlign = TextAlign.Center)
                        IconButton(onClick = { cambiar(i, 1) }) { Icon(Icons.Filled.Add, contentDescription = "Agregar uno") }
                    }
                }
                OutlinedTextField(
                    value = buscando,
                    onValueChange = { buscando = it },
                    label = { Text("Agregar otro producto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (buscando.isNotBlank()) {
                    val q = buscando.trim().simple()
                    Column(Modifier.heightIn(max = 180.dp)) {
                        menu.filter { it.nombre.simple().contains(q) }.take(5).forEach { p ->
                            TextButton(onClick = {
                                val i = items.indexOfFirst { it.productoId == p.id }
                                if (i >= 0) cambiar(i, 1)
                                else items.add(VentaItem(p.id, p.nombre, 1, p.precio, p.costo))
                                buscando = ""
                            }) { Text("+ ${p.nombre} · ${Formato.dinero(p.precio)}") }
                        }
                    }
                }
                HorizontalDivider()
                Text("Medio de pago", style = MaterialTheme.typography.labelMedium)
                Opciones(Categorias.metodosPago, metodo) { metodo = it }
                Text("Descuento", style = MaterialTheme.typography.labelMedium)
                Opciones(listOf("$", "%"), if (enPorcentaje) "%" else "$") { enPorcentaje = it == "%" }
                CampoNumero(descuento, if (enPorcentaje) "Porcentaje" else "Monto") { descuento = it }
                Row { Text("Subtotal", Modifier.weight(1f), color = TextoSuave); Text(Formato.dinero(subtotal)) }
                Row {
                    Text("Total", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text(Formato.dinero(subtotal - (descPlata ?: 0.0)), fontWeight = FontWeight.Bold, color = Ambar)
                }
                TextButton(onClick = { confirmarBorrar = true }) { Text("Borrar venta", color = Perdida) }
            }
        },
        confirmButton = {
            Button(
                enabled = descPlata != null && editada.items.isNotEmpty(),
                onClick = { alGuardar(editada.copy(descuento = descPlata ?: 0.0)) },
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } },
    )

    if (confirmarBorrar) {
        AlertDialog(
            onDismissRequest = { confirmarBorrar = false },
            title = { Text("¿Borrar esta venta?") },
            text = { Text("${Formato.fechaHora(venta.fecha)} por ${Formato.dinero(venta.total)}. Se descuenta del balance.") },
            confirmButton = { TextButton(onClick = { confirmarBorrar = false; alBorrar() }) { Text("Borrar") } },
            dismissButton = { TextButton(onClick = { confirmarBorrar = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
fun Dato(titulo: String, valor: String, detalle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 6.dp)) {
        Text(titulo, style = MaterialTheme.typography.labelMedium, color = TextoSuave)
        Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (detalle != null) Text(detalle, style = MaterialTheme.typography.bodySmall, color = TextoSuave)
    }
}
