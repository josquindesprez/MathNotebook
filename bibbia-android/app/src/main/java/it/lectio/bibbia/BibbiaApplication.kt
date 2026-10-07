package it.lectio.bibbia

import android.app.Application
import kotlinx.coroutines.launch

class BibbiaApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // I testi inclusi nell'APK vengono importati nel database locale al primo avvio
        // (e ripresi se l'importazione era stata interrotta). Nessun accesso alla rete.
        container.applicationScope.launch {
            container.translationRepository.ensureBundledInstalled()
        }
    }
}
