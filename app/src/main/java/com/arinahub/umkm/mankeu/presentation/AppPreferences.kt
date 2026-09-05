package com.arinahub.umkm.mankeu.presentation

import android.content.Context
import android.content.res.Configuration
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import com.arinahub.umkm.mankeu.R
import java.util.Locale

enum class ThemeMode(val labelRes: Int) {
    SYSTEM(R.string.settings_theme_system),
    LIGHT(R.string.settings_theme_light),
    DARK(R.string.settings_theme_dark),
}

/**
 * How much decorative animation to run. [REDUCED] keeps meaning-carrying transitions but shortens
 * them; [OFF] removes them entirely. Honoured by [com.arinahub.umkm.mankeu.presentation.components]
 * and the screen-change transition in `ArinaCalculatorApp`.
 */
enum class MotionMode(val labelRes: Int) {
    FULL(R.string.settings_motion_full),
    REDUCED(R.string.settings_motion_reduced),
    OFF(R.string.settings_motion_off),
}

enum class AppLanguage(val tag: String, val label: String) {
    INDONESIAN("id", "Bahasa Indonesia"),
    ENGLISH("en", "English"),
}

private const val PREFS_NAME = "arina_ui_prefs"
private const val KEY_THEME_MODE = "theme_mode"
private const val KEY_MOTION_MODE = "motion_mode"
private const val KEY_LANGUAGE = "language"
private const val KEY_ACCOUNT_NAME = "account_name"
private const val KEY_HAS_SEEN_TOUR = "has_seen_tour"
private const val KEY_HAS_SEEN_CORE_TOUR = "has_seen_core_tour"
private const val KEY_HAS_SEEN_SETTINGS_TOUR = "has_seen_settings_tour"
private const val KEY_HAS_SEEN_SCOPE_TOUR = "has_seen_scope_tour"
private const val KEY_HAS_SEEN_ENTRY_TOUR = "has_seen_entry_tour"
private const val KEY_HAS_SEEN_REPORTS_TOUR = "has_seen_reports_tour"
private const val KEY_HAS_SEEN_IMPORT_TOUR = "has_seen_import_tour"
private const val KEY_HAPTICS = "haptics_enabled"
private const val KEY_SOUND = "sound_enabled"

private fun Context.prefs() = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

fun Context.readLanguage(): AppLanguage {
    val stored = prefs().getString(KEY_LANGUAGE, null)
    return AppLanguage.entries.firstOrNull { it.tag == stored } ?: AppLanguage.INDONESIAN
}

/**
 * Wraps a base context so every `stringResource` in the app resolves against the owner's chosen
 * language rather than the phone's. Applied in `MainActivity.attachBaseContext`, which is the
 * reliable pre-API-33 route for a per-app language (the framework `LocaleManager` only exists on
 * API 33+, and this app supports API 26+). Changing the language calls `Activity.recreate()`.
 */
fun Context.withAppLanguage(): Context {
    val locale = Locale.forLanguageTag(readLanguage().tag)
    Locale.setDefault(locale)
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    return createConfigurationContext(config)
}

/**
 * Device-local display preferences (theme, motion, language). Deliberately backed by
 * [android.content.SharedPreferences] rather than a Room table: these are per-device UI choices,
 * not accounting data, and must stay out of the audited financial schema, its migrations, and its
 * export/backup contract.
 */
class AppPreferencesState internal constructor(context: Context) {
    private val appContext = context.applicationContext

    var themeMode: ThemeMode by mutableStateOf(
        appContext.prefs().getString(KEY_THEME_MODE, null)
            ?.let { raw -> runCatching { ThemeMode.valueOf(raw) }.getOrNull() }
            ?: ThemeMode.SYSTEM,
    )
        private set

    var motionMode: MotionMode by mutableStateOf(
        appContext.prefs().getString(KEY_MOTION_MODE, null)
            ?.let { raw -> runCatching { MotionMode.valueOf(raw) }.getOrNull() }
            ?: MotionMode.FULL,
    )
        private set

    var language: AppLanguage by mutableStateOf(appContext.readLanguage())
        private set

    /**
     * Touch vibration. On by default because the phone's own "Touch vibration" setting is the outer
     * switch — this one can only turn it off for this app, never on when the phone says no.
     */
    var hapticsEnabled: Boolean by mutableStateOf(appContext.prefs().getBoolean(KEY_HAPTICS, true))
        private set

    /** Touch sounds. Off by default: a bookkeeping app that chirps at every tap is a nuisance. */
    var soundEnabled: Boolean by mutableStateOf(appContext.prefs().getBoolean(KEY_SOUND, false))
        private set

    fun updateHaptics(enabled: Boolean) {
        hapticsEnabled = enabled
        appContext.prefs().edit { putBoolean(KEY_HAPTICS, enabled) }
    }

    fun updateSound(enabled: Boolean) {
        soundEnabled = enabled
        appContext.prefs().edit { putBoolean(KEY_SOUND, enabled) }
    }

    var hasSeenTour: Boolean by mutableStateOf(
        appContext.prefs().getBoolean(KEY_HAS_SEEN_TOUR, false) || appContext.prefs().getBoolean(KEY_HAS_SEEN_CORE_TOUR, false)
    )
        private set

    fun updateHasSeenTour(seen: Boolean) {
        hasSeenTour = seen
        hasSeenCoreTour = seen
        appContext.prefs().edit {
            putBoolean(KEY_HAS_SEEN_TOUR, seen)
            putBoolean(KEY_HAS_SEEN_CORE_TOUR, seen)
        }
    }

