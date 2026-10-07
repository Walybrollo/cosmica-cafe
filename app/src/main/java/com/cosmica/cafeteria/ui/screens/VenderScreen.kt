package com.cosmica.cafeteria.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cosmica.cafeteria.CafeViewModel
import com.cosmica.cafeteria.data.Categorias
import com.cosmica.cafeteria.ui.Formato
import com.cosmica.cafeteria.ui.Opciones
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private const val TODOS = "Todos"

@Composable
fun VenderScreen(vm: CafeViewModel, snackbar: SnackbarHostState, modifier: Modifier) {
    val menu by vm.menu.collectAsState()
    val carrito by vm.carrito.collectAsState()
    var categoria by rememberSaveable { mutableStateOf(TODOS) }
    var metodo by rememberSaveable { mutableStateOf(Categorias.metodosPago.first()) }
    val scope = rememberCoroutineScope()
    // Día de la venta como "epoch day"; null = hoy.
    var diaElegido by rememberSaveable { mutableStateOf<Long?>(null) }
    var eligiendoDia by remember { mutableStateOf(false) }
    val dia = diaElegido?.let { LocalDate.ofEpochDay(it) }

    val categorias = listOf(TODOS) + menu.map { it.categoria }.distinct()
    val visibles = if (categoria == TODOS) menu else menu.filter { it.categoria == categoria }
    val lineas = menu.filter { (carrito[it.id] ?: 0) > 0 }
    val total = lineas.sumOf { it.precio * (carrito[it.id] ?: 0) }

    Column(modifier.fillMaxSize()) {
        Opciones(categorias, categoria, Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) { categoria = it }

        if (menu.isEmpty()) {
            Text(
                "Todavía no hay productos. Agregalos en la pestaña Menú.",
                Modifier.weight(1f).fillMaxWidth().padding(32.dp),
                textAlign = TextAlign.Center,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(visibles, key = { it.id }) { p ->
                    val cantidad = carrito[p.id] ?: 0
                    Card(
                        onClick = { vm.sumarAlCarrito(p.id, 1) },
                        colors = if (cantidad > 0) CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ) else CardDefaults.cardColors(),
                    ) {
                        Column(Modifier.padding(12.dp).fillMaxWidth()) {
                            Text(p.nombre, fontWeight = FontWeight.SemiBold, maxLines = 2)
                            Spacer(Modifier.weight(1f, fill = false))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(Formato.dinero(p.precio), Modifier.weight(1f))
                                if (cantidad > 0) {
                                    Text(
                                        "x$cantidad",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (lineas.isNotEmpty()) {
            Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pedido", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = { vm.vaciarCarrito() }) { Text("Vaciar") }
                    }
                    LazyColumn(Modifier.heightIn(max = 170.dp)) {
                        items(lineas, key = { it.id }) { p ->
                            val c = carrito[p.id] ?: 0
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(p.nombre, Modifier.weight(1f))
                                IconButton(onClick = { vm.sumarAlCarrito(p.id, -1) }) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Quitar uno")
                                }
                                Text("$c", Modifier.width(24.dp), textAlign = TextAlign.Center)
                                IconButton(onClick = { vm.sumarAlCarrito(p.id, 1) }) {
                                    Icon(Icons.Filled.Add, contentDescription = "Agregar uno")
                                }
                                Text(
                                    Formato.dinero(p.precio * c),
                                    Modifier.width(90.dp),
                                    textAlign = TextAlign.End,
                                )
                            }
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    FilterChip(
                        selected = dia != null,
                        onClick = { eligiendoDia = true },
                        leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                        label = { Text(if (dia == null) "Fecha: hoy" else "Fecha: ${Formato.dia(dia)} (venta atrasada)") },
                    )
                    Opciones(Categorias.metodosPago, metodo) { metodo = it }
                    Button(
                        onClick = {
                            vm.cobrar(metodo, dia)?.let { cobrado ->
                                val cuando = if (dia == null) "" else " el ${Formato.dia(dia)}"
                                diaElegido = null
                                scope.launch { snackbar.showSnackbar("Venta registrada$cuando: ${Formato.dinero(cobrado)}") }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    ) {
                        Text("Cobrar ${Formato.dinero(total)}", style = MaterialTheme.typography.titleMedium)
                    }
                }
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
