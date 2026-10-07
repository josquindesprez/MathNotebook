package it.lectio.bibbia.ui.about

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import it.lectio.bibbia.BuildConfig
import it.lectio.bibbia.R
import it.lectio.bibbia.data.source.TranslationCatalog
import it.lectio.bibbia.ui.components.BibbiaTopBar
import it.lectio.bibbia.ui.components.Hairline
import it.lectio.bibbia.ui.components.PageMargin
import it.lectio.bibbia.ui.components.ReadingMaxWidth
import it.lectio.bibbia.ui.components.SectionLabel
import it.lectio.bibbia.ui.theme.BibbiaTheme

/** Informazioni, privacy e attribuzioni (riassunto di LICENSES/ATTRIBUTIONS.md). */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val colors = BibbiaTheme.colors
    Scaffold(
        containerColor = colors.paper,
        topBar = { BibbiaTopBar(stringResource(R.string.about), onBack) },
    ) { padding ->
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
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, color = colors.ink)
                Text(
                    stringResource(R.string.version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted,
                )

                Block(stringResource(R.string.privacy_title), stringResource(R.string.privacy_text))

                Spacer(Modifier.height(28.dp))
                SectionLabel(stringResource(R.string.texts_and_licenses), color = colors.rubric)
                TranslationCatalog.all.forEach { t ->
                    Spacer(Modifier.height(14.dp))
                    Text(t.name, style = MaterialTheme.typography.titleMedium, color = colors.ink)
                    Text(t.edition, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                    Text(
                        t.attribution,
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = colors.inkMuted,
                    )
                    Text(t.license + " " + t.sourceUrl, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                }
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.cei_note), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)

                Block(stringResource(R.string.fonts_title), stringResource(R.string.fonts_text))
            }
        }
    }
}

@Composable
private fun Block(title: String, text: String) {
    val colors = BibbiaTheme.colors
    Spacer(Modifier.height(28.dp))
    SectionLabel(title, color = colors.rubric)
    Spacer(Modifier.height(8.dp))
    Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.ink)
    Spacer(Modifier.height(16.dp))
    Hairline()
}
