package com.cosmica.cafeteria.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

    private fun <T> escuchar(query: Query, convertir: (DocumentSnapshot) -> T?): Flow<List<T>> = callbackFlow {
        val registro = query.addSnapshotListener { snap, error ->
            if (error != null) {
                // Sin permiso (por ejemplo, después de cerrar sesión): mostramos vacío en vez de cerrar la app.
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snap != null) trySend(snap.documents.mapNotNull(convertir))
        }
        awaitClose { registro.remove() }
    }

    fun menu(): Flow<List<Producto>> = escuchar(productos.whereEqualTo("activo", true), ::aProducto)

    fun ventasDelMes(mes: YearMonth): Flow<List<Venta>> {
        val (d, h) = mes.rango()
        return escuchar(
            ventas.whereGreaterThanOrEqualTo("fecha", d).whereLessThanOrEqualTo("fecha", h)
                .orderBy("fecha", Query.Direction.DESCENDING),
            ::aVenta,
        )
    }

    fun gastosDelMes(mes: YearMonth): Flow<List<Gasto>> {
        val (d, h) = mes.rango()
        return escuchar(
            gastos.whereGreaterThanOrEqualTo("fecha", d).whereLessThanOrEqualTo("fecha", h)
                .orderBy("fecha", Query.Direction.DESCENDING),
            ::aGasto,
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

    fun quitarProducto(id: String) {
        productos.document(id).update("activo", false)
    }

    fun registrarVenta(carrito: Map<Producto, Int>, metodoPago: String, vendedor: String) {
        val lineas = carrito.filterValues { it > 0 }
        if (lineas.isEmpty()) return
        ventas.document().set(
            mapOf(
                "fecha" to System.currentTimeMillis(),
                "total" to lineas.entries.sumOf { (p, c) -> p.precio * c },
                "costoTotal" to lineas.entries.sumOf { (p, c) -> p.costo * c },
                "metodoPago" to metodoPago,
                "vendedor" to vendedor,
                "items" to lineas.map { (p, c) ->
                    mapOf(
                        "productoId" to p.id,
                        "nombreProducto" to p.nombre,
                        "cantidad" to c,
                        "precioUnitario" to p.precio,
                        "costoUnitario" to p.costo,
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
