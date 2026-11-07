package com.cosmica.cafeteria.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import java.time.YearMonth
import java.time.ZoneId

/** Rango [desde, hasta] en milisegundos de un mes completo en la zona horaria del teléfono. */
fun YearMonth.rango(): Pair<Long, Long> {
    val zona = ZoneId.systemDefault()
    val desde = atDay(1).atStartOfDay(zona).toInstant().toEpochMilli()
    val hasta = plusMonths(1).atDay(1).atStartOfDay(zona).toInstant().toEpochMilli() - 1
    return desde to hasta
}

/**
 * Datos compartidos en Firestore. Firestore guarda una copia en el teléfono, así que si se corta
 * internet se puede seguir vendiendo y todo se sincroniza al volver la conexión.
 * Por eso las escrituras no esperan respuesta del servidor.
 */
class Repositorio(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    private val productos = db.collection("productos")
    private val ventas = db.collection("ventas")
    private val gastos = db.collection("gastos")
    private val cierres = db.collection("cierres")
    private val insumos = db.collection("insumos")
    private val movimientos = db.collection("movimientos")

    private val _problema = MutableStateFlow<String?>(null)
    /** Motivo por el que no se pueden leer los datos de internet, o null si anda bien. */
    val problema: StateFlow<String?> = _problema

    private val consultasConPendientes = MutableStateFlow<Set<String>>(emptySet())
    /** Nombres de las listas que tienen cambios guardados en el teléfono que todavía no subieron. */
    val pendientes: StateFlow<Set<String>> = consultasConPendientes

    private fun <T> escuchar(nombre: String, query: Query, convertir: (DocumentSnapshot) -> T?): Flow<List<T>> = callbackFlow {
        val registro = query.addSnapshotListener(MetadataChanges.INCLUDE) { snap, error ->
            if (error != null) {
                _problema.value = when (error.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                        "Sin permiso para ver los datos. Revisá que tu correo esté en las reglas de Firestore."
                    else -> "No se pudo conectar con la base de datos (${error.code})."
                }
                // Mostramos vacío en vez de cerrar la app.
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snap != null) {
                if (!snap.metadata.isFromCache) _problema.value = null
                consultasConPendientes.update {
                    if (snap.metadata.hasPendingWrites()) it + nombre else it - nombre
                }
                trySend(snap.documents.mapNotNull(convertir))
            }
        }
        awaitClose {
            registro.remove()
            consultasConPendientes.update { it - nombre }
        }
    }

    fun menu(): Flow<List<Producto>> = escuchar("menú", productos.whereEqualTo("activo", true), ::aProducto)

    fun ventasDelMes(mes: YearMonth): Flow<List<Venta>> {
        val (d, h) = mes.rango()
        return escuchar(
            "ventas",
            ventas.whereGreaterThanOrEqualTo("fecha", d).whereLessThanOrEqualTo("fecha", h)
                .orderBy("fecha", Query.Direction.DESCENDING),
            ::aVenta,
        )
    }

    /** Ventas desde una fecha hasta hoy (para sugerir lo más vendido y repetir la última). */
    fun ventasDesde(desde: Long): Flow<List<Venta>> = escuchar(
        "ventas",
        ventas.whereGreaterThanOrEqualTo("fecha", desde).orderBy("fecha", Query.Direction.DESCENDING),
        ::aVenta,
    )

    fun gastosDelMes(mes: YearMonth): Flow<List<Gasto>> {
        val (d, h) = mes.rango()
        return escuchar(
            "gastos",
            gastos.whereGreaterThanOrEqualTo("fecha", d).whereLessThanOrEqualTo("fecha", h)
                .orderBy("fecha", Query.Direction.DESCENDING),
            ::aGasto,
        )
    }

    /** Cierres de caja de un mes, por día ("2026-11-01" a "2026-11-31"). */
    fun cierresDelMes(mes: YearMonth): Flow<List<Cierre>> = escuchar(
        "cierres",
        cierres.whereGreaterThanOrEqualTo("dia", mes.atDay(1).toString())
            .whereLessThanOrEqualTo("dia", mes.atEndOfMonth().toString())
            .orderBy("dia", Query.Direction.DESCENDING),
        ::aCierre,
    )

    fun guardarCierre(c: Cierre) {
        cierres.document(c.dia).set(
            mapOf(
                "dia" to c.dia,
                "fondoInicial" to c.fondoInicial,
                "ventasEfectivo" to c.ventasEfectivo,
                "ventasOtros" to c.ventasOtros,
                "contado" to c.contado,
                "cerradoPor" to c.cerradoPor,
                "nota" to c.nota,
                "fecha" to c.fecha,
            )
        )
    }

    fun insumos(): Flow<List<Insumo>> = escuchar("stock", insumos, ::aInsumo)

    fun movimientosDesde(desde: Long): Flow<List<MovimientoStock>> = escuchar(
        "stock",
        movimientos.whereGreaterThanOrEqualTo("fecha", desde),
        ::aMovimiento,
    )

    fun guardarInsumo(i: Insumo) {
        val datos = mapOf(
            "nombre" to i.nombre,
            "unidad" to i.unidad,
            "minimo" to i.minimo,
            "productoId" to i.productoId,
        )
        // Al editar no se toca la cantidad, que la cambian las ventas, compras y conteos.
        if (i.id.isEmpty()) insumos.document().set(datos + ("cantidad" to i.cantidad))
        else insumos.document(i.id).update(datos)
    }

    fun borrarInsumo(id: String) {
        insumos.document(id).delete()
    }

    /** Suma (o resta) [cambio] al stock del insumo y anota el movimiento. */
    fun moverStock(insumoId: String, cambio: Double, tipo: String) {
        if (cambio == 0.0) return
        insumos.document(insumoId).update("cantidad", FieldValue.increment(cambio))
        anotarMovimiento(insumoId, cambio, tipo)
    }

    /** Deja el stock en lo que se contó y anota la diferencia como consumo. */
    fun contarStock(insumoId: String, anterior: Double, contado: Double) {
        insumos.document(insumoId).update("cantidad", contado)
        anotarMovimiento(insumoId, contado - anterior, "conteo")
    }

    private fun anotarMovimiento(insumoId: String, cambio: Double, tipo: String) {
        movimientos.document().set(
            mapOf("insumoId" to insumoId, "fecha" to System.currentTimeMillis(), "tipo" to tipo, "cambio" to cambio)
        )
    }

    fun guardarProducto(p: Producto) {
        val doc = if (p.id.isEmpty()) productos.document() else productos.document(p.id)
        doc.set(
            mapOf(
                "nombre" to p.nombre,
                "categoria" to p.categoria,
                "precio" to p.precio,
                "costo" to p.costo,
                "activo" to p.activo,
            )
        )
    }

    /** Cambia la categoría de todos los productos que la usan (también sirve para juntar dos categorías). */
    fun renombrarCategoria(productosDeLaCategoria: List<Producto>, nueva: String) {
        productosDeLaCategoria.forEach { productos.document(it.id).update("categoria", nueva) }
    }

    fun quitarProducto(id: String) {
        productos.document(id).update("activo", false)
    }

    fun registrarVenta(carrito: Map<Producto, Int>, metodoPago: String, vendedor: String, fecha: Long) {
        val items = carrito.filterValues { it > 0 }.map { (p, c) ->
            VentaItem(p.id, p.nombre, c, p.precio, p.costo)
        }
        if (items.isEmpty()) return
        guardarVenta(Venta(fecha = fecha, total = 0.0, costoTotal = 0.0, metodoPago = metodoPago, vendedor = vendedor, items = items))
    }

    /** Guarda una venta nueva o editada; el total y el costo se recalculan con los productos y el descuento. */
    fun guardarVenta(v: Venta) {
        val items = v.items.filter { it.cantidad > 0 }
        val subtotal = items.sumOf { it.cantidad * it.precioUnitario }
        val descuento = v.descuento.coerceIn(0.0, subtotal)
        val doc = if (v.id.isEmpty()) ventas.document() else ventas.document(v.id)
        doc.set(
            mapOf(
                "fecha" to v.fecha,
                "total" to subtotal - descuento,
                "descuento" to descuento,
                "costoTotal" to items.sumOf { it.cantidad * it.costoUnitario },
                "metodoPago" to v.metodoPago,
                "vendedor" to v.vendedor,
                "items" to items.map {
                    mapOf(
                        "productoId" to it.productoId,
                        "nombreProducto" to it.nombreProducto,
                        "cantidad" to it.cantidad,
                        "precioUnitario" to it.precioUnitario,
                        "costoUnitario" to it.costoUnitario,
                    )
                },
            )
        )
    }

    fun borrarVenta(v: Venta) {
        ventas.document(v.id).delete()
    }

    fun guardarGasto(g: Gasto) {
        val doc = if (g.id.isEmpty()) gastos.document() else gastos.document(g.id)
        doc.set(
            mapOf(
                "fecha" to g.fecha,
                "descripcion" to g.descripcion,
                "categoria" to g.categoria,
                "monto" to g.monto,
                "cargadoPor" to g.cargadoPor,
            )
        )
    }

    fun borrarGasto(g: Gasto) {
        gastos.document(g.id).delete()
    }

    private fun aProducto(d: DocumentSnapshot): Producto? = d.getString("nombre")?.let { nombre ->
        Producto(
            id = d.id,
            nombre = nombre,
            categoria = d.getString("categoria") ?: "Otros",
            precio = d.getDouble("precio") ?: 0.0,
            costo = d.getDouble("costo") ?: 0.0,
            activo = d.getBoolean("activo") ?: true,
        )
    }

    private fun aVenta(d: DocumentSnapshot): Venta? {
        val items = (d.get("items") as? List<*>).orEmpty().mapNotNull { x ->
            val m = x as? Map<*, *> ?: return@mapNotNull null
            VentaItem(
                productoId = m["productoId"] as? String ?: "",
                nombreProducto = m["nombreProducto"] as? String ?: "?",
                cantidad = (m["cantidad"] as? Number)?.toInt() ?: 0,
                precioUnitario = (m["precioUnitario"] as? Number)?.toDouble() ?: 0.0,
                costoUnitario = (m["costoUnitario"] as? Number)?.toDouble() ?: 0.0,
            )
        }
        return Venta(
            id = d.id,
            fecha = d.getLong("fecha") ?: return null,
            total = d.getDouble("total") ?: 0.0,
            costoTotal = d.getDouble("costoTotal") ?: 0.0,
            metodoPago = d.getString("metodoPago") ?: "",
            vendedor = d.getString("vendedor") ?: "",
            items = items,
            descuento = d.getDouble("descuento") ?: 0.0,
        )
    }

    private fun aCierre(d: DocumentSnapshot): Cierre? = d.getString("dia")?.let { dia ->
        Cierre(
            dia = dia,
            fondoInicial = d.getDouble("fondoInicial") ?: 0.0,
            ventasEfectivo = d.getDouble("ventasEfectivo") ?: 0.0,
            ventasOtros = d.getDouble("ventasOtros") ?: 0.0,
            contado = d.getDouble("contado") ?: 0.0,
            cerradoPor = d.getString("cerradoPor") ?: "",
            nota = d.getString("nota") ?: "",
            fecha = d.getLong("fecha") ?: 0,
        )
    }

    private fun aInsumo(d: DocumentSnapshot): Insumo? = d.getString("nombre")?.let { nombre ->
        Insumo(
            id = d.id,
            nombre = nombre,
            unidad = d.getString("unidad") ?: "unidades",
            cantidad = d.getDouble("cantidad") ?: 0.0,
            minimo = d.getDouble("minimo") ?: 0.0,
            productoId = d.getString("productoId") ?: "",
        )
    }

    private fun aMovimiento(d: DocumentSnapshot): MovimientoStock? {
        val insumoId = d.getString("insumoId") ?: return null
        return MovimientoStock(
            insumoId = insumoId,
            fecha = d.getLong("fecha") ?: return null,
            tipo = d.getString("tipo") ?: "",
            cambio = d.getDouble("cambio") ?: 0.0,
        )
    }

    private fun aGasto(d: DocumentSnapshot): Gasto? = d.getLong("fecha")?.let { fecha ->
        Gasto(
            id = d.id,
            fecha = fecha,
            descripcion = d.getString("descripcion") ?: "",
            categoria = d.getString("categoria") ?: "Otros",
            monto = d.getDouble("monto") ?: 0.0,
            cargadoPor = d.getString("cargadoPor") ?: "",
        )
    }
}
