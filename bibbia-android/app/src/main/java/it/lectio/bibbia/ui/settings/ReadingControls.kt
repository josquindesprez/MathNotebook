package it.lectio.bibbia.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.lectio.bibbia.R
import it.lectio.bibbia.data.repository.ReaderFont
import it.lectio.bibbia.data.repository.ReaderLayout
import it.lectio.bibbia.data.repository.ReaderSettings
import it.lectio.bibbia.data.repository.ThemeMode
import it.lectio.bibbia.ui.components.SectionLabel
import it.lectio.bibbia.ui.theme.BibbiaTheme
import it.lectio.bibbia.ui.theme.LightPaper
import it.lectio.bibbia.ui.theme.NightPaper
import it.lectio.bibbia.ui.theme.SepiaPaper
import it.lectio.bibbia.ui.theme.family
import kotlin.math.roundToInt

/** Controlli tipografici, usati sia nel pannello "Aa" della lettura sia nelle Impostazioni. */
@Composable
fun ReadingControls(
    settings: ReaderSettings,
    onChange: ((ReaderSettings) -> ReaderSettings) -> Unit,
    modifier: Modifier = Modifier,
    showExtended: Boolean = true,
) {
    val colors = BibbiaTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // Tema
        Column {
            SectionLabel(stringResource(R.string.setting_theme))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ThemeSwatch(ThemeMode.SYSTEM, settings.theme, stringResource(R.string.theme_system)) { m -> onChange { it.copy(theme = m) } }
                ThemeSwatch(ThemeMode.LIGHT, settings.theme, stringResource(R.string.theme_light)) { m -> onChange { it.copy(theme = m) } }
                ThemeSwatch(ThemeMode.SEPIA, settings.theme, stringResource(R.string.theme_sepia)) { m -> onChange { it.copy(theme = m) } }
                ThemeSwatch(ThemeMode.DARK, settings.theme, stringResource(R.string.theme_dark)) { m -> onChange { it.copy(theme = m) } }
            }
        }

        // Carattere
        Column {
            SectionLabel(stringResource(R.string.setting_font))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReaderFont.entries.forEach { font ->
                    val selected = settings.font == font
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .border(
                                BorderStroke(if (selected) 1.2.dp else 0.6.dp, if (selected) colors.rubric else colors.hairline),
                                RoundedCornerShape(6.dp),
                            )
                            .clickable(role = Role.RadioButton) { onChange { it.copy(font = font) } }
                            .semantics { this.selected = selected }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Aa", style = TextStyle(fontFamily = font.family(), fontSize = 24.sp), color = colors.ink)
                            Text(
                                text = stringResource(font.labelRes()),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) colors.rubric else colors.inkMuted,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        // Dimensione
        LabeledSlider(
            label = stringResource(R.string.setting_size),
            valueLabel = "${settings.fontSizeSp.roundToInt()}",
            value = settings.fontSizeSp,
            range = ReaderSettings.MIN_FONT_SIZE..ReaderSettings.MAX_FONT_SIZE,
            steps = (ReaderSettings.MAX_FONT_SIZE - ReaderSettings.MIN_FONT_SIZE).toInt() - 1,
            onValue = { v -> onChange { it.copy(fontSizeSp = v.roundToInt().toFloat()) } },
        )

        // Interlinea
        LabeledSlider(
            label = stringResource(R.string.setting_line_height),
            valueLabel = "%.1f".format(settings.lineHeight),
            value = settings.lineHeight,
            range = ReaderSettings.MIN_LINE_HEIGHT..ReaderSettings.MAX_LINE_HEIGHT,
            steps = 9,
            onValue = { v -> onChange { it.copy(lineHeight = (v * 10).roundToInt() / 10f) } },
        )

        // Impaginazione
        Column {
            SectionLabel(stringResource(R.string.setting_layout))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LayoutOption(stringResource(R.string.layout_verses), settings.layout == ReaderLayout.VERSES, Modifier.weight(1f)) {
                    onChange { it.copy(layout = ReaderLayout.VERSES) }
                }
                LayoutOption(stringResource(R.string.layout_page), settings.layout == ReaderLayout.PAGE, Modifier.weight(1f)) {
                    onChange { it.copy(layout = ReaderLayout.PAGE) }
                }
            }
        }

        if (showExtended) {
            SwitchRow(stringResource(R.string.setting_verse_numbers), settings.showVerseNumbers) { v ->
                onChange { it.copy(showVerseNumbers = v) }
            }
            SwitchRow(stringResource(R.string.setting_justify), settings.justify) { v ->
                onChange { it.copy(justify = v) }
            }
            SwitchRow(stringResource(R.string.setting_keep_screen_on), settings.keepScreenOn) { v ->
                onChange { it.copy(keepScreenOn = v) }
            }
        }
    }
}

fun ReaderFont.labelRes(): Int = when (this) {
    ReaderFont.GARAMOND -> R.string.font_garamond
    ReaderFont.LITERATA -> R.string.font_literata
    ReaderFont.SYSTEM_SERIF -> R.string.font_system
}

@Composable
private fun ThemeSwatch(mode: ThemeMode, current: ThemeMode, label: String, onSelect: (ThemeMode) -> Unit) {
    val colors = BibbiaTheme.colors
    val selected = mode == current
    val (paper, ink) = when (mode) {
        ThemeMode.SYSTEM -> null to null
        ThemeMode.LIGHT -> LightPaper.paper to LightPaper.ink
        ThemeMode.SEPIA -> SepiaPaper.paper to SepiaPaper.ink
        ThemeMode.DARK -> NightPaper.paper to NightPaper.ink
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.RadioButton) { onSelect(mode) }
            .semantics {
                this.selected = selected
                contentDescription = label
            }
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .then(
                    if (paper != null) {
                        Modifier.background(paper)
                    } else {
                        Modifier.background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                0.5f to LightPaper.paper, 0.5f to NightPaper.paper,
                            ),
                        )
                    },
                )
                .border(
                    BorderStroke(if (selected) 1.6.dp else 0.6.dp, if (selected) colors.rubric else colors.hairline),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text("A", style = MaterialTheme.typography.titleMedium, color = ink ?: colors.inkMuted)
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected) colors.rubric else colors.inkMuted)
    }
}

@Composable
private fun LayoutOption(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = BibbiaTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .border(
                BorderStroke(if (selected) 1.2.dp else 0.6.dp, if (selected) colors.rubric else colors.hairline),
                RoundedCornerShape(6.dp),
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = if (selected) colors.rubric else colors.ink)
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValue: (Float) -> Unit,
) {
    val colors = BibbiaTheme.colors
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(label, Modifier.weight(1f))
            Text(valueLabel, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("A", style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            Spacer(Modifier.width(10.dp))
            Slider(
                value = value,
                onValueChange = onValue,
                valueRange = range,
                steps = steps,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = colors.rubric,
                    activeTrackColor = colors.rubric,
                    inactiveTrackColor = colors.hairline,
                    activeTickColor = colors.rubric.copy(alpha = 0f),
                    inactiveTickColor = colors.hairline.copy(alpha = 0f),
                ),
            )
            Spacer(Modifier.width(10.dp))
            Text("A", style = MaterialTheme.typography.titleLarge, color = colors.inkMuted)
        }
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    val colors = BibbiaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onChecked(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.ink, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.paper,
                checkedTrackColor = colors.rubric,
                uncheckedThumbColor = colors.inkFaint,
                uncheckedTrackColor = colors.paper,
                uncheckedBorderColor = colors.hairline,
            ),
        )
    }
}
