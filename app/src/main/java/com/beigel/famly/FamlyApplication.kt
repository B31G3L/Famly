package com.beigel.famly

import android.app.Application
import com.beigel.famly.di.AppContainer
import com.google.firebase.FirebaseApp

class FamlyApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        appContainer = AppContainer()
    }
}
