package com.cosmica.cafeteria.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmica.cafeteria.CafeViewModel
import com.cosmica.cafeteria.data.Categorias
import com.cosmica.cafeteria.data.Producto
import com.cosmica.cafeteria.data.Venta
import com.cosmica.cafeteria.ui.Formato
import com.cosmica.cafeteria.ui.Opciones
import com.cosmica.cafeteria.ui.theme.Ambar
import com.cosmica.cafeteria.ui.theme.Borde
import com.cosmica.cafeteria.ui.theme.SobreAmbar
import com.cosmica.cafeteria.ui.theme.Tarjeta
import com.cosmica.cafeteria.ui.theme.Tarjeta2
import com.cosmica.cafeteria.ui.theme.TextoSuave
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private const val TODOS = "Todo"

/** Texto sin tildes y en minúsculas, para que "cafe" encuentre "Café". */
private fun String.simple(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()

private fun Producto.esCombo() = categoria.simple().contains("combo")

@Composable
fun VenderScreen(vm: CafeViewModel, snackbar: SnackbarHostState, modifier: Modifier) {
    val menu by vm.menu.collectAsState()
    val carrito by vm.carrito.collectAsState()
    val masVendidos by vm.masVendidos.collectAsState()
    val ultima by vm.ultimaVenta.collectAsState()
    var busqueda by rememberSaveable { mutableStateOf("") }
    var categoria by rememberSaveable { mutableStateOf(TODOS) }
    var cobrando by remember { mutableStateOf(false) }
    val foco = LocalFocusManager.current
    val pantalla = rememberCoroutineScope()

    val lineas = menu.filter { (carrito[it.id] ?: 0) > 0 }
    val unidades = lineas.sumOf { carrito[it.id] ?: 0 }
    val total = lineas.sumOf { it.precio * (carrito[it.id] ?: 0) }
    val sumar: (Producto) -> Unit = { vm.sumarAlCarrito(it.id, 1) }

    Column(modifier.fillMaxSize()) {
        Buscador(busqueda, { busqueda = it }) { busqueda = ""; foco.clearFocus() }

        if (menu.isEmpty()) {
            Text(
                "Todavía no hay productos. Agregalos en la pestaña Menú.",
                Modifier.weight(1f).fillMaxWidth().padding(32.dp),
                textAlign = TextAlign.Center,
                color = TextoSuave,
            )
        } else if (busqueda.isNotBlank()) {
            val q = busqueda.trim().simple()
            val encontrados = menu.filter { it.nombre.simple().contains(q) }
                .sortedBy { if (it.nombre.simple().startsWith(q)) 0 else 1 }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                item {
                    Titulo(if (encontrados.isEmpty()) "Nada con \"${busqueda.trim()}\"" else "${encontrados.size} resultados")
                }
                items(encontrados, key = { it.id }) { p -> FilaResultado(p, carrito[p.id] ?: 0) { sumar(p) } }
            }
        } else {
            val categorias = listOf(TODOS) + menu.map { it.categoria }.distinct()
            val visibles = if (categoria == TODOS) menu else menu.filter { it.categoria == categoria }
            val combos = menu.filter { it.esCombo() }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (masVendidos.isNotEmpty()) {
                    titulo("Lo más vendido a esta hora")
                    items(masVendidos, key = { "top-" + it.id }) { p -> Mosaico(p, carrito[p.id] ?: 0) { sumar(p) } }
                }
                if (combos.isNotEmpty()) {
                    titulo("Combos de un toque")
                    items(combos, key = { "combo-" + it.id }, span = { GridItemSpan(maxLineSpan) }) { p ->
                        FilaCombo(p, carrito[p.id] ?: 0) { sumar(p) }
                    }
                }
                ultima?.let { v ->
                    item(key = "repetir", span = { GridItemSpan(maxLineSpan) }) { Repetir(v) { vm.repetir(v) } }
                }
                titulo("Todo el menú")
                item(key = "categorias", span = { GridItemSpan(maxLineSpan) }) {
                    Opciones(categorias, categoria) { categoria = it }
                }
                items(visibles, key = { "menu-" + it.id }) { p -> Mosaico(p, carrito[p.id] ?: 0) { sumar(p) } }
            }
        }

        if (lineas.isNotEmpty()) {
            BarraCarrito(unidades, total, alVaciar = { vm.vaciarCarrito() }) { foco.clearFocus(); cobrando = true }
        }
    }

    if (cobrando && lineas.isNotEmpty()) {
        HojaCobro(
            vm = vm,
            lineas = lineas,
            carrito = carrito,
            total = total,
            alCerrar = { cobrando = false },
            alCobrar = { texto ->
                cobrando = false
                busqueda = ""
                pantalla.launch { snackbar.showSnackbar(texto) }
            },
        )
    }
}

