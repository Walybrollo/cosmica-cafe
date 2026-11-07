package com.cosmica.cafeteria.data

/** Un producto del menú. Si se quita del menú queda inactivo para no perder el historial. */
data class Producto(
    val id: String = "",
    val nombre: String,
    val categoria: String,
    val precio: Double,
    val costo: Double,
    val activo: Boolean = true,
)

val Producto.ganancia: Double get() = precio - costo
val Producto.margen: Double get() = if (precio > 0) ganancia / precio else 0.0

/** Línea de una venta. Guarda nombre, precio y costo del momento, así el balance no cambia si después se edita el menú. */
data class VentaItem(
    val productoId: String,
    val nombreProducto: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val costoUnitario: Double,
)

data class Venta(
    val id: String = "",
    val fecha: Long,
    val total: Double,
    val costoTotal: Double,
    val metodoPago: String,
    val vendedor: String,
    val items: List<VentaItem>,
    /** Plata descontada del subtotal; el total ya la tiene restada. */
    val descuento: Double = 0.0,
)

val Venta.subtotal: Double get() = items.sumOf { it.cantidad * it.precioUnitario }
val Venta.esConsumo: Boolean get() = metodoPago == Categorias.CONSUMO

/** Cierre de caja de un día. El id es la fecha (2026-11-07), así hay uno solo por día. */
data class Cierre(
    val dia: String,
    val fondoInicial: Double,
    val ventasEfectivo: Double,
    val ventasOtros: Double,
    val contado: Double,
    val cerradoPor: String,
    val nota: String = "",
    val fecha: Long = 0,
) {
    val esperado: Double get() = fondoInicial + ventasEfectivo
    val diferencia: Double get() = contado - esperado
}

/**
 * Un insumo con stock. Si tiene [productoId], se descuenta solo cada vez que se vende ese producto
 * (alfajores, galletas). Si no, se actualiza con compras y conteos (café, leche, vasos).
 */
data class Insumo(
    val id: String = "",
    val nombre: String,
    val unidad: String,
    val cantidad: Double,
    val minimo: Double,
    val productoId: String = "",
)

/** Cambio de stock: compra (+), conteo (lo que había menos lo contado), venta (-) o ajuste. */
data class MovimientoStock(
    val insumoId: String,
    val fecha: Long,
    val tipo: String,
    val cambio: Double,
)

data class Gasto(
    val id: String = "",
    val fecha: Long,
    val descripcion: String,
    val categoria: String,
    val monto: Double,
    val cargadoPor: String = "",
)

data class ResumenProducto(
    val nombre: String,
    val cantidad: Int,
    val ingresos: Double,
    val ganancia: Double,
)

object Categorias {
    val productos = listOf("Cafés", "Bebidas", "Comidas", "Dulces", "Otros")
    val gastos = listOf("Insumos", "Alquiler", "Servicios", "Sueldos", "Impuestos", "Otros")
    val metodosPago = listOf("Efectivo", "Tarjeta", "Transferencia")
    /** Lo que consume el equipo (desayunos, meriendas): no es venta, se cuenta a costo. */
    const val CONSUMO = "Consumo del equipo"
    val metodosCobro = metodosPago + CONSUMO
    val unidades = listOf("unidades", "kg", "litros", "paquetes")
}
