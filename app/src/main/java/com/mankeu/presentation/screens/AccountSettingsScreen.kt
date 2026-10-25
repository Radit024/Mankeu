package com.mankeu.presentation.screens

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.background
import androidx.compose.ui.platform.testTag
import com.mankeu.presentation.ProfileDraft
import com.mankeu.presentation.SettingsPage
import com.mankeu.presentation.components.CardDetails
import com.mankeu.presentation.components.primaryActionButtonColors
import com.mankeu.presentation.components.fadeThrough
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.mankeu.BuildConfig
import com.mankeu.R
import com.mankeu.auth.AuthUser
import com.mankeu.presentation.AppLanguage
import com.mankeu.presentation.FeedbackEvent
import com.mankeu.presentation.LocalFeedback
import com.mankeu.presentation.LocalMankeuSemanticColors
import com.mankeu.presentation.MotionMode
import com.mankeu.presentation.ThemeMode
import com.mankeu.presentation.components.AnimationLicenseButton
import com.mankeu.presentation.components.MankeuAppLogo
import com.mankeu.presentation.components.TourTarget
import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import com.mankeu.presentation.LocalMotion
import com.mankeu.presentation.components.rememberTourListState
import com.mankeu.presentation.components.tourTarget

/** Settings overview and focused pages; the app shell owns navigation and drafts. */
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    motionMode: MotionMode,
    onMotionModeChange: (MotionMode) -> Unit,
    hapticsEnabled: Boolean,
    onHapticsChange: (Boolean) -> Unit,
    soundEnabled: Boolean,
    onSoundChange: (Boolean) -> Unit,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    accountName: String,
    authUser: AuthUser?,
    onSignInGoogle: () -> Unit,
    onSignOut: () -> Unit,
    onOpenHelpCenter: () -> Unit,
    tourOffersEnabled: Boolean,
    onTourOffersChange: (Boolean) -> Unit,
    onResetTours: () -> Unit,
    page: SettingsPage,
    onOpenPage: (SettingsPage) -> Unit,
    draft: ProfileDraft,
    onDraftChange: (ProfileDraft) -> Unit,
    onSaveDraft: () -> Unit,
    stateHolder: androidx.compose.runtime.saveable.SaveableStateHolder = rememberSaveableStateHolder(),
) {
    val motion = LocalMotion.current
    AnimatedContent(
        targetState = page,
        transitionSpec = { fadeThrough(motion) },
        label = "settingsPage",
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) { renderedPage ->
        stateHolder.SaveableStateProvider(renderedPage) {
            if (renderedPage == SettingsPage.HOME) {
                SettingsHub(accountName, themeMode, language, motionMode, authUser != null, onOpenPage, onOpenHelpCenter)
            } else {
                LazyColumn(
                    state = rememberTourListState(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize().imePadding().testTag("settings-detail"),
                ) {
                    when (renderedPage) {
                        SettingsPage.PROFILE -> item { ProfileEditCard(draft, onDraftChange, onSaveDraft) }
                        SettingsPage.ACCOUNT -> item { AccountCard(accountName, authUser, onSignInGoogle, onSignOut) }
                        SettingsPage.ABOUT -> {
                            item { SystemBannerCard() }
                            item {
                                CardDetails("system-specs", stringResource(R.string.settings_specs_show), stringResource(R.string.settings_specs_hide)) { SystemSpecsCard() }
                            }
                            item { ContactCard() }
                            item { AnimationLicenseButton() }
                        }
                        else -> item {
                            SettingsPreferencePage(
                                renderedPage, themeMode, onThemeModeChange, language, onLanguageChange,
                                motionMode, onMotionModeChange, hapticsEnabled, onHapticsChange,
                                soundEnabled, onSoundChange, tourOffersEnabled, onTourOffersChange,
                                onOpenHelpCenter, onResetTours,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Formulir pengisian data profil pemilik, kontak opsional WhatsApp/Telegram,
 * serta informasi usaha tani / komoditas lahan (100% offline-first).
 */
@Composable
private fun ProfileEditCard(
    draft: ProfileDraft,
    onDraftChange: (ProfileDraft) -> Unit,
    onSaveDraft: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header Avatar & Nama
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(56.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            draft.name.trim().take(1).uppercase().ifBlank { "A" },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        draft.name.ifBlank { stringResource(R.string.profile_full_name) },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.profile_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider()

            Text(
                stringResource(R.string.profile_section_owner),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            // Nama Lengkap
            OutlinedTextField(
                value = draft.name,
                onValueChange = { onDraftChange(draft.copy(name = it)) },
                label = { Text(stringResource(R.string.profile_full_name)) },
                supportingText = { Text(stringResource(R.string.profile_full_name_hint)) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("profile-name"),
            )

            // Komoditas Utama
            OutlinedTextField(
                value = draft.commodity,
                onValueChange = { onDraftChange(draft.copy(commodity = it)) },
                label = { Text(stringResource(R.string.profile_commodity)) },
                placeholder = { Text(stringResource(R.string.profile_commodity_placeholder)) },
                supportingText = { Text(stringResource(R.string.profile_commodity_helper)) },
                leadingIcon = { Icon(Icons.Default.Eco, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("profile-commodity"),
            )

            // Luas Lahan
            OutlinedTextField(
                value = draft.landArea,
                onValueChange = { onDraftChange(draft.copy(landArea = it)) },
                label = { Text(stringResource(R.string.profile_land_area)) },
                placeholder = { Text(stringResource(R.string.profile_land_area_placeholder)) },
                supportingText = { Text(stringResource(R.string.profile_land_area_helper)) },
                leadingIcon = { Icon(Icons.Default.Landscape, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("profile-landArea"),
            )

            HorizontalDivider()

            Text(
                stringResource(R.string.profile_section_contact),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            // Nomor WhatsApp
            OutlinedTextField(
                value = draft.whatsapp,
                onValueChange = { onDraftChange(draft.copy(whatsapp = it)) },
                label = { Text(stringResource(R.string.profile_phone)) },
                placeholder = { Text(stringResource(R.string.profile_phone_placeholder)) },
                supportingText = { Text(stringResource(R.string.profile_phone_helper)) },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("profile-whatsapp"),
            )

            // ID / Kontak Telegram
            OutlinedTextField(
                value = draft.telegram,
                onValueChange = { onDraftChange(draft.copy(telegram = it)) },
                label = { Text(stringResource(R.string.profile_telegram)) },
                placeholder = { Text(stringResource(R.string.profile_telegram_placeholder)) },
                supportingText = { Text(stringResource(R.string.profile_telegram_helper)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("profile-telegram"),
            )

            // Lokasi / Desa
            OutlinedTextField(
                value = draft.location,
                onValueChange = { onDraftChange(draft.copy(location = it)) },
                label = { Text(stringResource(R.string.profile_location)) },
                placeholder = { Text(stringResource(R.string.profile_location_placeholder)) },
                supportingText = { Text(stringResource(R.string.profile_location_helper)) },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("profile-location"),
            )

            // Tombol Simpan Perubahan
            Button(
                onClick = onSaveDraft,
                colors = primaryActionButtonColors(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.profile_save), fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Kartu status akun Google & pencadangan Google Drive terenkripsi.
 */
@Composable
private fun AccountCard(
    accountName: String,
    authUser: AuthUser?,
    onSignInGoogle: () -> Unit,
    onSignOut: () -> Unit,
) {
    val semantic = LocalMankeuSemanticColors.current
    val feedback = LocalFeedback.current

    Card(
        modifier = Modifier.fillMaxWidth().tourTarget(TourTarget.SETTINGS_DRIVE),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                stringResource(R.string.account_section_cloud),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (authUser != null) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = semantic.success)
                        } else {
                            Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        authUser?.displayName ?: accountName.ifBlank { stringResource(R.string.account_title) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (authUser != null) {
                        Text(
                            authUser.email ?: stringResource(R.string.account_signed_in_as, authUser.uid.take(8)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                            Icon(
                                Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = semantic.success,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.settings_google_connected),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = semantic.success,
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.account_signed_out_status),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (authUser == null) {
                Button(
                    colors = primaryActionButtonColors(),
                    onClick = {
                        feedback(FeedbackEvent.TAP)
                        onSignInGoogle()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_google_logo),
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.Unspecified,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.account_sign_in_google), fontWeight = FontWeight.Bold)
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                stringResource(R.string.account_prototype_notice),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                stringResource(R.string.account_offline_notice),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = {
                        feedback(FeedbackEvent.TAP)
                        onSignOut()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.account_sign_out), fontWeight = FontWeight.Bold)
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = semantic.successContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = semantic.onSuccessContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                stringResource(R.string.account_prototype_notice),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelLarge,
                                color = semantic.onSuccessContainer,
                            )
                            Text(
                                stringResource(R.string.account_drive_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = semantic.onSuccessContainer,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Banner Informasi Sistem Arina Agri
 */
@Composable
private fun SystemBannerCard() {
    val semantic = LocalMankeuSemanticColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MankeuAppLogo(size = 56.dp)
            Text(
                stringResource(R.string.app_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.app_subtitle),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(vertical = 2.dp),
            ) {
                Text(
                    stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = semantic.successContainer,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = semantic.onSuccessContainer,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            stringResource(R.string.system_offline_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            color = semantic.onSuccessContainer,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            stringResource(R.string.system_offline_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = semantic.onSuccessContainer,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tabel Rincian Spesifikasi Sistem Native
 */
@Composable
private fun SystemSpecsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.system_tab_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            val specs = listOf(
                stringResource(R.string.system_platform_label) to stringResource(R.string.system_platform_val),
                stringResource(R.string.system_ui_label) to stringResource(R.string.system_ui_val),
                stringResource(R.string.system_database_label) to stringResource(R.string.system_database_val),
                stringResource(R.string.system_version_label) to BuildConfig.VERSION_NAME,
                stringResource(R.string.system_build_label) to "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
            )

            specs.forEachIndexed { index, (label, value) ->
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        value,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (index < specs.lastIndex) {
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}

/**
 * Kartu Hubungi Kami / Bantuan Pengembang
 */
@Composable
private fun ContactCard() {
    val context = LocalContext.current
    val email = stringResource(R.string.settings_contact_email)
    val subject = stringResource(R.string.settings_contact_subject, BuildConfig.VERSION_NAME)
    val noAppMessage = stringResource(R.string.settings_contact_no_app)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.settings_contact_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.settings_contact_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.MailOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.settings_contact_email),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Button(
                colors = primaryActionButtonColors(),
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO, "mailto:$email".toUri())
                        .putExtra(Intent.EXTRA_SUBJECT, subject)
                    runCatching { context.startActivity(intent) }
                        .onFailure { Toast.makeText(context, noAppMessage, Toast.LENGTH_LONG).show() }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Text(stringResource(R.string.settings_contact_btn), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun ToggleRow(
    title: String,
    help: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val feedback = LocalFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = { feedback(FeedbackEvent.TAP); onCheckedChange(it) },
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(help, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
internal fun SettingCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}