@Composable
private fun Buscador(valor: String, alCambiar: (String) -> Unit, alLimpiar: () -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        placeholder = { Text("Buscar producto…", color = TextoSuave) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextoSuave) },
        trailingIcon = {
            if (valor.isNotEmpty()) {
                IconButton(onClick = alLimpiar) { Icon(Icons.Filled.Close, contentDescription = "Borrar búsqueda") }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Tarjeta,
            unfocusedContainerColor = Tarjeta,
            unfocusedBorderColor = Borde,
            focusedBorderColor = Ambar,
        ),
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

private fun LazyGridScope.titulo(texto: String) {
    item(key = "t-$texto", span = { GridItemSpan(maxLineSpan) }) { Titulo(texto) }
}

@Composable
private fun Titulo(texto: String) {
    Text(
        texto.uppercase(),
        Modifier.padding(top = 14.dp, bottom = 2.dp),
        color = TextoSuave,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
    )
}

@Composable
private fun Insignia(cantidad: Int, modifier: Modifier = Modifier) {
    Box(modifier.size(24.dp), contentAlignment = Alignment.Center) {
        Surface(shape = CircleShape, color = Ambar, modifier = Modifier.fillMaxSize()) {}
        Text("$cantidad", color = SobreAmbar, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
    }
}

@Composable
private fun Mosaico(p: Producto, cantidad: Int, alTocar: () -> Unit) {
    Surface(
        onClick = alTocar,
        shape = RoundedCornerShape(16.dp),
        color = if (cantidad > 0) Tarjeta2 else Tarjeta,
        border = BorderStroke(1.dp, if (cantidad > 0) Ambar.copy(alpha = 0.6f) else Borde),
        modifier = Modifier.height(88.dp),
    ) {
        Box(Modifier.padding(10.dp)) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Text(
                    p.nombre,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = if (cantidad > 0) 22.dp else 0.dp),
                )
                Text(Formato.dinero(p.precio), color = Ambar, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
            if (cantidad > 0) Insignia(cantidad, Modifier.align(Alignment.TopEnd))
        }
    }
}

@Composable
private fun BotonMas(cantidad: Int, alTocar: () -> Unit) {
    Box {
        Surface(
            onClick = alTocar,
            shape = RoundedCornerShape(12.dp),
            color = if (cantidad > 0) Ambar else Tarjeta2,
            contentColor = if (cantidad > 0) SobreAmbar else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (cantidad > 0) Text("$cantidad", fontWeight = FontWeight.ExtraBold)
                else Icon(Icons.Filled.Add, contentDescription = "Agregar")
            }
        }
    }
}

@Composable
private fun FilaResultado(p: Producto, cantidad: Int, alTocar: () -> Unit) {
    Surface(onClick = alTocar, color = MaterialTheme.colorScheme.background) {
        Column {
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.nombre, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("${p.categoria} · ${Formato.dinero(p.precio)}", color = TextoSuave, fontSize = 12.sp)
                }
                BotonMas(cantidad, alTocar)
            }
            HorizontalDivider(color = Borde)
        }
    }
}

@Composable
private fun FilaCombo(p: Producto, cantidad: Int, alTocar: () -> Unit) {
    Surface(
        onClick = alTocar,
        shape = RoundedCornerShape(16.dp),
        color = Tarjeta,
        border = BorderStroke(1.dp, if (cantidad > 0) Ambar.copy(alpha = 0.6f) else Borde),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocalCafe, contentDescription = null, tint = Ambar)
            Text(
                p.nombre,
                Modifier.weight(1f).padding(horizontal = 12.dp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(Formato.dinero(p.precio), color = Ambar, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.width(10.dp))
            BotonMas(cantidad, alTocar)
        }
    }
}

