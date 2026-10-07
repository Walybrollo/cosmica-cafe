package com.cosmica.cafeteria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.cosmica.cafeteria.ui.AppCafe
import com.cosmica.cafeteria.ui.theme.CafeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Íconos claros en la barra de estado, sobre el cielo negro del encabezado.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        val app = application as CafeApp
        if (!app.firebaseConfigurado) {
            setContent {
                CafeTheme {
                    Surface(Modifier.fillMaxSize()) {
                        Box(Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "Falta conectar la app con Firebase (archivo google-services.json).",
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
            return
        }
        val vm = ViewModelProvider(this, CafeViewModel.factory(app.repositorio, app.sesion))[CafeViewModel::class.java]
        setContent {
            CafeTheme {
                AppCafe(vm)
            }
        }
    }
}
