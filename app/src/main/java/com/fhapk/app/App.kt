package com.fhapk.app

import android.app.Application
import com.fhapk.app.data.AppDatabase
import com.fhapk.app.data.NoteRepository
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class App : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val repository: NoteRepository by lazy {
        NoteRepository(AppDatabase.get(this).noteDao(), applicationScope)
    }

    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
        } catch (_: Exception) {
            // Firebase tidak dikonfigurasi: aplikasi tetap berjalan offline (lokal saja)
        }
        repository.startSync()
    }
}