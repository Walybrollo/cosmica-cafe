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
import androidx.compose.ui.unit.dp
import com.cosmica.cafeteria.CafeViewModel
import com.cosmica.cafeteria.data.Categorias
import com.cosmica.cafeteria.data.Producto
import com.cosmica.cafeteria.data.ganancia
import com.cosmica.cafeteria.data.margen
import com.cosmica.cafeteria.ui.CampoNumero
import com.cosmica.cafeteria.ui.Formato
import com.cosmica.cafeteria.ui.Opciones
import com.cosmica.cafeteria.ui.theme.Ganancia
import com.cosmica.cafeteria.ui.theme.Perdida

@Composable
fun MenuScreen(vm: CafeViewModel, modifier: Modifier) {
    val menu by vm.menu.collectAsState()
    var editando by remember { mutableStateOf<Producto?>(null) }
    var aQuitar by remember { mutableStateOf<Producto?>(null) }

    Box(modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 88.dp)) {
            menu.groupBy { it.categoria }.forEach { (categoria, productos) ->
                item(key = "cat-$categoria") {
                    Text(
                        categoria,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                }
                items(productos, key = { it.id }) { p ->
                    Card(onClick = { editando = p }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.nombre, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Costo ${Formato.dinero(p.costo)} · Ganancia ${Formato.dinero(p.ganancia)} (${Formato.porcentaje(p.margen)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (p.ganancia >= 0) Ganancia else Perdida,
                                )
                            }
                            Text(Formato.dinero(p.precio), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = {
                editando = Producto(nombre = "", categoria = Categorias.productos.first(), precio = 0.0, costo = 0.0)
            },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Agregar producto") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    editando?.let { p ->
        DialogoProducto(
            producto = p,
            alCerrar = { editando = null },
            alGuardar = { vm.guardarProducto(it); editando = null },
            alQuitar = { aQuitar = p; editando = null },
        )
    }

    aQuitar?.let { p ->
        AlertDialog(
            onDismissRequest = { aQuitar = null },
            title = { Text("¿Quitar \"${p.nombre}\" del menú?") },
            text = { Text("Las ventas que ya se hicieron siguen contando en el balance.") },
            confirmButton = { TextButton(onClick = { vm.quitarProducto(p); aQuitar = null }) { Text("Quitar") } },
            dismissButton = { TextButton(onClick = { aQuitar = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun DialogoProducto(producto: Producto, alCerrar: () -> Unit, alGuardar: (Producto) -> Unit, alQuitar: () -> Unit) {
    val nuevo = producto.id.isEmpty()
    var nombre by remember { mutableStateOf(producto.nombre) }
    var categoria by remember { mutableStateOf(producto.categoria) }
    var precio by remember { mutableStateOf(if (nuevo) "" else Formato.paraEditar(producto.precio)) }
    var costo by remember { mutableStateOf(if (nuevo) "" else Formato.paraEditar(producto.costo)) }
    val precioNum = Formato.leerNumero(precio)
    val costoNum = Formato.leerNumero(costo)
    val valido = nombre.isNotBlank() && precioNum != null && costoNum != null

    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(if (nuevo) "Nuevo producto" else "Editar producto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Categoría", style = MaterialTheme.typography.labelMedium)
                Opciones(Categorias.productos, categoria) { categoria = it }
                CampoNumero(precio, "Precio de venta") { precio = it }
                CampoNumero(costo, "Costo (lo que te sale hacerlo)") { costo = it }
                if (precioNum != null && costoNum != null) {
                    val g = precioNum - costoNum
                    Text(
                        "Ganancia por unidad: ${Formato.dinero(g)}" +
                            if (precioNum > 0) " (${Formato.porcentaje(g / precioNum)})" else "",
                        color = if (g >= 0) Ganancia else Perdida,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valido,
                onClick = {
                    alGuardar(
                        producto.copy(
                            nombre = nombre.trim(),
                            categoria = categoria,
                            precio = precioNum ?: 0.0,
                            costo = costoNum ?: 0.0,
                        )
                    )
                },
            ) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (!nuevo) TextButton(onClick = alQuitar) { Text("Quitar del menú") }
                TextButton(onClick = alCerrar) { Text("Cancelar") }
            }
        },
    )
}