    var hasSeenCoreTour: Boolean by mutableStateOf(
        appContext.prefs().getBoolean(KEY_HAS_SEEN_CORE_TOUR, false) || appContext.prefs().getBoolean(KEY_HAS_SEEN_TOUR, false)
    )
        private set

    fun updateHasSeenCoreTour(seen: Boolean) {
        hasSeenCoreTour = seen
        hasSeenTour = seen
        appContext.prefs().edit {
            putBoolean(KEY_HAS_SEEN_CORE_TOUR, seen)
            putBoolean(KEY_HAS_SEEN_TOUR, seen)
        }
    }

    var hasSeenSettingsTour: Boolean by mutableStateOf(
        appContext.prefs().getBoolean(KEY_HAS_SEEN_SETTINGS_TOUR, false)
    )
        private set

    fun updateHasSeenSettingsTour(seen: Boolean) {
        hasSeenSettingsTour = seen
        appContext.prefs().edit { putBoolean(KEY_HAS_SEEN_SETTINGS_TOUR, seen) }
    }

    var hasSeenScopeTour: Boolean by mutableStateOf(
        appContext.prefs().getBoolean(KEY_HAS_SEEN_SCOPE_TOUR, false)
    )
        private set

    fun updateHasSeenScopeTour(seen: Boolean) {
        hasSeenScopeTour = seen
        appContext.prefs().edit { putBoolean(KEY_HAS_SEEN_SCOPE_TOUR, seen) }
    }

    var hasSeenImportTour: Boolean by mutableStateOf(
        appContext.prefs().getBoolean(KEY_HAS_SEEN_IMPORT_TOUR, false)
    )
        private set

    fun updateHasSeenImportTour(seen: Boolean) {
        hasSeenImportTour = seen
        appContext.prefs().edit { putBoolean(KEY_HAS_SEEN_IMPORT_TOUR, seen) }
    }

    var hasSeenEntryTour: Boolean by mutableStateOf(
        appContext.prefs().getBoolean(KEY_HAS_SEEN_ENTRY_TOUR, false)
    )
        private set

    fun updateHasSeenEntryTour(seen: Boolean) {
        hasSeenEntryTour = seen
        appContext.prefs().edit { putBoolean(KEY_HAS_SEEN_ENTRY_TOUR, seen) }
    }

    var hasSeenReportsTour: Boolean by mutableStateOf(
        appContext.prefs().getBoolean(KEY_HAS_SEEN_REPORTS_TOUR, false)
    )
        private set

    fun updateHasSeenReportsTour(seen: Boolean) {
        hasSeenReportsTour = seen
        appContext.prefs().edit { putBoolean(KEY_HAS_SEEN_REPORTS_TOUR, seen) }
    }

    /**
     * Marks every tour unseen again, so each one replays on its next visit.
     *
     * One call rather than seven, and one `edit` block rather than seven commits — a partially
     * reset set of flags would leave the user halfway through an onboarding they asked to restart.
     */
    fun resetAllTours() {
        hasSeenTour = false
        hasSeenCoreTour = false
        hasSeenSettingsTour = false
        hasSeenScopeTour = false
        hasSeenImportTour = false
        hasSeenEntryTour = false
        hasSeenReportsTour = false
        appContext.prefs().edit {
            putBoolean(KEY_HAS_SEEN_TOUR, false)
            putBoolean(KEY_HAS_SEEN_CORE_TOUR, false)
            putBoolean(KEY_HAS_SEEN_SETTINGS_TOUR, false)
            putBoolean(KEY_HAS_SEEN_SCOPE_TOUR, false)
            putBoolean(KEY_HAS_SEEN_IMPORT_TOUR, false)
            putBoolean(KEY_HAS_SEEN_ENTRY_TOUR, false)
            putBoolean(KEY_HAS_SEEN_REPORTS_TOUR, false)
        }
    }

    /**
     * Display name for the local (not-yet-synced) account. Stored here rather than in the Room
     * schema for the same reason as the other preferences: it is device-local identity, not
     * audited financial data.
     */
    var accountName: String by mutableStateOf(appContext.prefs().getString(KEY_ACCOUNT_NAME, "").orEmpty())
        private set

    fun updateAccountName(name: String) {
        accountName = name
        appContext.prefs().edit { putString(KEY_ACCOUNT_NAME, name) }
    }

    fun updateTheme(mode: ThemeMode) {
        themeMode = mode
        appContext.prefs().edit { putString(KEY_THEME_MODE, mode.name) }
    }

    fun updateMotion(mode: MotionMode) {
        motionMode = mode
        appContext.prefs().edit { putString(KEY_MOTION_MODE, mode.name) }
    }

    /** Persists the language. The caller must recreate the Activity for it to take effect. */
    fun updateLanguage(newLanguage: AppLanguage) {
        language = newLanguage
        appContext.prefs().edit { putString(KEY_LANGUAGE, newLanguage.tag) }
    }
}

/**
 * The system-wide animator duration scale (Settings > Accessibility > Remove animations, or
 * Developer options). 0 means the user asked for no animation at all.
 */
@Composable
fun rememberSystemAnimationScale(): Float {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
    }
}

@Composable
fun rememberAppPreferencesState(): AppPreferencesState {
    val context = LocalContext.current
    return remember { AppPreferencesState(context) }
}
