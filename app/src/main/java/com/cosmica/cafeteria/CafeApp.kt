package com.cosmica.cafeteria

import android.app.Application
import com.cosmica.cafeteria.data.Repositorio
import com.cosmica.cafeteria.data.Sesion
import com.google.firebase.FirebaseApp

class CafeApp : Application() {
    /** False si el APK se compiló sin google-services.json. */
    val firebaseConfigurado: Boolean by lazy { FirebaseApp.getApps(this).isNotEmpty() }
    val repositorio: Repositorio by lazy { Repositorio() }
    val sesion: Sesion by lazy { Sesion() }
}
