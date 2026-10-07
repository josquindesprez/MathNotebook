package it.lectio.bibbia.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.lectio.bibbia.R
import it.lectio.bibbia.ui.theme.BibbiaTheme
import it.lectio.bibbia.ui.theme.SectionLabelStyle

/** Larghezza massima del testo: oltre questa misura le righe diventano faticose da leggere. */
val ReadingMaxWidth: Dp = 640.dp

/** Margine laterale standard delle pagine. */
val PageMargin: Dp = 28.dp

/** Contenitore centrato a larghezza di lettura (utile su tablet e in orizzontale). */
@Composable
fun ReadingColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = ReadingMaxWidth).fillMaxWidth()) { content() }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = BibbiaTheme.colors.inkMuted) {
    Text(
        text = text.uppercase(),
        style = SectionLabelStyle,
        color = color,
        modifier = modifier,
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, thickness = 0.6.dp, color = BibbiaTheme.colors.hairline)
}

/** Barra superiore discreta: niente colori pieni, niente ombre. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibbiaTopBar(
    title: String,
    onBack: (() -> Unit)?,
    actions: @Composable () -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(title, style = MaterialTheme.typography.titleLarge)
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BibbiaTheme.colors.paper,
            scrolledContainerColor = BibbiaTheme.colors.paper,
            titleContentColor = BibbiaTheme.colors.ink,
            navigationIconContentColor = BibbiaTheme.colors.inkMuted,
            actionIconContentColor = BibbiaTheme.colors.inkMuted,
        ),
    )
}

/** Stato vuoto sobrio: un titolo e una riga di spiegazione, centrati. */
@Composable
fun EmptyState(title: String, message: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PageMargin, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = BibbiaTheme.colors.ink,
            textAlign = TextAlign.Center,
        )
        if (message != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = BibbiaTheme.colors.inkMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
