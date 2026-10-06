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
}
