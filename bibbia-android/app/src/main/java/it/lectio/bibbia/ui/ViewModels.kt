package it.lectio.bibbia.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import it.lectio.bibbia.AppContainer
import it.lectio.bibbia.BibbiaApplication

/** Recupera il contenitore delle dipendenze dall'Application. */
@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as BibbiaApplication).container

/** Crea (o recupera) un ViewModel che dipende dal contenitore e dallo stato salvato. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    crossinline create: (AppContainer, SavedStateHandle) -> VM,
): VM {
    val container = appContainer()
    return viewModel(
        key = key,
        factory = viewModelFactory { initializer { create(container, createSavedStateHandle()) } },
    )
}
