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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.cosmica.cafeteria.EstadoInsumo
import com.cosmica.cafeteria.data.Categorias
import com.cosmica.cafeteria.data.Insumo
import com.cosmica.cafeteria.data.Producto
import com.cosmica.cafeteria.ui.CampoNumero
import com.cosmica.cafeteria.ui.Formato
import com.cosmica.cafeteria.ui.Opciones
import com.cosmica.cafeteria.ui.theme.Ambar
import com.cosmica.cafeteria.ui.theme.Perdida
import com.cosmica.cafeteria.ui.theme.SobreAmbar
import com.cosmica.cafeteria.ui.theme.TextoSuave

@Composable
fun StockScreen(vm: CafeViewModel, modifier: Modifier) {
    val stock by vm.stock.collectAsState()
    val menu by vm.menu.collectAsState()
    var editando by remember { mutableStateOf<Insumo?>(null) }
    var comprando by remember { mutableStateOf<Insumo?>(null) }
    var contando by remember { mutableStateOf<Insumo?>(null) }
    val paraComprar = stock.filter { it.hayQueComprar }

    Box(modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 88.dp)) {
            if (stock.isEmpty()) {
                item {
                    Text(
                        "Cargá los insumos que querés controlar.\n\n" +
                            "Los que se venden por unidad (alfajores, galletas) se descuentan solos al vender.\n\n" +
                            "Los de granel (café, leche, vasos) se suman cuando cargás una compra y se corrigen " +
                            "con un conteo cada tanto. Con eso la app calcula para cuántos días te alcanza.",
                        Modifier.fillMaxWidth().padding(24.dp),
                        textAlign = TextAlign.Center,
                        color = TextoSuave,
                    )
                }
            }
            if (paraComprar.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Perdida.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = Perdida)
                            Column(Modifier.padding(start = 12.dp)) {
                                Text("Para comprar", fontWeight = FontWeight.Bold)
                                Text(paraComprar.joinToString { it.insumo.nombre })
                            }
                        }
                    }
                }
            }
            items(stock, key = { it.insumo.id }) { e ->
                TarjetaInsumo(
                    e,
                    producto = menu.firstOrNull { it.id == e.insumo.productoId },
                    alEditar = { editando = e.insumo },
                    alComprar = { comprando = e.insumo },
                    alContar = { contando = e.insumo },
                )
            }
        }
        ExtendedFloatingActionButton(
            onClick = { editando = Insumo(nombre = "", unidad = Categorias.unidades.first(), cantidad = 0.0, minimo = 0.0) },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Nuevo insumo") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    editando?.let { i ->
        DialogoInsumo(
            insumo = i,
            menu = menu,
            alCerrar = { editando = null },
            alGuardar = { vm.guardarInsumo(it); editando = null },
            alBorrar = { vm.borrarInsumo(i); editando = null },
        )
    }
    comprando?.let { i ->
        DialogoCantidad("Compra de ${i.nombre}", "Cantidad comprada (${i.unidad})", alCerrar = { comprando = null }) {
            vm.comprarInsumo(i, it); comprando = null
        }
    }
    contando?.let { i ->
        DialogoCantidad(
            "Conteo de ${i.nombre}",
            "¿Cuánto hay ahora? (${i.unidad})",
            ayuda = "La app tenía ${Formato.cantidad(i.cantidad)} ${i.unidad}. Lo que falte cuenta como consumo.",
            alCerrar = { contando = null },
        ) { vm.contarInsumo(i, it); contando = null }
    }
}

