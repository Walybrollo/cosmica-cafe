package com.cosmica.cafeteria

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cosmica.cafeteria.data.Categorias
import com.cosmica.cafeteria.data.Cierre
import com.cosmica.cafeteria.data.Insumo
import com.cosmica.cafeteria.data.MovimientoStock
import com.cosmica.cafeteria.data.Gasto
import com.cosmica.cafeteria.data.Producto
import com.cosmica.cafeteria.data.Repositorio
import com.cosmica.cafeteria.data.ResumenProducto
import com.cosmica.cafeteria.data.Sesion
import com.cosmica.cafeteria.data.Usuario
import com.cosmica.cafeteria.data.Venta
import com.cosmica.cafeteria.data.esConsumo
import com.cosmica.cafeteria.data.subtotal
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

data class EstadoInsumo(
    val insumo: Insumo,
    val consumoPorDia: Double?,
    val diasQueAlcanza: Double?,
    val hayQueComprar: Boolean,
)

data class Balance(
    val ingresos: Double = 0.0,
    val costoMercaderia: Double = 0.0,
    val gastos: Double = 0.0,
    val cantidadVentas: Int = 0,
    val porMetodoPago: Map<String, Double> = emptyMap(),
    val porVendedor: Map<String, Double> = emptyMap(),
    val gastosPorCategoria: Map<String, Double> = emptyMap(),
    val productos: List<ResumenProducto> = emptyList(),
    /** Día del mes -> total vendido. */
    val porDia: Map<Int, Double> = emptyMap(),
    /** Hora del día -> cantidad de ventas. */
    val porHora: Map<Int, Int> = emptyMap(),
    val descuentos: Double = 0.0,
    /** Consumo del equipo a costo, y lo que sería a precio de venta. */
    val consumoEquipo: Double = 0.0,
    val consumoAPrecio: Double = 0.0,
    val consumosPorPersona: Map<String, Pair<Int, Double>> = emptyMap(),
    val consumoProductos: List<Pair<String, Int>> = emptyList(),
) {
    val gananciaBruta: Double get() = ingresos - costoMercaderia
    val gananciaNeta: Double get() = gananciaBruta - gastos - consumoEquipo
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

    val balance: StateFlow<Balance> = estado(combine(ventas, gastos) { v, g -> armarBalance(v, g) }, Balance())

    /**
     * Balance del mes anterior para comparar. Si se mira el mes en curso, se toma el mismo período
     * (del 1 al día de hoy) para que la comparación sea justa.
     */
    val balanceAnterior: StateFlow<Balance> = estado(
        mes.flatMapLatest { m ->
            val anterior = m.minusMonths(1)
            val hasta = if (m == YearMonth.now()) LocalDate.now().dayOfMonth else 31
            fun Long.dia() = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).dayOfMonth
            combine(
                conSesion { repo.ventasDelMes(anterior) },
                conSesion { repo.gastosDelMes(anterior) },
            ) { v, g -> armarBalance(v.filter { it.fecha.dia() <= hasta }, g.filter { it.fecha.dia() <= hasta }) }
        },
        Balance(),
    )

    /** true si se compara solo una parte del mes anterior (porque el mes actual no terminó). */
    val comparaPeriodoParcial: StateFlow<Boolean> = estado(mes.map { it == YearMonth.now() }, true)

    val cierres: StateFlow<List<Cierre>> =
        estado(mes.flatMapLatest { m -> conSesion { repo.cierresDelMes(m) } }, emptyList())

    /** Ventas de los últimos 28 días, sin importar el mes que se esté mirando. */
    private val recientes: StateFlow<List<Venta>> = estado(
        conSesion { repo.ventasDesde(System.currentTimeMillis() - 28L * 24 * 60 * 60 * 1000) },
        emptyList(),
    )

    /**
     * Los productos que más se venden a esta hora (una hora antes y después), según los últimos 28 días.
     * Si todavía hay pocas ventas a esta hora, se completa con lo más vendido de todo el día.
     */
    val masVendidos: StateFlow<List<Producto>> = estado(
        combine(menu, recientes) { m, v -> masVendidosAhora(m, v.filter { !it.esConsumo }, LocalTime.now().hour) },
        emptyList(),
    )

    /** La venta más reciente, para poder repetirla con un toque. */
    val ultimaVenta: StateFlow<Venta?> = estado(recientes.map { l -> l.firstOrNull { !it.esConsumo } }, null)

    /** Ventas de hoy, para el cierre de caja (no depende del mes que se esté mirando). */
    val ventasHoy: StateFlow<List<Venta>> = estado(
        recientes.map { l ->
            val inicio = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            l.filter { it.fecha >= inicio && !it.esConsumo }
        },
        emptyList(),
    )

    /** Cierre de hoy, si ya se hizo. */
    val cierreHoy: StateFlow<Cierre?> = estado(
        conSesion { repo.cierresDelMes(YearMonth.now()) }.map { l -> l.firstOrNull { it.dia == LocalDate.now().toString() } },
        null,
    )

    // ---- Stock ----

    val insumos: StateFlow<List<Insumo>> = estado(
        conSesion { repo.insumos() }.map { l -> l.sortedBy { it.nombre.lowercase() } },
        emptyList(),
    )

    private val movimientos: StateFlow<List<MovimientoStock>> = estado(
        conSesion { repo.movimientosDesde(System.currentTimeMillis() - DIAS_CONSUMO * DIA_MS) },
        emptyList(),
    )

    /** Cada insumo con cuánto se gasta por día y para cuántos días alcanza. */
    val stock: StateFlow<List<EstadoInsumo>> = estado(
        combine(insumos, movimientos) { ins, movs -> ins.map { estadoDe(it, movs.filter { m -> m.insumoId == it.id }) } },
        emptyList(),
    )

    fun guardarInsumo(i: Insumo) = repo.guardarInsumo(i)
    fun borrarInsumo(i: Insumo) = repo.borrarInsumo(i.id)
    fun comprarInsumo(i: Insumo, cantidad: Double) = repo.moverStock(i.id, cantidad, "compra")
    fun contarInsumo(i: Insumo, contado: Double) = repo.contarStock(i.id, i.cantidad, contado)

    /** Resta del stock lo vendido (o lo devuelve, con [signo] = 1) en los insumos ligados a productos. */
    private fun descontarStock(cantidades: Map<String, Int>, signo: Int = -1) {
        insumos.value.filter { it.productoId.isNotEmpty() }.forEach { i ->
            val c = cantidades[i.productoId] ?: 0
            if (c != 0) repo.moverStock(i.id, signo * c.toDouble(), if (signo < 0) "venta" else "ajuste")
        }
    }

    private fun Venta.cantidades(): Map<String, Int> =
        items.groupBy { it.productoId }.mapValues { (_, l) -> l.sumOf { it.cantidad } }

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

    /** Suma al carrito lo mismo que tenía [v] (solo los productos que siguen en el menú). */
    fun repetir(v: Venta) {
        val activos = menu.value.map { it.id }.toSet()
        carrito.update { actual ->
            v.items.filter { it.productoId in activos }.fold(actual) { c, i ->
                c + (i.productoId to (c[i.productoId] ?: 0) + i.cantidad)
            }
        }
    }

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
        descontarStock(lineas.mapKeys { it.key.id })
        vaciarCarrito()
        // Para el consumo del equipo se devuelve lo que costó.
        return lineas.entries.sumOf { (p, c) -> (if (metodoPago == Categorias.CONSUMO) p.costo else p.precio) * c }
    }

    fun borrarVenta(v: Venta) {
        repo.borrarVenta(v)
        descontarStock(v.cantidades(), signo = 1)
    }

    /** Guarda los cambios de una venta y ajusta el stock por la diferencia de productos. */
    fun editarVenta(original: Venta, editada: Venta) {
        repo.guardarVenta(editada)
        val antes = original.cantidades()
        val despues = editada.cantidades()
        val diferencia = (antes.keys + despues.keys).associateWith { (despues[it] ?: 0) - (antes[it] ?: 0) }
        descontarStock(diferencia)
    }

    fun cerrarCaja(fondoInicial: Double, contado: Double, nota: String) {
        val hoy = ventasHoy.value
        val efectivo = hoy.filter { it.metodoPago == Categorias.metodosPago.first() }.sumOf { it.total }
        repo.guardarCierre(
            Cierre(
                dia = LocalDate.now().toString(),
                fondoInicial = fondoInicial,
                ventasEfectivo = efectivo,
                ventasOtros = hoy.sumOf { it.total } - efectivo,
                contado = contado,
                cerradoPor = nombreUsuario,
                nota = nota.trim(),
                fecha = System.currentTimeMillis(),
            )
        )
    }

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

    fun guardarGasto(g: Gasto, compra: Pair<Insumo, Double>? = null) {
        repo.guardarGasto(if (g.id.isEmpty()) g.copy(cargadoPor = nombreUsuario) else g)
        compra?.let { (i, c) -> comprarInsumo(i, c) }
    }
    fun borrarGasto(g: Gasto) = repo.borrarGasto(g)

    companion object {
        private const val CANTIDAD_SUGERIDOS = 6
        private const val DIA_MS = 24L * 60 * 60 * 1000
        private const val DIAS_CONSUMO = 30L

        fun armarBalance(todas: List<Venta>, g: List<Gasto>): Balance {
            val zona = ZoneId.systemDefault()
            val v = todas.filter { !it.esConsumo }
            val consumos = todas.filter { it.esConsumo }
            fun Long.hora() = Instant.ofEpochMilli(this).atZone(zona)
            return Balance(
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
                porDia = v.groupBy { it.fecha.hora().dayOfMonth }.mapValues { (_, l) -> l.sumOf { it.total } },
                porHora = v.groupBy { it.fecha.hora().hour }.mapValues { (_, l) -> l.size },
                descuentos = v.sumOf { it.descuento },
                consumoEquipo = consumos.sumOf { it.costoTotal },
                consumoAPrecio = consumos.sumOf { it.subtotal },
                consumosPorPersona = consumos.groupBy { it.vendedor.ifBlank { "Sin nombre" } }
                    .mapValues { (_, l) -> l.size to l.sumOf { it.costoTotal } },
                consumoProductos = consumos.flatMap { it.items }.groupBy { it.nombreProducto }
                    .map { (n, l) -> n to l.sumOf { it.cantidad } }.sortedByDescending { it.second },
            )
        }

        /**
         * Consumo por día según los últimos 30 días: lo que bajó con ventas y conteos.
         * Hace falta al menos un día de datos para estimar.
         */
        fun estadoDe(i: Insumo, movs: List<MovimientoStock>, ahora: Long = System.currentTimeMillis()): EstadoInsumo {
            val usos = movs.filter { it.tipo == "venta" || it.tipo == "conteo" || it.tipo == "ajuste" }
            val consumo = -usos.sumOf { it.cambio }
            val desde = movs.minOfOrNull { it.fecha }
            val dias = desde?.let { (ahora - it).toDouble() / DIA_MS } ?: 0.0
            val porDia = if (dias >= 1.0 && consumo > 0) consumo / dias else null
            val alcanza = porDia?.let { (i.cantidad / it).coerceAtLeast(0.0) }
            return EstadoInsumo(
                insumo = i,
                consumoPorDia = porDia,
                diasQueAlcanza = alcanza,
                hayQueComprar = i.cantidad <= i.minimo || (alcanza != null && alcanza < 3),
            )
        }

        fun masVendidosAhora(menu: List<Producto>, ventas: List<Venta>, hora: Int): List<Producto> {
            val porId = menu.associateBy { it.id }
            val zona = ZoneId.systemDefault()
            fun ranking(l: List<Venta>) = l.flatMap { it.items }
                .groupBy { it.productoId }
                .mapValues { (_, items) -> items.sumOf { it.cantidad } }
                .entries.sortedByDescending { it.value }
                .mapNotNull { porId[it.key] }
            val aEstaHora = ventas.filter {
                val h = java.time.Instant.ofEpochMilli(it.fecha).atZone(zona).hour
                kotlin.math.abs(h - hora) <= 1
            }
            return (ranking(aEstaHora) + ranking(ventas)).distinct().take(CANTIDAD_SUGERIDOS)
        }

        fun factory(repo: Repositorio, sesion: Sesion) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = CafeViewModel(repo, sesion) as T
        }
    }
}
