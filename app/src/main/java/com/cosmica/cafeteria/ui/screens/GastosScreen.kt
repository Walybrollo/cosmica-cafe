package com.cosmica.cafeteria.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.cosmica.cafeteria.data.Categorias
import com.cosmica.cafeteria.data.Gasto
import com.cosmica.cafeteria.data.Insumo
import com.cosmica.cafeteria.ui.CampoNumero
import com.cosmica.cafeteria.ui.Formato
import com.cosmica.cafeteria.ui.Opciones
import com.cosmica.cafeteria.ui.SelectorMes

@Composable
fun GastosScreen(vm: CafeViewModel, modifier: Modifier) {
    val gastos by vm.gastos.collectAsState()
    val insumos by vm.insumos.collectAsState()
    val rubros by vm.rubros.collectAsState()
    // null = cerrado; un Gasto sin id = gasto nuevo.
    var editando by remember { mutableStateOf<Gasto?>(null) }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            SelectorMes(vm)
            Dato(
                "Gastos del mes",
                Formato.dinero(gastos.sumOf { it.monto }),
                "${gastos.size} registros",
                Modifier.padding(horizontal = 16.dp),
            )
            if (gastos.isEmpty()) {
                Text(
                    "No hay gastos en este mes. Tocá \"Nuevo gasto\" para cargar uno.",
                    Modifier.fillMaxWidth().padding(32.dp),
                    textAlign = TextAlign.Center,
                )
            }
            LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 88.dp)) {
                items(gastos, key = { it.id }) { g ->
                    Card(onClick = { editando = g }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(g.descripcion, fontWeight = FontWeight.SemiBold)
                                Text(
                                    listOf(Formato.fecha(g.fecha), if (g.mercaderia) "Mercadería · ${g.categoria}" else g.categoria, g.cargadoPor).filter { it.isNotBlank() }.joinToString(" · "),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                            Text(Formato.dinero(g.monto), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = {
                editando = Gasto(
                    fecha = fechaParaNuevoGasto(vm),
                    descripcion = "",
                    categoria = rubros.first(),
                    monto = 0.0,
                    mercaderia = true,
                )
            },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Nuevo gasto") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    editando?.let { g ->
        DialogoGasto(
            gasto = g,
            insumos = insumos,
            rubros = rubros,
            alCerrar = { editando = null },
            alGuardar = { gasto, compra -> vm.guardarGasto(gasto, compra); editando = null },
            alBorrar = { vm.borrarGasto(g); editando = null },
        )
    }
}

/** Si se está mirando otro mes, el gasto nuevo se anota el día 1 de ese mes; si no, ahora. */
private fun fechaParaNuevoGasto(vm: CafeViewModel): Long {
    val mes = vm.mes.value
    return if (mes == java.time.YearMonth.now()) System.currentTimeMillis()
    else mes.atDay(1).atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
}

@Composable
private fun DialogoGasto(
    gasto: Gasto,
    insumos: List<Insumo>,
    rubros: List<String>,
    alCerrar: () -> Unit,
    alGuardar: (Gasto, Pair<Insumo, Double>?) -> Unit,
    alBorrar: () -> Unit,
) {
    var insumoNombre by remember { mutableStateOf(NINGUNO) }
    var cantidadComprada by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf(gasto.descripcion) }
    var monto by remember { mutableStateOf(if (gasto.id.isEmpty()) "" else Formato.paraEditar(gasto.monto)) }
    var tipo by remember { mutableStateOf(if (gasto.mercaderia) MERCADERIA else LOCAL) }
    val opcionesRubro = (rubros + gasto.categoria.takeIf { gasto.mercaderia }).filterNotNull().distinct() + NUEVO
    var rubro by remember { mutableStateOf(if (gasto.mercaderia) gasto.categoria else rubros.first()) }
    var rubroNuevo by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf(if (gasto.mercaderia) Categorias.gastos.first() else gasto.categoria) }
    val esMercaderia = tipo == MERCADERIA
    val categoriaFinal = when {
        !esMercaderia -> categoria
        rubro == NUEVO -> rubroNuevo.trim()
        else -> rubro
    }
    val montoNum = Formato.leerNumero(monto)
    val insumo = if (gasto.id.isEmpty() && esMercaderia) insumos.firstOrNull { it.nombre == insumoNombre } else null
    val cantidadNum = Formato.leerNumero(cantidadComprada)
    val compra = if (insumo != null && cantidadNum != null && cantidadNum > 0) insumo to cantidadNum else null
    val valido = descripcion.isNotBlank() && montoNum != null && montoNum > 0 && categoriaFinal.isNotBlank() &&
        (insumo == null || compra != null)

    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(if (gasto.id.isEmpty()) "Nuevo gasto" else "Editar gasto") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción (ej: leche, pan, alquiler)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                CampoNumero(monto, "Monto") { monto = it }
                Text("Tipo", style = MaterialTheme.typography.labelMedium)
                Opciones(listOf(MERCADERIA, LOCAL), tipo) { tipo = it }
                if (esMercaderia) {
                    Text(
                        "Lo que se usa para hacer los productos (leche, pan, fiambre, mango, vasos). " +
                            "Se ve en el Balance pero no se resta de nuevo, porque ya está en el costo de cada producto.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text("Rubro", style = MaterialTheme.typography.labelMedium)
                    Opciones(opcionesRubro, rubro) { rubro = it }
                    if (rubro == NUEVO) {
                        OutlinedTextField(
                            value = rubroNuevo,
                            onValueChange = { rubroNuevo = it },
                            label = { Text("Nombre del rubro (ej: Tortas)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (gasto.id.isEmpty() && insumos.isNotEmpty()) {
                        Text("¿Sumar al stock?", style = MaterialTheme.typography.labelMedium)
                        Opciones(listOf(NINGUNO) + insumos.map { it.nombre }, insumoNombre) { insumoNombre = it }
                        if (insumo != null) CampoNumero(cantidadComprada, "Cantidad comprada (${insumo.unidad})") { cantidadComprada = it }
                    }
                } else {
                    Text(
                        "Alquiler, luz, gas, internet, limpieza, arreglos. Estos sí se restan de la ganancia.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text("Categoría", style = MaterialTheme.typography.labelMedium)
                    Opciones(Categorias.gastos, categoria) { categoria = it }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valido,
                onClick = {
                    alGuardar(
                        gasto.copy(
                            descripcion = descripcion.trim(),
                            monto = montoNum ?: 0.0,
                            categoria = categoriaFinal,
                            mercaderia = esMercaderia,
                        ),
                        compra,
                    )
                },
            ) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (gasto.id.isNotEmpty()) TextButton(onClick = alBorrar) { Text("Borrar") }
                TextButton(onClick = alCerrar) { Text("Cancelar") }
            }
        },
    )
}

private const val NINGUNO = "No"
private const val NUEVO = "+ Nuevo rubro"
private const val MERCADERIA = "Mercadería"
private const val LOCAL = "Gasto del local"