@Composable
private fun Repetir(v: Venta, alTocar: () -> Unit) {
    val detalle = v.items.joinToString(" + ") { if (it.cantidad > 1) "${it.cantidad} ${it.nombreProducto}" else it.nombreProducto }
    Surface(
        onClick = alTocar,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, Borde),
        modifier = Modifier.padding(top = 10.dp),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.History, contentDescription = null, tint = TextoSuave, modifier = Modifier.size(18.dp))
            Text(
                "Repetir última venta · $detalle",
                Modifier.padding(start = 10.dp),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BarraCarrito(unidades: Int, total: Double, alVaciar: () -> Unit, alCobrar: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Tarjeta2,
        border = BorderStroke(1.dp, Color2A),
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (unidades == 1) "1 producto" else "$unidades productos", color = TextoSuave, fontSize = 12.sp)
                Text(Formato.dinero(total), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            }
            IconButton(onClick = alVaciar) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Vaciar pedido", tint = TextoSuave)
            }
            Button(
                onClick = alCobrar,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
            ) { Text("Cobrar", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) }
        }
    }
}

private val Color2A = androidx.compose.ui.graphics.Color(0xFF2A3470)

/** Hoja que sube desde abajo con el detalle del pedido, la fecha y el medio de pago. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HojaCobro(
    vm: CafeViewModel,
    lineas: List<Producto>,
    carrito: Map<String, Int>,
    total: Double,
    alCerrar: () -> Unit,
    alCobrar: (String) -> Unit,
) {
    var metodo by rememberSaveable { mutableStateOf(Categorias.metodosPago.first()) }
    // Día de la venta como "epoch day"; null = hoy.
    var diaElegido by rememberSaveable { mutableStateOf<Long?>(null) }
    var eligiendoDia by remember { mutableStateOf(false) }
    val dia = diaElegido?.let { LocalDate.ofEpochDay(it) }
    val estado = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = alCerrar, sheetState = estado, containerColor = Tarjeta) {
        Column(Modifier.padding(horizontal = 18.dp).navigationBarsPadding().padding(bottom = 12.dp)) {
            Text("Pedido", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            LazyColumn(Modifier.heightIn(max = 260.dp).padding(top = 6.dp)) {
                items(lineas, key = { it.id }) { p ->
                    val c = carrito[p.id] ?: 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.nombre, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        IconButton(onClick = { vm.sumarAlCarrito(p.id, -1) }) {
                            Icon(Icons.Filled.Remove, contentDescription = "Quitar uno")
                        }
                        Text("$c", Modifier.width(22.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { vm.sumarAlCarrito(p.id, 1) }) {
                            Icon(Icons.Filled.Add, contentDescription = "Agregar uno")
                        }
                        Text(Formato.dinero(p.precio * c), Modifier.width(84.dp), textAlign = TextAlign.End)
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Borde)
            Text("Medio de pago", color = TextoSuave, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
            Opciones(Categorias.metodosPago, metodo) { metodo = it }
            FilterChip(
                selected = dia != null,
                onClick = { eligiendoDia = true },
                leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                label = { Text(if (dia == null) "Fecha: hoy" else "Fecha: ${Formato.dia(dia)} (venta atrasada)") },
                modifier = Modifier.padding(top = 4.dp),
            )
            Button(
                onClick = {
                    vm.cobrar(metodo, dia)?.let { cobrado ->
                        val cuando = if (dia == null) "" else " el ${Formato.dia(dia)}"
                        val texto = "Venta registrada$cuando: ${Formato.dinero(cobrado)}"
                        alCobrar(texto)
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) {
                Text("Cobrar ${Formato.dinero(total)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
    }

    if (eligiendoDia) {
        ElegirDia(
            inicial = dia ?: LocalDate.now(),
            alCerrar = { eligiendoDia = false },
            alElegir = { d ->
                diaElegido = if (d == LocalDate.now()) null else d.toEpochDay()
                eligiendoDia = false
            },
        )
    }
}

/** Calendario para elegir el día de una venta; no deja elegir días futuros. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ElegirDia(inicial: LocalDate, alCerrar: () -> Unit, alElegir: (LocalDate) -> Unit) {
    // El calendario trabaja en milisegundos UTC a medianoche.
    val hoyUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val estado = rememberDatePickerState(
        initialSelectedDateMillis = inicial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= hoyUtc
        },
    )
    DatePickerDialog(
        onDismissRequest = alCerrar,
        confirmButton = {
            TextButton(onClick = {
                estado.selectedDateMillis?.let { ms ->
                    alElegir(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate())
                } ?: alCerrar()
            }) { Text("Listo") }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } },
    ) {
        DatePicker(state = estado, title = { Text("¿Qué día fue la venta?", Modifier.padding(start = 24.dp, top = 16.dp)) })
    }
}
