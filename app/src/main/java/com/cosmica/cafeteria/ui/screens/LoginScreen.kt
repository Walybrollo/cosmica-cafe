package com.cosmica.cafeteria.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.cosmica.cafeteria.CafeViewModel
import com.cosmica.cafeteria.R
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(vm: CafeViewModel) {
    var email by rememberSaveable { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var entrando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun entrar() {
        if (email.isBlank() || clave.isBlank() || entrando) return
        entrando = true
        error = null
        scope.launch {
            error = vm.entrar(email, clave)
            entrando = false
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Cósmica",
            modifier = Modifier.size(220.dp).clip(RoundedCornerShape(24.dp)).background(Color.White),
        )
        Text("Ingresá con tu cuenta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = clave,
            onValueChange = { clave = it },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { entrar() }),
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { entrar() }, enabled = !entrando, modifier = Modifier.fillMaxWidth()) {
            if (entrando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Entrar")
        }
    }
}
