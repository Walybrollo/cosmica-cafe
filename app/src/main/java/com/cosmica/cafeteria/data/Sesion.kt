package com.cosmica.cafeteria.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

/** Usuario que inició sesión. El nombre es lo que va antes de la @ del correo. */
data class Usuario(val email: String) {
    val nombre: String get() = email.substringBefore('@').replaceFirstChar { it.uppercase() }
}

class Sesion(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {
    private val _usuario = MutableStateFlow(auth.currentUser?.email?.let { Usuario(it) })
    val usuario: StateFlow<Usuario?> = _usuario

    init {
        auth.addAuthStateListener { a -> _usuario.value = a.currentUser?.email?.let { Usuario(it) } }
    }

    /** Devuelve null si entró bien, o el motivo del error en castellano. */
    suspend fun entrar(email: String, clave: String): String? = try {
        auth.signInWithEmailAndPassword(email.trim(), clave).await()
        null
    } catch (e: FirebaseAuthInvalidUserException) {
        "No existe un usuario con ese correo."
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        "Correo o contraseña incorrectos."
    } catch (e: FirebaseNetworkException) {
        "Sin conexión a internet. Para entrar la primera vez hace falta internet."
    } catch (e: FirebaseTooManyRequestsException) {
        "Demasiados intentos. Esperá unos minutos."
    } catch (e: Exception) {
        "No se pudo entrar: ${e.localizedMessage ?: "error desconocido"}"
    }

    fun salir() = auth.signOut()
}
