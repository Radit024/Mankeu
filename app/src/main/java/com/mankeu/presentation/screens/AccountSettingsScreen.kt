package com.mankeu.presentation.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.mankeu.domain.ProfileContactUtils
import com.mankeu.presentation.AppLanguage
import com.mankeu.presentation.FeedbackEvent
import com.mankeu.presentation.LocalFeedback
import com.mankeu.presentation.LocalMankeuSemanticColors
import com.mankeu.presentation.MotionMode
import com.mankeu.presentation.ThemeMode
import com.mankeu.presentation.components.AnimationLicenseButton
import com.mankeu.presentation.components.MankeuAppLogo
import com.mankeu.presentation.components.TourStep
import com.mankeu.presentation.components.TourTarget
import com.mankeu.presentation.components.rememberTourListState
import com.mankeu.presentation.components.tourTarget

enum class SettingsTab(val titleRes: Int, val icon: ImageVector) {
    PROFILE(R.string.settings_tab_profile, Icons.Default.Person),
    GENERAL(R.string.settings_tab_general, Icons.Default.Tune),
    SYSTEM(R.string.settings_tab_system, Icons.Default.Info),
}

/**
 * Pengaturan: Tab Profil (data pemilik, usaha, kontak & akun Google), Tab Umum (tampilan, bahasa,
 * gerakan, respon getar/suara, panduan), dan Tab Sistem (spesifikasi platform, privasi offline, lisensi,
 * kontak dukungan).
 */
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
    onAccountNameChange: (String) -> Unit,
    authUser: AuthUser?,
    onSignInGoogle: () -> Unit,
    onSignOut: () -> Unit,
    onOpenHelpCenter: () -> Unit,
    tourOffersEnabled: Boolean,
    onTourOffersChange: (Boolean) -> Unit,
    onResetTours: () -> Unit,
    whatsappPhone: String = "",
    telegramContact: String = "",
    location: String = "",
    commodity: String = "",
    landArea: String = "",
    onSaveProfile: (name: String, wa: String, tele: String, loc: String, comm: String, area: String) -> Unit = { _, _, _, _, _, _ -> },
    activeTourStep: TourStep? = null,
) {
    val feedback = LocalFeedback.current
    var selectedTab by rememberSaveable { mutableStateOf(SettingsTab.PROFILE) }

    // Otomatis arahkan tab aktif jika tour interaktif memandu target tertentu
    LaunchedEffect(activeTourStep) {
        when (activeTourStep?.target) {
            TourTarget.SETTINGS_GUIDES, TourTarget.SETTINGS_DISPLAY -> {
                selectedTab = SettingsTab.GENERAL
            }
            TourTarget.SETTINGS_DRIVE -> {
                selectedTab = SettingsTab.PROFILE
            }
            else -> {}
        }
    }

    LazyColumn(
        state = rememberTourListState(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Tab Bar Navigasi (Profil, Umum, Sistem)
        item {
            PrimaryTabRow(
                selectedTabIndex = SettingsTab.entries.indexOf(selectedTab),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            ) {
                SettingsTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = {
                            if (selectedTab != tab) {
                                feedback(FeedbackEvent.TAP)
                                selectedTab = tab
                            }
                        },
                        icon = {
                            Icon(tab.icon, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        text = {
                            Text(
                                stringResource(tab.titleRes),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            )
                        },
                    )
                }
            }
        }

        // ==========================================
        // TAB 1: PROFIL PEMILIK & KONTAK
        // ==========================================
        if (selectedTab == SettingsTab.PROFILE) {
            item {
                ProfileEditCard(
                    accountName = accountName,
                    whatsappPhone = whatsappPhone,
                    telegramContact = telegramContact,
                    location = location,
                    commodity = commodity,
                    landArea = landArea,
                    authUser = authUser,
                    onSaveProfile = { name, wa, tele, loc, comm, area ->
                        onAccountNameChange(name)
                        onSaveProfile(name, wa, tele, loc, comm, area)
                    },
                )
            }

            item {
                AccountCard(
                    accountName = accountName,
                    authUser = authUser,
                    onSignInGoogle = onSignInGoogle,
                    onSignOut = onSignOut,
                )
            }
        }

        // ==========================================
        // TAB 2: PENGATURAN UMUM
        // ==========================================
        if (selectedTab == SettingsTab.GENERAL) {
            item {
                SettingCard(
                    title = stringResource(R.string.settings_display),
                    modifier = Modifier.tourTarget(TourTarget.SETTINGS_DISPLAY),
                ) {
                    SegmentedChoice(
                        options = ThemeMode.entries.map { it to stringResource(it.labelRes) },
                        selected = themeMode,
                        onSelect = onThemeModeChange,
                    )
                }
            }

            item {
                SettingCard(stringResource(R.string.settings_language)) {
                    LanguagePicker(language = language, onLanguageChange = onLanguageChange)
                }
            }

            item {
                SettingCard(stringResource(R.string.settings_motion)) {
                    SegmentedChoice(
                        options = MotionMode.entries.map { it to stringResource(it.labelRes) },
                        selected = motionMode,
                        onSelect = onMotionModeChange,
                    )
                    Text(
                        stringResource(R.string.settings_motion_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SettingCard(stringResource(R.string.settings_feedback)) {
                    ToggleRow(
                        title = stringResource(R.string.settings_haptics),
                        help = stringResource(R.string.settings_haptics_help),
                        checked = hapticsEnabled,
                        onCheckedChange = onHapticsChange,
                    )
                    ToggleRow(
                        title = stringResource(R.string.settings_sound),
                        help = stringResource(R.string.settings_sound_help),
                        checked = soundEnabled,
                        onCheckedChange = onSoundChange,
                    )
                }
            }

            // Panduan & Bantuan Interaktif
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().tourTarget(TourTarget.SETTINGS_GUIDES),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.settings_tour_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.settings_tour_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = onOpenHelpCenter,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        ) {
                            Text(stringResource(R.string.help_open_center), fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.settings_tour_offers), modifier = Modifier.weight(1f))
                            Switch(checked = tourOffersEnabled, onCheckedChange = onTourOffersChange)
                        }
                        TextButton(
                            onClick = onResetTours,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.settings_tour_reset), fontWeight = FontWeight.Bold)
                        }
                        Text(
                            stringResource(R.string.settings_tour_reset_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // ==========================================
        // TAB 3: INFORMASI SISTEM & PLATFORM
        // ==========================================
        if (selectedTab == SettingsTab.SYSTEM) {
            item {
                SystemBannerCard()
            }

            item {
                SystemSpecsCard()
            }

            item {
                ContactCard()
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AnimationLicenseButton()
                    }
                }
            }
        }
    }
}

/**
 * Formulir pengisian data profil pemilik, kontak notifikasi WhatsApp/Telegram,
 * serta informasi usaha tani / komoditas lahan (100% offline-first).
 */
@Composable
private fun ProfileEditCard(
    accountName: String,
    whatsappPhone: String,
    telegramContact: String,
    location: String,
    commodity: String,
    landArea: String,
    authUser: AuthUser?,
    onSaveProfile: (name: String, wa: String, tele: String, loc: String, comm: String, area: String) -> Unit,
) {
    val context = LocalContext.current
    val feedback = LocalFeedback.current
    val semantic = LocalMankeuSemanticColors.current

    val initialName = authUser?.displayName?.takeIf { it.isNotBlank() } ?: accountName
    var nameInput by rememberSaveable(accountName, authUser) { mutableStateOf(initialName) }
    var whatsappInput by rememberSaveable(whatsappPhone) { mutableStateOf(whatsappPhone) }
    var telegramInput by rememberSaveable(telegramContact) { mutableStateOf(telegramContact) }
    var locationInput by rememberSaveable(location) { mutableStateOf(location) }
    var commodityInput by rememberSaveable(commodity) { mutableStateOf(commodity) }
    var landAreaInput by rememberSaveable(landArea) { mutableStateOf(landArea) }
    var showSavedStatus by remember { mutableStateOf(false) }

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
                        val avatarText = authUser?.displayName?.take(1)
                            ?: authUser?.email?.take(1)
                            ?: nameInput.trim().take(1)
                        if (avatarText.isBlank()) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        } else {
                            Text(
                                avatarText.uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        nameInput.ifBlank { stringResource(R.string.profile_full_name) },
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

            // Nama Lengkap
            OutlinedTextField(
                value = nameInput,
                onValueChange = {
                    nameInput = it
                    showSavedStatus = false
                },
                label = { Text(stringResource(R.string.profile_full_name)) },
                supportingText = { Text(stringResource(R.string.profile_full_name_hint)) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Nomor WhatsApp
            OutlinedTextField(
                value = whatsappInput,
                onValueChange = {
                    whatsappInput = it
                    showSavedStatus = false
                },
                label = { Text(stringResource(R.string.profile_phone)) },
                placeholder = { Text(stringResource(R.string.profile_phone_placeholder)) },
                supportingText = { Text(stringResource(R.string.profile_phone_helper)) },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // ID / Kontak Telegram
            OutlinedTextField(
                value = telegramInput,
                onValueChange = {
                    telegramInput = it
                    showSavedStatus = false
                },
                label = { Text(stringResource(R.string.profile_telegram)) },
                placeholder = { Text(stringResource(R.string.profile_telegram_placeholder)) },
                supportingText = { Text(stringResource(R.string.profile_telegram_helper)) },
                leadingIcon = { Icon(Icons.Default.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Lokasi / Desa
            OutlinedTextField(
                value = locationInput,
                onValueChange = {
                    locationInput = it
                    showSavedStatus = false
                },
                label = { Text(stringResource(R.string.profile_location)) },
                placeholder = { Text(stringResource(R.string.profile_location_placeholder)) },
                supportingText = { Text(stringResource(R.string.profile_location_helper)) },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Komoditas Utama
            OutlinedTextField(
                value = commodityInput,
                onValueChange = {
                    commodityInput = it
                    showSavedStatus = false
                },
                label = { Text(stringResource(R.string.profile_commodity)) },
                placeholder = { Text(stringResource(R.string.profile_commodity_placeholder)) },
                supportingText = { Text(stringResource(R.string.profile_commodity_helper)) },
                leadingIcon = { Icon(Icons.Default.Eco, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Luas Lahan
            OutlinedTextField(
                value = landAreaInput,
                onValueChange = {
                    landAreaInput = it
                    showSavedStatus = false
                },
                label = { Text(stringResource(R.string.profile_land_area)) },
                placeholder = { Text(stringResource(R.string.profile_land_area_placeholder)) },
                supportingText = { Text(stringResource(R.string.profile_land_area_helper)) },
                leadingIcon = { Icon(Icons.Default.Landscape, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (showSavedStatus) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = semantic.successContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = semantic.onSuccessContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.profile_saved_toast),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = semantic.onSuccessContainer,
                        )
                    }
                }
            }

            // Tombol Simpan Perubahan
            Button(
                onClick = {
                    feedback(FeedbackEvent.TAP)
                    val trimmedName = nameInput.trim()
                    val normalizedWa = ProfileContactUtils.normalizeWhatsAppPhone(whatsappInput)
                    val normalizedTele = ProfileContactUtils.normalizeTelegramContact(telegramInput).displayValue

                    onSaveProfile(
                        trimmedName,
                        normalizedWa,
                        normalizedTele,
                        locationInput.trim(),
                        commodityInput.trim(),
                        landAreaInput.trim(),
                    )
                    showSavedStatus = true
                    Toast.makeText(context, context.getString(R.string.profile_saved_toast), Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
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
                                "Google Drive Aktif",
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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
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
private fun ToggleRow(
    title: String,
    help: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val feedback = LocalFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
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
private fun SettingCard(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SegmentedChoice(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = selected == value,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                label = {
                    Text(
                        label,
                        maxLines = 1,
                        fontWeight = if (selected == value) FontWeight.Bold else FontWeight.Medium,
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePicker(language: AppLanguage, onLanguageChange: (AppLanguage) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = language.label,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.settings_language)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppLanguage.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        expanded = false
                        if (option != language) onLanguageChange(option)
                    },
                    trailingIcon = {
                        if (option == language) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                )
            }
        }
    }
}
