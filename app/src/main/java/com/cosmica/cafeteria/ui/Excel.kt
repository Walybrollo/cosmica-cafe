package com.cosmica.cafeteria.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.cosmica.cafeteria.Balance
import com.cosmica.cafeteria.data.Cierre
import com.cosmica.cafeteria.data.Gasto
import com.cosmica.cafeteria.data.Venta
import com.cosmica.cafeteria.data.subtotal
import java.io.File
import java.io.OutputStream
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Una hoja de la planilla: nombre y filas (la primera es el encabezado). */
class Hoja(val nombre: String, val filas: List<List<Any?>>)

/**
 * Arma un archivo .xlsx sencillo (sin librerías) que abre Excel, Google Sheets y LibreOffice.
 * Los números quedan como números, así se pueden sumar en la planilla.
 */
object Excel {
    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun columna(i: Int): String {
        var n = i + 1
        val sb = StringBuilder()
        while (n > 0) {
            val r = (n - 1) % 26
            sb.insert(0, ('A' + r))
            n = (n - 1) / 26
        }
        return sb.toString()
    }

    private fun hojaXml(h: Hoja): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        val anchos = h.filas.firstOrNull()?.indices?.map { c ->
            (h.filas.maxOfOrNull { (it.getOrNull(c)?.toString()?.length ?: 0) } ?: 10).coerceIn(8, 60) + 2
        }.orEmpty()
        if (anchos.isNotEmpty()) {
            sb.append("<cols>")
            anchos.forEachIndexed { i, w -> sb.append("""<col min="${i + 1}" max="${i + 1}" width="$w" customWidth="1"/>""") }
            sb.append("</cols>")
        }
        sb.append("<sheetData>")
        h.filas.forEachIndexed { f, fila ->
            sb.append("""<row r="${f + 1}">""")
            fila.forEachIndexed { c, v ->
                val ref = "${columna(c)}${f + 1}"
                val estilo = if (f == 0) " s=\"1\"" else ""
                when (v) {
                    null -> {}
                    is Number -> sb.append("""<c r="$ref"$estilo><v>${v.toDouble()}</v></c>""")
                    else -> sb.append("""<c r="$ref" t="inlineStr"$estilo><is><t xml:space="preserve">${esc(v.toString())}</t></is></c>""")
                }
            }
            sb.append("</row>")
        }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    fun escribir(hojas: List<Hoja>, salida: OutputStream) {
        ZipOutputStream(salida).use { zip ->
            fun archivo(nombre: String, contenido: String) {
                zip.putNextEntry(ZipEntry(nombre))
                zip.write(contenido.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            archivo(
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
                    """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""" +
                    """<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""" +
                    """<Default Extension="xml" ContentType="application/xml"/>""" +
                    """<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""" +
                    """<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""" +
                    hojas.indices.joinToString("") {
                        """<Override PartName="/xl/worksheets/sheet${it + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>"""
                    } + "</Types>",
            )
            archivo(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
                    """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
                    """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>""" +
                    "</Relationships>",
            )
            archivo(
                "xl/workbook.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
                    """<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>""" +
                    hojas.mapIndexed { i, h -> """<sheet name="${esc(h.nombre.take(31))}" sheetId="${i + 1}" r:id="rId${i + 1}"/>""" }.joinToString("") +
                    "</sheets></workbook>",
            )
            archivo(
                "xl/_rels/workbook.xml.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
                    """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
                    hojas.indices.joinToString("") {
                        """<Relationship Id="rId${it + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${it + 1}.xml"/>"""
                    } +
                    """<Relationship Id="rId${hojas.size + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""" +
                    "</Relationships>",
            )
            archivo(
                "xl/styles.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
                    """<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""" +
                    """<fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>""" +
                    """<fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>""" +
                    """<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>""" +
                    """<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""" +
                    """<cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/></cellXfs>""" +
                    """<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>""" +
                    "</styleSheet>",
            )
            hojas.forEachIndexed { i, h -> archivo("xl/worksheets/sheet${i + 1}.xml", hojaXml(h)) }
        }
    }

    private val fecha = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val hora = DateTimeFormatter.ofPattern("HH:mm")

    /** Planilla de un mes: resumen, ventas, productos, gastos y cierres de caja. */
    fun hojasDelMes(mes: YearMonth, b: Balance, ventas: List<Venta>, gastos: List<Gasto>, cierres: List<Cierre>): List<Hoja> {
        val zona = ZoneId.systemDefault()
        fun Long.f() = Instant.ofEpochMilli(this).atZone(zona)
        val resumen = buildList<List<Any?>> {
            add(listOf("Concepto", "Monto"))
            add(listOf("Mes", Formato.mes(mes)))
            add(listOf("Ventas (${b.cantidadVentas})", b.ingresos))
            if (b.descuentos > 0) add(listOf("Descuentos dados", b.descuentos))
            add(listOf("Costo de lo vendido", b.costoMercaderia))
            add(listOf("Ganancia bruta", b.gananciaBruta))
            add(listOf("Gastos", b.gastos))
            add(listOf("Ganancia neta", b.gananciaNeta))
            add(listOf("Ticket promedio", b.ticketPromedio))
            b.porMetodoPago.forEach { (m, v) -> add(listOf("Cobrado en $m", v)) }
        }
        val hojaVentas = listOf(listOf("Fecha", "Hora", "Productos", "Medio de pago", "Vendió", "Subtotal", "Descuento", "Total", "Costo", "Ganancia")) +
            ventas.sortedBy { it.fecha }.map { v ->
                listOf(
                    v.fecha.f().format(fecha), v.fecha.f().format(hora),
                    v.items.joinToString(", ") { "${it.cantidad} ${it.nombreProducto}" },
                    v.metodoPago, v.vendedor, v.subtotal, v.descuento, v.total, v.costoTotal, v.total - v.costoTotal,
                )
            }
        val hojaProductos = listOf(listOf("Producto", "Cantidad", "Vendido", "Ganancia")) +
            b.productos.map { listOf(it.nombre, it.cantidad, it.ingresos, it.ganancia) }
        val hojaGastos = listOf(listOf("Fecha", "Descripción", "Categoría", "Monto", "Cargó")) +
            gastos.sortedBy { it.fecha }.map { listOf(it.fecha.f().format(fecha), it.descripcion, it.categoria, it.monto, it.cargadoPor) }
        val hojaCierres = listOf(listOf("Día", "Fondo inicial", "Ventas en efectivo", "Otros medios", "Debía haber", "Contado", "Diferencia", "Cerró", "Nota")) +
            cierres.sortedBy { it.dia }.map {
                listOf(it.dia, it.fondoInicial, it.ventasEfectivo, it.ventasOtros, it.esperado, it.contado, it.diferencia, it.cerradoPor, it.nota)
            }
        return listOf(
            Hoja("Resumen", resumen),
            Hoja("Ventas", hojaVentas),
            Hoja("Productos", hojaProductos),
            Hoja("Gastos", hojaGastos),
            Hoja("Cierres de caja", hojaCierres),
        )
    }

    /** Guarda la planilla y abre el menú de Android para compartirla (WhatsApp, mail, Drive...). */
    fun compartir(context: Context, nombreArchivo: String, hojas: List<Hoja>) {
        val carpeta = File(context.cacheDir, "planillas").apply { mkdirs() }
        val archivo = File(carpeta, nombreArchivo)
        archivo.outputStream().use { escribir(hojas, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", archivo)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, nombreArchivo.removeSuffix(".xlsx"))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Enviar planilla"))
    }
}
