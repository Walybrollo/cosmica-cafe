package com.cosmica.cafeteria.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmica.cafeteria.CafeViewModel
import com.cosmica.cafeteria.data.Cliente
import com.cosmica.cafeteria.data.Pasaporte
import com.cosmica.cafeteria.ui.Formato
import com.cosmica.cafeteria.ui.theme.Ambar
import com.cosmica.cafeteria.ui.theme.Borde
import com.cosmica.cafeteria.ui.theme.SobreAmbar
import com.cosmica.cafeteria.ui.theme.Tarjeta2
import com.cosmica.cafeteria.ui.theme.TextoSuave
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch

/** Colores de los planetas del pasaporte (los mismos que ve el cliente). */
private val Planetas = listOf(
    Color(0xFFF2B35B), Color(0xFF8EC5FF), Color(0xFFFF8A7A),
    Color(0xFFB9A6FF), Color(0xFF6FD08C), Color(0xFFF5A3C7),
)

/**
 * Parte de la hoja de cobro para sellar el pasaporte de un cliente: se escanea su QR (o se escribe
 * su código), se eligen cuántos cafés sellar y, si ya juntó 6, se puede canjear el café gratis.
 */
@Composable
fun SeccionPasaporte(
    vm: CafeViewModel,
    cliente: Cliente?,
    sellos: Int,
    canje: Boolean,
    precioGratis: Double,
    alElegirCliente: (Cliente) -> Unit,
    alQuitarCliente: () -> Unit,
    alCambiarSellos: (Int) -> Unit,
    alCanjear: (Boolean) -> Unit,
) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    var codigo by remember { mutableStateOf("") }
    var buscando by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<String?>(null) }

    fun buscar(texto: String) {
        if (texto.isBlank() || buscando) return
        buscando = true
        aviso = null
        alcance.launch {
            val encontrado = try {
                vm.buscarCliente(texto)
            } catch (e: Exception) {
                aviso = "No se pudo buscar. Revisá la conexión a internet."
                null
            }
            buscando = false
            if (encontrado != null) {
                codigo = ""
                alElegirCliente(encontrado)
            } else if (aviso == null) {
                aviso = "No encontramos ese pasaporte."
            }
        }
    }

    fun escanear() {
        aviso = null
        val opciones = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(contexto, opciones).startScan()
            .addOnSuccessListener { b -> b.rawValue?.let { buscar(it) } }
            .addOnFailureListener { aviso = "No se pudo abrir el escáner. Escribí el código del cliente." }
    }

    Text(
        "Pasaporte de sellos",
        color = TextoSuave,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
    )

    if (cliente == null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { escanear() },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Ambar.copy(alpha = 0.6f)),
            ) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = Ambar)
                Spacer(Modifier.width(6.dp))
                Text("Escanear", color = Ambar, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = codigo,
                onValueChange = { codigo = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(6) },
                placeholder = { Text("Código", color = TextoSuave) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { buscar(codigo) }),
                trailingIcon = {
                    if (buscando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else IconButton(onClick = { buscar(codigo) }, enabled = codigo.length == 6) {
                        Icon(Icons.Filled.Search, contentDescription = "Buscar pasaporte")
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Borde, focusedBorderColor = Ambar),
                modifier = Modifier.weight(1f),
            )
        }
        aviso?.let { Text(it, color = Ambar, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }
        return
    }

    val meta = Pasaporte.SELLOS_PARA_PREMIO
    val tienePremio = cliente.sellos >= meta
    val quedan = cliente.sellos + sellos - if (canje) meta else 0

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Tarjeta2,
        border = BorderStroke(1.dp, if (tienePremio) Ambar.copy(alpha = 0.7f) else Borde),
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Ambar, modifier = Modifier.size(18.dp))
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(cliente.nombre.ifBlank { "Cliente ${cliente.codigo}" }, fontWeight = FontWeight.Bold)
                    Text(
                        "${minOf(cliente.sellos, meta)} de $meta sellos" + if (cliente.sellos > meta) " (+${cliente.sellos - meta})" else "",
                        color = TextoSuave,
                        fontSize = 12.sp,
                    )
                }
                IconButton(onClick = alQuitarCliente) { Icon(Icons.Filled.Close, contentDescription = "Quitar cliente") }
            }
            Planetitas(cliente.sellos, Modifier.padding(top = 6.dp, bottom = 4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Cafés a sellar", Modifier.weight(1f))
                IconButton(onClick = { alCambiarSellos(sellos - 1) }, enabled = sellos > 0) {
                    Icon(Icons.Filled.Remove, contentDescription = "Un sello menos")
                }
                Text("$sellos", Modifier.width(22.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                IconButton(onClick = { alCambiarSellos(sellos + 1) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Un sello más")
                }
            }
            if (tienePremio) {
                FilterChip(
                    selected = canje,
                    onClick = { alCanjear(!canje) },
                    label = { Text("Canjear café gratis (−${Formato.dinero(precioGratis)})", fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(50),
                    border = null,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Ambar.copy(alpha = 0.14f),
                        labelColor = Ambar,
                        selectedContainerColor = Ambar,
                        selectedLabelColor = SobreAmbar,
                    ),
                )
            }
            Text(
                if (quedan >= meta && !canje) "Queda con $quedan sellos: ya tiene un café gratis para canjear."
                else "Queda con $quedan de $meta sellos.",
                color = TextoSuave,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, end = 8.dp),
            )
        }
    }
}

/** Fila de 6 planetas: los sellados con color, los que faltan apagados. */
@Composable
private fun Planetitas(sellos: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Planetas.forEachIndexed { i, color ->
            val lleno = i < sellos
            Surface(
                shape = CircleShape,
                color = if (lleno) color else Color.Transparent,
                border = if (lleno) null else BorderStroke(1.dp, Borde),
                modifier = Modifier.size(20.dp).clip(CircleShape),
            ) {}
        }
    }
}
