package com.cosmica.cafeteria

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cosmica.cafeteria.data.Categorias
import com.cosmica.cafeteria.data.Gasto
import com.cosmica.cafeteria.data.Producto
import com.cosmica.cafeteria.data.Repositorio
import com.cosmica.cafeteria.data.ResumenProducto
import com.cosmica.cafeteria.data.Sesion
import com.cosmica.cafeteria.data.Usuario
import com.cosmica.cafeteria.data.Venta
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

data class Balance(
    val ingresos: Double = 0.0,
    val costoMercaderia: Double = 0.0,
    val gastos: Double = 0.0,
    val cantidadVentas: Int = 0,
    val porMetodoPago: Map<String, Double> = emptyMap(),
    val porVendedor: Map<String, Double> = emptyMap(),
    val gastosPorCategoria: Map<String, Double> = emptyMap(),
    val productos: List<ResumenProducto> = emptyList(),
) {
    val gananciaBruta: Double get() = ingresos - costoMercaderia
    val gananciaNeta: Double get() = gananciaBruta - gastos
    val ticketPromedio: Double get() = if (cantidadVentas > 0) ingresos / cantidadVentas else 0.0
}

@OptIn(ExperimentalCoroutinesApi::class)
class CafeViewModel(private val repo: Repositorio, private val sesion: Sesion) : ViewModel() {

    private fun <T> estado(flow: Flow<T>, inicial: T): StateFlow<T> =
        flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), inicial)

    val usuario: StateFlow<Usuario?> = sesion.usuario

    /** Problema de conexión o permisos con la base de datos, para avisar en pantalla. */
    val problema: StateFlow<String?> = repo.problema
    /** Listas con cambios que todavía no subieron a internet. */
    val pendientes: StateFlow<Set<String>> = repo.pendientes

    /** Escucha los datos solo mientras hay alguien con sesión iniciada. */
    private fun <T> conSesion(datos: () -> Flow<List<T>>): Flow<List<T>> =
        usuario.flatMapLatest { if (it == null) flowOf(emptyList()) else datos() }

    val menu: StateFlow<List<Producto>> = estado(
        conSesion { repo.menu() }.map { l -> l.sortedWith(compareBy({ it.categoria }, { it.nombre.lowercase() })) },
        emptyList(),
    )

    /** Categorías que usan los productos del menú; si el menú está vacío, unas de ejemplo. */
    val categoriasMenu: StateFlow<List<String>> = estado(
        menu.map { l -> l.map { it.categoria }.distinct().sortedBy { it.lowercase() }.ifEmpty { Categorias.productos } },
        Categorias.productos,
    )

    /** Mes que se está mirando en Ventas, Gastos y Balance. */
    val mes = MutableStateFlow(YearMonth.now())

    val ventas: StateFlow<List<Venta>> =
        estado(mes.flatMapLatest { m -> conSesion { repo.ventasDelMes(m) } }, emptyList())

    val gastos: StateFlow<List<Gasto>> =
        estado(mes.flatMapLatest { m -> conSesion { repo.gastosDelMes(m) } }, emptyList())

    val balance: StateFlow<Balance> = estado(
        combine(ventas, gastos) { v, g ->
            Balance(
                ingresos = v.sumOf { it.total },
                costoMercaderia = v.sumOf { it.costoTotal },
                gastos = g.sumOf { it.monto },
                cantidadVentas = v.size,
                porMetodoPago = v.groupBy { it.metodoPago }.mapValues { (_, l) -> l.sumOf { it.total } },
                porVendedor = v.groupBy { it.vendedor.ifBlank { "Sin nombre" } }
                    .mapValues { (_, l) -> l.sumOf { it.total } },
                gastosPorCategoria = g.groupBy { it.categoria }.mapValues { (_, l) -> l.sumOf { it.monto } },
                productos = v.flatMap { it.items }.groupBy { it.nombreProducto }.map { (nombre, items) ->
                    ResumenProducto(
                        nombre = nombre,
                        cantidad = items.sumOf { it.cantidad },
                        ingresos = items.sumOf { it.cantidad * it.precioUnitario },
                        ganancia = items.sumOf { it.cantidad * (it.precioUnitario - it.costoUnitario) },
                    )
                }.sortedByDescending { it.cantidad },
            )
        },
        Balance(),
    )

    /** Carrito de la venta en curso: id de producto -> cantidad. */
    val carrito = MutableStateFlow<Map<String, Int>>(emptyMap())

    private val nombreUsuario: String get() = usuario.value?.nombre ?: ""

    suspend fun entrar(email: String, clave: String): String? = sesion.entrar(email, clave)

    fun salir() {
        vaciarCarrito()
        sesion.salir()
    }

    fun cambiarMes(delta: Long) = mes.update { it.plusMonths(delta) }

    fun sumarAlCarrito(id: String, delta: Int) = carrito.update { actual ->
        val nueva = (actual[id] ?: 0) + delta
        if (nueva <= 0) actual - id else actual + (id to nueva)
    }

    fun vaciarCarrito() = carrito.update { emptyMap() }

    /**
     * Devuelve el total cobrado, o null si el carrito estaba vacío.
     * [dia] permite cargar una venta olvidada de otro día; se guarda con la hora actual.
     */
    fun cobrar(metodoPago: String, dia: LocalDate? = null): Double? {
        val porId = menu.value.associateBy { it.id }
        val lineas = carrito.value.mapNotNull { (id, c) -> porId[id]?.let { it to c } }.toMap()
        if (lineas.isEmpty()) return null
        val fecha = if (dia == null || dia == LocalDate.now()) System.currentTimeMillis()
        else dia.atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        repo.registrarVenta(lineas, metodoPago, nombreUsuario, fecha)
        vaciarCarrito()
        return lineas.entries.sumOf { (p, c) -> p.precio * c }
    }

    fun borrarVenta(v: Venta) = repo.borrarVenta(v)

    fun guardarProducto(p: Producto) = repo.guardarProducto(p)
    fun renombrarCategoria(vieja: String, nueva: String) {
        val limpia = nueva.trim()
        if (limpia.isEmpty() || limpia == vieja) return
        repo.renombrarCategoria(menu.value.filter { it.categoria == vieja }, limpia)
    }

    fun quitarProducto(p: Producto) {
        repo.quitarProducto(p.id)
        carrito.update { it - p.id }
    }

    fun guardarGasto(g: Gasto) = repo.guardarGasto(if (g.id.isEmpty()) g.copy(cargadoPor = nombreUsuario) else g)
    fun borrarGasto(g: Gasto) = repo.borrarGasto(g)

    companion object {
        fun factory(repo: Repositorio, sesion: Sesion) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = CafeViewModel(repo, sesion) as T
        }
    }
}
