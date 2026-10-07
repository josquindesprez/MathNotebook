package it.lectio.bibbia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.lectio.bibbia.data.repository.ThemeMode
import it.lectio.bibbia.navigation.BibbiaNavHost
import it.lectio.bibbia.ui.theme.BibbiaTheme
import kotlinx.coroutines.flow.map

/** Unica Activity: ospita la navigazione Compose e applica il tema scelto dall'utente. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val settings = (application as BibbiaApplication).container.settingsRepository
        val themeFlow = settings.readerSettings.map { it.theme }
        setContent {
            val theme by themeFlow.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            BibbiaTheme(mode = theme) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(BibbiaTheme.colors.paper)
                        .imePadding(),
                ) {
                    BibbiaNavHost()
                }
            }
        }
    }
}