@Composable
private fun TarjetaInsumo(
    e: EstadoInsumo,
    producto: Producto?,
    alEditar: () -> Unit,
    alComprar: () -> Unit,
    alContar: () -> Unit,
) {
    val i = e.insumo
    Card(onClick = alEditar, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(i.nombre, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                if (e.hayQueComprar) {
                    Surface(color = Perdida, contentColor = SobreAmbar, shape = RoundedCornerShape(8.dp)) {
                        Text("Comprar", Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(
                "${Formato.cantidad(i.cantidad)} ${i.unidad}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (e.hayQueComprar) Perdida else Ambar,
            )
            val detalle = buildList {
                e.diasQueAlcanza?.let { add(if (it < 1) "Alcanza para menos de un día" else "Alcanza para ~${it.toInt()} días") }
                e.consumoPorDia?.let { add("se usan ~${Formato.cantidad(it)} por día") }
                if (producto != null) add("se descuenta al vender ${producto.nombre}")
                if (i.minimo > 0) add("mínimo ${Formato.cantidad(i.minimo)}")
            }
            if (detalle.isNotEmpty()) {
                Text(detalle.joinToString(" · ").replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = TextoSuave)
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = alComprar) { Text("Compré") }
                OutlinedButton(onClick = alContar) { Text("Conté") }
            }
        }
    }
}

@Composable
private fun DialogoCantidad(
    titulo: String,
    etiqueta: String,
    ayuda: String? = null,
    alCerrar: () -> Unit,
    alGuardar: (Double) -> Unit,
) {
    var valor by remember { mutableStateOf("") }
    val numero = Formato.leerNumero(valor)
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ayuda?.let { Text(it, color = TextoSuave) }
                CampoNumero(valor, etiqueta) { valor = it }
            }
        },
        confirmButton = { TextButton(enabled = numero != null, onClick = { alGuardar(numero ?: 0.0) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } },
    )
}

@Composable
private fun DialogoInsumo(
    insumo: Insumo,
    menu: List<Producto>,
    alCerrar: () -> Unit,
    alGuardar: (Insumo) -> Unit,
    alBorrar: () -> Unit,
) {
    val nuevo = insumo.id.isEmpty()
    var nombre by remember { mutableStateOf(insumo.nombre) }
    var unidad by remember { mutableStateOf(insumo.unidad) }
    var cantidad by remember { mutableStateOf(if (nuevo) "" else Formato.paraEditar(insumo.cantidad)) }
    var minimo by remember { mutableStateOf(if (insumo.minimo > 0) Formato.paraEditar(insumo.minimo) else "") }
    var productoId by remember { mutableStateOf(insumo.productoId) }
    var buscando by remember { mutableStateOf("") }
    var confirmarBorrar by remember { mutableStateOf(false) }

    val cantidadNum = if (cantidad.isBlank()) 0.0 else Formato.leerNumero(cantidad)
    val minimoNum = if (minimo.isBlank()) 0.0 else Formato.leerNumero(minimo)
    val valido = nombre.isNotBlank() && cantidadNum != null && minimoNum != null
    val ligado = menu.firstOrNull { it.id == productoId }

    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(if (nuevo) "Nuevo insumo" else "Editar insumo") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre (ej: café en grano, alfajores)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Se mide en", style = MaterialTheme.typography.labelMedium)
                Opciones(Categorias.unidades, unidad) { unidad = it }
                if (nuevo) CampoNumero(cantidad, "Cuánto hay ahora") { cantidad = it }
                CampoNumero(minimo, "Avisar cuando quede menos de") { minimo = it }
                Text("¿Se descuenta solo al vender un producto?", style = MaterialTheme.typography.labelMedium)
                Text(
                    ligado?.let { "Sí: 1 por cada ${it.nombre} vendido" } ?: "No (se controla con compras y conteos)",
                    color = if (ligado != null) Ambar else TextoSuave,
                )
                OutlinedTextField(
                    value = buscando,
                    onValueChange = { buscando = it },
                    label = { Text("Buscar producto para ligar") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (buscando.isNotBlank()) {
                    menu.filter { it.nombre.contains(buscando.trim(), ignoreCase = true) }.take(5).forEach { p ->
                        TextButton(onClick = { productoId = p.id; buscando = "" }) { Text(p.nombre) }
                    }
                }
                if (ligado != null) TextButton(onClick = { productoId = "" }) { Text("No ligar a ningún producto") }
                if (!nuevo) TextButton(onClick = { confirmarBorrar = true }) { Text("Borrar insumo", color = Perdida) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valido,
                onClick = {
                    alGuardar(
                        insumo.copy(
                            nombre = nombre.trim(),
                            unidad = unidad,
                            cantidad = if (nuevo) cantidadNum ?: 0.0 else insumo.cantidad,
                            minimo = minimoNum ?: 0.0,
                            productoId = productoId,
                        )
                    )
                },
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } },
    )

    if (confirmarBorrar) {
        AlertDialog(
            onDismissRequest = { confirmarBorrar = false },
            title = { Text("¿Borrar ${insumo.nombre}?") },
            text = { Text("Se deja de controlar su stock.") },
            confirmButton = { TextButton(onClick = { confirmarBorrar = false; alBorrar() }) { Text("Borrar") } },
            dismissButton = { TextButton(onClick = { confirmarBorrar = false }) { Text("Cancelar") } },
        )
    }
}
