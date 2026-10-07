package it.lectio.bibbia.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import it.lectio.bibbia.R
import it.lectio.bibbia.data.repository.ReaderSettings
import it.lectio.bibbia.data.repository.SettingsRepository
import it.lectio.bibbia.domain.model.Verse
import it.lectio.bibbia.ui.appViewModel
import it.lectio.bibbia.ui.components.BibbiaTopBar
import it.lectio.bibbia.ui.components.Hairline
import it.lectio.bibbia.ui.components.PageMargin
import it.lectio.bibbia.ui.components.ReadingMaxWidth
import it.lectio.bibbia.ui.components.SectionLabel
import it.lectio.bibbia.ui.reader.VerseStyle
import it.lectio.bibbia.ui.reader.buildVerses
import it.lectio.bibbia.ui.reader.readerTextStyle
import it.lectio.bibbia.ui.theme.BibbiaTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settings: SettingsRepository) : ViewModel() {
    val readerSettings: StateFlow<ReaderSettings?> = settings.readerSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun update(transform: (ReaderSettings) -> ReaderSettings) {
        viewModelScope.launch { settings.update(transform) }
    }
}

private val SampleVerses = listOf(
    Verse("riveduta", "PSA", 42, 1, "Come la cerva agogna i rivi dell’acque, così l’anima mia agogna te, o Dio."),
    Verse("riveduta", "PSA", 42, 2, "L’anima mia è assetata di Dio, dell’Iddio vivente: Quando verrò e comparirò al cospetto di Dio?"),
)

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val viewModel = appViewModel { c, _ -> SettingsViewModel(c.settingsRepository) }
    val settings by viewModel.readerSettings.collectAsStateWithLifecycle()
    val colors = BibbiaTheme.colors

    Scaffold(
        containerColor = colors.paper,
        topBar = { BibbiaTopBar(stringResource(R.string.settings), onBack) },
    ) { padding ->
        val current = settings ?: return@Scaffold
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .widthIn(max = ReadingMaxWidth)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PageMargin)
                    .padding(bottom = 40.dp),
            ) {
                // Anteprima dal vivo
                SectionLabel(stringResource(R.string.preview), color = colors.rubric)
                Spacer(Modifier.height(10.dp))
                val style = VerseStyle(
                    numberColor = colors.rubric.copy(alpha = 0.85f),
                    bookmarkColor = colors.rubric,
                    selectionColor = colors.selection,
                    flashColor = colors.selection,
                    highlightColors = colors.highlights,
                    showNumbers = current.showVerseNumbers,
                    italicBrackets = false,
                )
                Text(
                    text = buildVerses(SampleVerses, style, inline = true, emptySet(), emptyMap(), null, null).text,
                    style = readerTextStyle(current, "it"),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(20.dp))
                Hairline()
                Spacer(Modifier.height(20.dp))
                ReadingControls(settings = current, onChange = viewModel::update)
            }
        }
    }
}
