package com.cosmica.cafeteria.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.cosmica.cafeteria.CafeViewModel
import com.cosmica.cafeteria.ui.screens.BalanceScreen
import com.cosmica.cafeteria.ui.screens.GastosScreen
import com.cosmica.cafeteria.ui.screens.LoginScreen
import com.cosmica.cafeteria.ui.screens.MenuScreen
import com.cosmica.cafeteria.ui.screens.VenderScreen
import com.cosmica.cafeteria.ui.screens.VentasScreen

private enum class Seccion(val titulo: String, val icono: ImageVector) {
    Vender("Vender", Icons.Filled.PointOfSale),
    Ventas("Ventas", Icons.Filled.Receipt),
    Gastos("Gastos", Icons.Filled.Payments),
    Menu("Menú", Icons.Filled.RestaurantMenu),
    Balance("Balance", Icons.Filled.BarChart),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCafe(vm: CafeViewModel) {
    val usuario by vm.usuario.collectAsState()
    val u = usuario
    if (u == null) {
        Surface(Modifier.fillMaxSize()) { LoginScreen(vm) }
        return
    }
    var actual by rememberSaveable { mutableIntStateOf(0) }
    val seccion = Seccion.entries[actual]
    val snackbar = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (seccion == Seccion.Vender) "Cósmica Café" else seccion.titulo) },
                actions = {
                    Text(u.nombre, color = MaterialTheme.colorScheme.onPrimary)
                    IconButton(onClick = { vm.salir() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Cerrar sesión",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                Seccion.entries.forEachIndexed { i, s ->
                    NavigationBarItem(
                        selected = i == actual,
                        onClick = { actual = i },
                        icon = { Icon(s.icono, contentDescription = null) },
                        label = { Text(s.titulo) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val problema by vm.problema.collectAsState()
        val pendientes by vm.pendientes.collectAsState()
        Column(Modifier.padding(padding)) {
            AvisoSincronizacion(problema, pendientes)
            val m = Modifier.weight(1f)
            when (seccion) {
            Seccion.Vender -> VenderScreen(vm, snackbar, m)
            Seccion.Ventas -> VentasScreen(vm, m)
            Seccion.Gastos -> GastosScreen(vm, m)
            Seccion.Menu -> MenuScreen(vm, m)
            Seccion.Balance -> BalanceScreen(vm, m)
            }
        }
    }
}

/** Franja que avisa si los datos no se están compartiendo con el otro teléfono. */
@Composable
private fun AvisoSincronizacion(problema: String?, pendientes: Set<String>) {
    val texto = problema ?: if (pendientes.isNotEmpty()) {
        "Cambios en ${pendientes.sorted().joinToString()} guardados en este teléfono, esperando internet para compartirse."
    } else null
    if (texto == null) return
    Surface(
        color = if (problema != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(texto, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall)
    }
}
