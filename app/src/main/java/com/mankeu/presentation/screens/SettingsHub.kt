package com.mankeu.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mankeu.R
import com.mankeu.presentation.*

/** A single scrolling overview; account identity and business profile remain separate. */
@Composable
internal fun SettingsHub(
    accountName: String,
    themeMode: ThemeMode,
    language: AppLanguage,
    motionMode: MotionMode,
    signedIn: Boolean,
    onOpenPage: (SettingsPage) -> Unit,
    onOpenHelpCenter: () -> Unit,
) {
    val dark = isDarkScheme()
    fun accent(family: AccentFamily) = accentFamilyColors(family, dark)
    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier.fillMaxSize().testTag("settings-home"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item("profile") {
            Card(onClick = { onOpenPage(SettingsPage.PROFILE) }, modifier = Modifier.fillMaxWidth().testTag("settings-profile")) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                            Text(accountName.trim().take(1).uppercase().ifBlank { "A" }, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text(accountName.ifBlank { stringResource(R.string.profile_title) }, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.settings_edit_profile), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                }
            }
        }
        item("experience") {
            SettingsGroup(stringResource(R.string.settings_experience)) {
                SettingsLinkRow(stringResource(R.string.settings_display), stringResource(themeMode.labelRes), Icons.Default.Palette, accent(AccentFamily.BLUE), { onOpenPage(SettingsPage.APPEARANCE) }, Modifier.testTag("settings-appearance"))
                SettingsLinkRow(stringResource(R.string.settings_language), language.label, Icons.Default.Language, accent(AccentFamily.MINT), { onOpenPage(SettingsPage.LANGUAGE) }, Modifier.testTag("settings-language"))
                SettingsLinkRow(stringResource(R.string.settings_motion), stringResource(motionMode.labelRes), Icons.Default.Animation, accent(AccentFamily.LAVENDER), { onOpenPage(SettingsPage.MOTION) }, Modifier.testTag("settings-motion"))
                SettingsLinkRow(stringResource(R.string.settings_feedback), stringResource(R.string.settings_feedback_summary), Icons.Default.Vibration, accent(AccentFamily.SAND), { onOpenPage(SettingsPage.FEEDBACK) }, Modifier.testTag("settings-feedback"))
            }
        }
        item("account") {
            SettingsGroup(stringResource(R.string.settings_optional_account)) {
                SettingsLinkRow(stringResource(R.string.settings_google_account), stringResource(if (signedIn) R.string.settings_google_connected else R.string.account_signed_out_status), Icons.Default.AccountCircle, accent(AccentFamily.MINT), { onOpenPage(SettingsPage.ACCOUNT) }, Modifier.testTag("settings-account"))
            }
        }
        item("help") {
            SettingsGroup(stringResource(R.string.help_center_title)) {
                SettingsLinkRow(stringResource(R.string.help_open_center), stringResource(R.string.settings_help_summary), Icons.Default.HelpOutline, accent(AccentFamily.TEAL), onOpenHelpCenter, Modifier.testTag("settings-help"))
                SettingsLinkRow(stringResource(R.string.settings_tour_title), stringResource(R.string.settings_guides_summary), Icons.Default.Explore, accent(AccentFamily.BLUE), { onOpenPage(SettingsPage.GUIDES) }, Modifier.testTag("settings-guides"))
            }
        }
        item("about") {
            SettingsGroup(stringResource(R.string.settings_about_entry)) {
                SettingsLinkRow(stringResource(R.string.settings_about_entry), stringResource(R.string.settings_about_summary), Icons.Default.Info, accent(AccentFamily.TERRACOTTA), { onOpenPage(SettingsPage.ABOUT) }, Modifier.testTag("settings-about"))
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(content = content)
        }
    }
}

@Composable
internal fun SettingsLinkRow(title: String, value: String, icon: ImageVector, accent: MetricAccentColors, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Button, onClick = onClick),
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(value) },
        leadingContent = {
            Surface(color = accent.container, shape = RoundedCornerShape(10.dp)) {
                Icon(icon, null, Modifier.padding(8.dp).size(20.dp), tint = accent.icon)
            }
        },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
