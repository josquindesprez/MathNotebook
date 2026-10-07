package it.lectio.bibbia.ui.translations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import it.lectio.bibbia.R
import it.lectio.bibbia.data.repository.TranslationRepository
import it.lectio.bibbia.domain.model.InstallStatus
import it.lectio.bibbia.domain.model.TranslationOrigin
import it.lectio.bibbia.domain.model.TranslationState
import it.lectio.bibbia.ui.appViewModel
import it.lectio.bibbia.ui.components.BibbiaTopBar
import it.lectio.bibbia.ui.components.Hairline
import it.lectio.bibbia.ui.components.PageMargin
import it.lectio.bibbia.ui.components.ReadingMaxWidth
import it.lectio.bibbia.ui.formatDate
import it.lectio.bibbia.ui.messageRes
import it.lectio.bibbia.ui.theme.BibbiaTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class TranslationsViewModel(private val repository: TranslationRepository) : ViewModel() {
    val states: StateFlow<List<TranslationState>> = repository.observeStates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun install(id: String) {
        viewModelScope.launch { repository.install(id) }
    }

    fun remove(id: String) {
        viewModelScope.launch { repository.uninstall(id) }
    }
}

@Composable
fun TranslationsScreen(onBack: () -> Unit) {
    val viewModel = appViewModel { c, _ -> TranslationsViewModel(c.translationRepository) }
    val states by viewModel.states.collectAsStateWithLifecycle()
    val colors = BibbiaTheme.colors

    Scaffold(
        containerColor = colors.paper,
        topBar = { BibbiaTopBar(stringResource(R.string.translations), onBack) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier.widthIn(max = ReadingMaxWidth),
                contentPadding = PaddingValues(bottom = 40.dp),
            ) {
                item {
                    Text(
                        stringResource(R.string.translations_intro),
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = colors.inkMuted,
                        modifier = Modifier.padding(horizontal = PageMargin, vertical = 12.dp),
                    )
                }
                items(states, key = { it.translation.id }) { state ->
                    TranslationCard(
                        state = state,
                        onInstall = { viewModel.install(state.translation.id) },
                        onRemove = { viewModel.remove(state.translation.id) },
                    )
                    Hairline(Modifier.padding(horizontal = PageMargin))
                }
                item {
                    Text(
                        stringResource(R.string.cei_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted,
                        modifier = Modifier.padding(horizontal = PageMargin, vertical = 20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TranslationCard(state: TranslationState, onInstall: () -> Unit, onRemove: () -> Unit) {
    val colors = BibbiaTheme.colors
    val t = state.translation
    val downloadable = t.origin is TranslationOrigin.Downloadable
    Column(Modifier.padding(horizontal = PageMargin, vertical = 18.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                t.abbreviation,
                style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 0.14.em),
                color = colors.rubric,
            )
            Spacer(Modifier.width(10.dp))
            Text(t.languageLabel, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
        }
        Text(t.name, style = MaterialTheme.typography.titleLarge, color = colors.ink)
        Text(t.edition, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
        Spacer(Modifier.height(8.dp))
        Text(t.description, style = MaterialTheme.typography.bodyMedium, color = colors.ink)
        Spacer(Modifier.height(8.dp))
        Text(
            t.attribution,
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
            color = colors.inkMuted,
        )
        Text(
            stringResource(R.string.license_label, t.license),
            style = MaterialTheme.typography.bodySmall,
            color = colors.inkMuted,
        )
        Spacer(Modifier.height(10.dp))
        when (state.status) {
            InstallStatus.INSTALLED -> Row(verticalAlignment = Alignment.CenterVertically) {
                val count = NumberFormat.getIntegerInstance(Locale.ITALIAN).format(state.verseCount)
                Text(
                    stringResource(R.string.installed_summary, count, state.installedAt?.let { formatDate(it) } ?: ""),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.inkMuted,
                    modifier = Modifier.weight(1f),
                )
                if (downloadable) {
                    TextButton(onClick = onRemove) { Text(stringResource(R.string.remove), color = colors.inkMuted) }
                }
            }
            InstallStatus.INSTALLING -> Column {
                Text(
                    stringResource(R.string.installing_percent, (state.progress * 100).toInt()),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.inkMuted,
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { state.progress },
                    color = colors.rubric,
                    trackColor = colors.hairline,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            InstallStatus.FAILED, InstallStatus.NOT_INSTALLED -> Column {
                state.error?.let { error ->
                    Text(
                        stringResource(error.messageRes()),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.rubric,
                    )
                    Spacer(Modifier.height(6.dp))
                }
                val sizeMb = (t.origin as? TranslationOrigin.Downloadable)?.approximateSizeMb
                OutlinedButton(
                    onClick = onInstall,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.rubric),
                ) {
                    Text(
                        when {
                            state.status == InstallStatus.FAILED -> stringResource(R.string.retry)
                            sizeMb != null -> stringResource(R.string.download_size, sizeMb)
                            else -> stringResource(R.string.install)
                        },
                    )
                }
            }
        }
    }
}
