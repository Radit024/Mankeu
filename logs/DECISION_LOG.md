# Decision Log (Architecture Decision Records)

### [DEC-035] Settings overview, focused pages and root-owned edit session
- **Date**: 2026-10-25
- **Status**: Accepted; implementation and named verification recorded in the native UI QA report.
- **Context**: Three Settings tabs mixed long profile forms, optional authentication, preferences and technical details. Moving to focused pages must preserve Back precedence, unsaved fields, tours, Help return and Activity recreation.
- **Decision**: Replace the tabs with a grouped overview and eight detail pages. MankeuApp owns saveable page, immutable six-field ProfileDraft/baseline, discard modal and Help return marker. Dirty is draft != baseline. Toolbar and system Back use the same root action; tours retain their existing modal precedence. Save normalizes once and calls existing updateProfile once. A root-hoisted SaveableStateHolder preserves page scroll across Help. Internal pages use the existing fade-through helper, respecting Reduced/Off. Tour list/target registration is observable so relocation retries on page arrival.
- **Rationale / alternatives**: Keeping the tabs retains form density. A navigation library or new repository would add ownership and persistence machinery to a small overlay. Reuse existing preferences and shell; retain Activity.recreate for theme/language as required by D-017. Authentication does not own the local draft. No search, custom-theme picker or duplicate data operations.
- **Affected components**: SettingsNavigation.kt, MankeuApp.kt, SettingsHub.kt, SettingsPreferencePages.kt, AccountSettingsScreen.kt, SpotlightTour.kt, ID/EN strings, native Settings tests.
- **Migration / rollback**: No preference-key, database or backup migration. Revert layout and matching tour/Back behavior together; preserve user data and unrelated edits.

### [DEC-034] Forest Charcoal, readable ink and separate filled actions
- **Date**: 2026-10-25
- **Status**: Accepted; supersedes the dark token values in DEC-033 without changing metric meanings.
- **Context**: The user rejected a dark identity dominated by cyan/indigo/plum and requested recognizable Arina green with secondary families aligned to light. A deep green suitable for a white-text button is too dark for primary text/icons on dark surfaces.
- **Decision**: Define all 48 Material dark roles as Forest Charcoal. Material primary ink is #8ED8AE; semantic actionContainer is #357956 with white foreground; brand remains #52B788. Extend existing MetricColors ownership to mint, terracotta, blue, lavender, sand and teal families. Preserve light Material tokens and four established light metric pairs. Categories use stable normalized-name hues rather than warning/danger roles or sorted positions. Regenerate only the five dark static Lottie derivatives, retaining exact light hashes, source archives, shapes, timing and licenses.
- **Rationale / alternatives**: One primary value cannot serve both readable dark-surface ink and subdued filled actions. Keeping two explicit roles avoids per-screen compensations. Category hue identifies a category; semantic status still needs text/icons. Six hues may collide, so no uniqueness guarantee is made. No new dependency or financial behavior.
- **Affected components**: Theme.kt, MetricColors.kt, ActionColors.kt, filled action call sites, night resources, Lottie generator/derivatives/provenance, actual-token verifier, palette tests.
- **Migration / rollback**: Visual-only; revert tokens, action helper usages and matching artwork together. No data conversion.

### [DEC-033] Separation of Financial Metric Category Accents from Status Indicators
- **Date**: 2026-10-24
- **Status**: Accepted, implemented, and verified with contrast and visual audits.
- **Context**: The financial summary metric cards on Home and across reports displayed uniform gray/white container cards, requiring users to rely strictly on small icons to distinguish between Arus Kas, Laba Bersih, Pemasukan Operasional, and Pengeluaran Operasional. Furthermore, previous ad-hoc coloring caused card backgrounds to turn red or yellow on negative/warning states, conflating category identity with financial status.
- **Decision**:
  1. Established `MetricAccent` (`CASH`, `PROFIT`, `INCOME`, `EXPENSE`) and `MetricAccentColors` in `MetricColors.kt` providing four tailored WCAG-accessible light and dark container/icon pairs:
     - **CASH**: Light `#EDF4FF` / `#315D90`, Dark `#1E2C3D` / `#A9CCF4` (Cool Blue)
     - **PROFIT**: Light `#F2EEFF` / `#66508D`, Dark `#2A2438` / `#D0BDF4` (Lavender)
     - **INCOME**: Light `#EAF6EF` / `#2D6A4F`, Dark `#1C2E24` / `#A9D8BC` (Mint Arina Green)
     - **EXPENSE**: Light `#F5F0E6` / `#76603F`, Dark `#2E281F` / `#DEC9A3` (Warm Cream / Earth)
  2. Maintained strict semantic separation between category accent and financial state:
     - Card container and icon tints represent category identity and remain stable across positive, negative, and zero values.
     - Financial figures always render in `MaterialTheme.colorScheme.onSurface` on accented cards.
     - Deficit, loss, or warning conditions are presented explicitly via `MankeuStatusBadge` (e.g. `Defisit` with warning icon) rather than re-coloring the entire category card.
  3. Extended automated visual audit in `scripts/verify_visual_system.py` with `audit_metric_accents`, verifying 32 distinct contrast pairs for nominal, label, icon, and deficit indicators across both themes.
- **Rationale / alternatives**: Overwriting category background colors on state changes creates cognitive disorientation and violates WCAG 1.4.1 (use of color alone). Dedicated category accents coupled with explicit status badges deliver rapid visual scanning while preserving audit trail clarity and accessible contrast.
- **Affected components**: `MetricColors.kt`, `SharedComponents.kt`, `verify_visual_system.py`, `strings.xml`.

### [DEC-032] Mankeu Navigation and Section Motion Architecture Across Motion Modes
- **Date**: 2026-10-24
- **Status**: Accepted, implemented, and verified with test suite and design checks.
- **Context**: The application required a standardized, polished motion system for navigation, tab changes, book mode switches (`Realisasi` vs `Proyeksi`), scope boundaries, overlays, settings tabs, help center, dynamic forms, lazy list mutations, and import/transfer stages. Previously, navigation lacked a consistent transition contract, causing vertical drift between tabs, clashing motions between cards and screens, draft loss during tab switches, and unhandled accessibility states for users requiring reduced or disabled motion.
- **Decision**:
  1. Implemented `MankeuTransitions` and transition helpers in `ContentTransitions.kt`:
     - **Fade-Through**: Sequential fade-out (90ms) followed by fade-in (180ms, 270ms total) with no spatial drift, standardized for primary destination switches.
     - **Shared-Axis X**: Directional horizontal travel (16dp or 24dp) + fade (320ms/180ms) for book modes, reports tabs, settings tabs, and help center list-to-article navigation.
     - **Shared-Axis Z**: Scale (0.97x) + vertical shift + fade for overlay sheets (Data, Settings, Help).
     - **Crossfade**: Clean 90ms fade for status boundaries, import stages, and hero card data changes.
     - **Item Motion**: `LazyItemScope.itemMotion` integration for lazy lists with 180ms fade and placement tweens.
     - **Conditional Form Transitions**: `conditionalEnter` and `conditionalExit` for expandable form fields.
  2. Enforced strict 3-tier motion levels via `MankeuMotion`:
     - **Full (Expressive)**: Expressive transitions with spatial displacement, scaling, sequential fades, and placement tweens.
     - **Reduced**: Fade-only transitions (max 140ms); all spatial travel, expansion, scaling, and placement disabled.
     - **Off**: Instant 0ms transitions via `EnterTransition.None togetherWith ExitTransition.None`.
  3. Preserved state and draft restoration:
     - Implemented `rememberSaveableStateHolder` in `AccountSettingsScreen` to preserve unsaved form drafts across tab switches.
     - Search query and list scroll state preserved when navigating into and out of Help articles.
     - Unpacked `(currentDest, currentMode)` in `AnimatedContent` ensuring outgoing screens render their own consistent mode context during transitions without number interpolation.
- **Rationale / alternatives**: Standardizing motion via pure Jetpack Compose Animation primitives without adding third-party animation libraries keeps the app lightweight, robust, accessible, and compliant with Android design guidelines.
- **Affected components**: `ContentTransitions.kt`, `MankeuApp.kt`, `HomeScreen.kt`, `ReportsScreen.kt`, `AccountSettingsScreen.kt`, `HelpUi.kt`, `EntryScreen.kt`, `StockSection.kt`, `MoreScreen.kt`, `SharedComponents.kt`, `MankeuSheet.kt`, `UX_AND_ACCESSIBILITY.md`.

### [DEC-031] Strict Semantic Color Protocol: Reserving Red Strictly for Danger States & Standardizing Mode Selector to Arina Green
- **Date**: 2026-10-23
- **Status**: Accepted, implemented, and verified with test suite and design checks.
- **Context**: The user requested that the "Proyeksi" selector button in the header bar not be colored red, and that red color across the entire application be strictly audited and reserved only for actual danger/destructive states (e.g. deletion, irreversible data loss, deficits/losses). Previously, "Proyeksi" mode in `SingleChoiceSegmentedButtonRow` used `secondaryContainer` (`#FCDACF` reddish terracotta/salmon), which is identical to `errorContainer` and caused the active projection mode pill to appear as an error/danger button. Furthermore, other non-danger components such as `TourOfferCard` in `HelpUi.kt`, `MetricTone.DANGER` on standard operational expense cards on `HomeScreen`, `RecordFab`'s "Catat Pengeluaran" button, and `WelcomeScreen`'s bookkeeping notebook artwork used red/terracotta tints.
- **Decision**:
  1. Standardized `SingleChoiceSegmentedButtonRow` in `MankeuApp.kt`: both "Realisasi" and "Proyeksi" modes now use `activeContainerColor = MaterialTheme.colorScheme.primaryContainer` and `activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer`. Selection is communicated cleanly via active container fill, checkmark icon, and bold font weight, eliminating the terracotta/red color.
  2. Changed `TourOfferCard` in `HelpUi.kt` to `primaryContainer` and `onPrimaryContainer` from `secondaryContainer`, presenting onboarding tour invitations in fresh Arina green rather than error red.
  3. Corrected `summary.operatingExpenseRupiah` on `HomeScreen.kt` from `MetricTone.DANGER` to `MetricTone.PRIMARY`. Standard operational expenditures are normal business transactions; only negative financial states (net profit < 0 or operating cash flow < 0) now invoke `MetricTone.DANGER`.
  4. Changed "Catat Pengeluaran" FAB button in `RecordFab.kt` from `semantic.dangerContainer` to `MaterialTheme.colorScheme.surfaceContainerHigh` and `onSurface`, removing alarming red styling from everyday expense entry.
  5. Changed decorative notebook line in `WelcomeScreen.kt` from `colors.secondary` to `colors.primary`.
  6. Preserved red (`MaterialTheme.colorScheme.error` / `semantic.danger`) strictly for actual danger sections: destructive deletions (trash icon on businesses, delete transaction confirmations), financial loss/deficit highlights (negative cash flow, sell-at-a-loss warnings), over-budget variances, and form validation errors.
- **Rationale / alternatives**: Red in mobile UX universally signifies danger, errors, or financial deficits. Applying red/terracotta to informational mode toggles, guided tour offers, or regular expenses induced unnecessary anxiety and confused users. Standardizing non-danger sections to brand green and neutral tones while strictly isolating red to danger/destructive interactions aligns with human interface guidelines and user intent.
- **Affected components**: `MankeuApp.kt`, `HelpUi.kt`, `HomeScreen.kt`, `RecordFab.kt`, `WelcomeScreen.kt`.

### [DEC-030] 3-Tab Settings Architecture and Offline-First User Profile Parity with Arina Agri Web App
- **Date**: 2026-10-23
- **Status**: Accepted, implemented, and verified with complete test suite.
- **Context**: The user requested an examination of the settings pages and architecture in the Arina Agri web application (`arina-agri`) and the implementation of its settings systems and features into the Android mobile app. The web application organizes settings cleanly into three distinct tabs: **Profil** (Full name, WhatsApp notification number, Telegram ID, Location/Village, Main Commodity, Land Area, and profile photo/identity), **Umum** (Theme appearance, language, UI preferences), and **Informasi Sistem** (Application specs, offline local storage environment, architecture, and developer contact). Previously, the mobile app rendered all settings in an unstructured single vertical list with only local name and basic preferences.
- **Decision**:
  1. Re-architected `SettingsScreen` in `AccountSettingsScreen.kt` into a native Material 3 `PrimaryTabRow` with three canonical tabs:
     - **Tab Profil** (`SettingsTab.PROFILE`): Profile avatar/initials, full name (`accountName`), WhatsApp notification number (`whatsappPhone`), Telegram ID (`telegramContact`), Location/Village (`location`), Main Commodity (`commodity`), Land Area (`landArea`), "Simpan Perubahan" action button with visual feedback, and the Google Drive cloud synchronization status card (`TourTarget.SETTINGS_DRIVE`).
     - **Tab Umum** (`SettingsTab.GENERAL`): Display & theme picker (System, Light, Dark) (`TourTarget.SETTINGS_DISPLAY`), language picker (ID, EN), motion & animation scale (Full, Reduced, Off), touch feedback toggles (haptics, sound), and interactive page guides & tour offers (`TourTarget.SETTINGS_GUIDES`).
     - **Tab Sistem** (`SettingsTab.SYSTEM`): Arina Agri app banner and version badge, 100% Offline-First & Privacy architecture guarantee card, technical specification table (Android Native, Jetpack Compose Material 3, Room SQLite v10 with AES-256 GCM encryption), contact developer button (mailto intent), and animation licenses.
  2. Maintained Room financial schema purity per invariant rules: stored profile contact and operational metadata (`whatsapp_phone`, `telegram_contact`, `profile_location`, `profile_commodity`, `profile_land_area`) in device-local `AppPreferences` (SharedPreferences) rather than polluting the audited financial database.
  3. Created `com.mankeu.domain.ProfileContactUtils` with pure domain normalization functions for WhatsApp phone numbers (leading 0 to standard country code format) and Telegram handles (@username or numeric chatId).
  4. Synchronized interactive tour step changes (`activeTourStep`) from `SpotlightTour` in `MankeuApp.kt` to automatically navigate between tabs during guided tours (steps 1 & 2 in General, step 3 in Profile) without breaking layout or tour spotlight bounds.
- **Rationale / alternatives**: Adopting the web application's 3-tab model provides immediate visual clarity, domain consistency, and complete feature parity while respecting Android-native Material 3 design patterns and local-first architectural invariants.
- **Affected components**: `AccountSettingsScreen.kt`, `ProfileContact.kt`, `AppPreferences.kt`, `MankeuApp.kt`, `values/strings.xml`, `values-en/strings.xml`, `ProfileContactTest.kt`.

### [DEC-029] Standardized Primary Action Button Green (#2D6A4F) and Strict Semantic Color Usage (Red for Danger Only, Yellow for Alerts)
- **Date**: 2026-10-21
- **Status**: Accepted, implemented, and verified with complete test suite and lint.
- **Context**: The user requested that all action buttons use the solid Arina brand green (`#2D6A4F` light / `#52B788` dark) shown in the reference image ("Tambah Usaha"), red be strictly reserved for danger/destructive actions only (e.g. deletion, discarding data), and yellow/amber be used for alerts/warning states. Previously, `FilledTonalButton` in several screens resolved to `secondaryContainer` (`#FCDACF` terracotta/pinkish-coral); `EntryScreen`'s "Perlu Unit Usaha" banner used `error` (red) for both its container and the "Tambah Unit" action button (violating semantic rules); `MankeuSheet` only supported `destructive` (red) with no warning state; and `NavigationBarItem`/`NavigationRailItem`/`FilterChip`/`SegmentedButton` defaulted to peachy terracotta indicators.
- **Decision**:
  1. Replaced all 8 `FilledTonalButton` invocations across `ManageScopeScreen` (Tambah Usaha, Tambah Unit), `StockSection` (Kelola Stok), `PlanCards` (Buka Rencana Usaha), `MoreScreen` (Cadangkan ke Drive, Masuk Google), and `AccountSettingsScreen` (Buka Pusat Bantuan, Kirim Masukan) with solid Arina green `Button` (`#2D6A4F` light / `#52B788` dark, `onPrimary = #FFFFFF`).
  2. Corrected `EntryScreen`'s "Perlu Unit Usaha" banner: converted the card container to `semantic.warningContainer`, icon tint to `semantic.warning`, title & body text to `semantic.onWarningContainer`, and changed "Tambah Unit" button from red to solid Arina primary green `Button(onClick = onOpenData)`.
  3. Enhanced `MankeuSheet` to support `warning: Boolean = false`: when `destructive = true` uses `colorScheme.error` (red, danger), when `warning = true` uses `semantic.warning` (amber/yellow, alert), and defaults to `colorScheme.primary` (Arina green).
  4. Styled `NavigationBarItem` and `NavigationRailItem` in `MankeuApp.kt` with `indicatorColor = MaterialTheme.colorScheme.primaryContainer`, `selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer`, and `selectedTextColor = MaterialTheme.colorScheme.primary` to display soft mint green indicators.
  5. Styled `FilterChip` and `SegmentedButton` active states to use `primaryContainer` and `onPrimaryContainer` across `EntryScreen`, `StockSection`, `MoreScreen`, `PlanScreen`, and `AccountSettingsScreen`.
- **Rationale / alternatives**: Strictly aligns with the user's color hierarchy: green for positive/action buttons, red reserved exclusively for irreversible destructive actions, and yellow/amber for warnings and alerts.
- **Affected components**: `ManageScopeScreen.kt`, `StockSection.kt`, `PlanCards.kt`, `MoreScreen.kt`, `AccountSettingsScreen.kt`, `EntryScreen.kt`, `MankeuSheet.kt`, `MankeuApp.kt`, `PlanScreen.kt`, `HomeScreen.kt`.

### [DEC-028] Unified First-Use & No-Unit Empty States Across All Primary Destinations
- **Date**: 2026-10-21
- **Status**: Accepted, implemented, and verified with test suite and lint.
- **Context**: When no business or project/unit had been created yet, Catatan (EntryScreen/PlanScreen), Stok (StockScreen), and Laporan (ReportsScreen) displayed inconsistent, fragmented layouts: Catatan rendered an error container banner above an empty ledger card with an active record button; Stok only checked business existence and omitted unit existence; and Laporan rendered full report tabs and a report empty card prompting the user to record transactions that could not yet be saved.
- **Decision**:
  1. Aligned all 4 primary destinations (Beranda, Catatan, Stok, Laporan) with the canonical first-use empty state UX established by HomeScreen:
     - When state.businesses.isEmpty(): render full-height centered EmptyCard with R.string.setup_business_title, R.string.setup_business_body, action R.string.scope_add_business (onOpenData), HelpArticle.START_SCOPE, and StateArtwork.BUSINESS_PLAN (144 dp).
     - When state.projects.isEmpty(): render full-height centered EmptyCard with R.string.setup_project_title, R.string.setup_project_body, action R.string.scope_add_project (onOpenData), HelpArticle.START_SCOPE, and StateArtwork.BUSINESS_PLAN (144 dp).
  2. Updated MankeuApp.kt screen routing to propagate hasProjects = state.projects.isNotEmpty() to StockScreen, onOpenData to ReportsScreen, and expanded across all screens.
  3. Added instrumented regression tests to ContextualLottieTest.kt verifying both first-use (no business) and no-unit (has business, no unit) states across screens.
- **Rationale / alternatives**: Guarantees visual and functional consistency across the entire app so users are always clearly guided to create their business and unit first, avoiding confusing error banners or dead-end record buttons.
- **Affected components**: EntryScreen.kt, PlanScreen.kt, StockSection.kt, ReportsScreen.kt, MankeuApp.kt, ContextualLottieTest.kt.

### [DEC-027] Enforced Explicit Artwork Selection and Contextual Variety Across Empty States
- **Date**: 2026-10-21
- **Status**: Accepted, implemented, and verified with complete test suite.
- **Context**: Several empty states (e.g. first-use Home without business vs transaction ledger without entries) previously defaulted to the generic StateArtwork.EMPTY document illustration. In addition, SharedComponents.kt's EmptyCard had a default argument (artwork: StateArtwork = StateArtwork.EMPTY), allowing screen call sites to omit the contextual illustration role and resulting in monotonous empty state visuals across unrelated screens.
- **Decision**:
  1. Removed default argument from EmptyCard signature (artwork: StateArtwork required), allowing the Kotlin compiler to strictly enforce intentional artwork selection at all call sites.
  2. Mapped all 11 screen call sites and test fixtures explicitly to their semantic roles:
     - Onboarding & Scope Setup: Beranda (no business, no project/unit), Kelola Usaha (empty business, empty project), Stok (no business), and Proyeksi (no plan) mapped to StateArtwork.BUSINESS_PLAN (144 dp).
     - Financial Ledger & Search: Catat Transaksi (empty ledger, zero search/filter match) and VisualSystemTest mapped to StateArtwork.EMPTY (88 dp).
     - Financial Reports: Laporan (empty report) mapped to StateArtwork.REPORTS (88 dp).
  3. Preserved all existing user flows, buttons (Tambah Usaha, Tambah Unit, Catat Sekarang, Buka Rencana), Help Center deep links (learn = HelpArticle), and full-height centered card layouts.
- **Rationale / alternatives**: Compiler-enforced parameter prevents future regressions when new screens are built. Reusing existing bundled, verified Lottie assets introduces 0 additional bytes or licenses while cleanly differentiating first-use setup from an empty transaction list.
- **Affected components**: SharedComponents.kt, HomeScreen.kt, ManageScopeScreen.kt, StockSection.kt, EntryScreen.kt, ReportsScreen.kt, VisualSystemTest.kt, ContextualLottieTest.kt.

### [DEC-026] Contextual, Offline LottieFiles Artwork with Explicit Asset Licensing
- **Date**: 2026-10-21
- **Status**: Accepted; user-confirmed application of the five browser-reviewed candidates.
- **Context**: A generic empty illustration was repeated across unrelated financial workflows. The installed native Lottie Compose dependency already owns rendering and motion preferences.
- **Decision**: Extend StateArtwork/StateIllustration and let screen call sites select the relevant role. Add Business Plan, Growth Chart, Cardboard Box, Upload File and Cloud Data Backup only to their intended contexts. Ship deterministic light/dark JSON from pinned source archives, using the app theme rather than OS-only night qualifiers. Keep the Lottie Simple License and creator credits bundled and readable offline in About; these derivatives are not relicensed as GPL.
- **Motion**: Play decoration once, pause when outside the visible window or below STARTED lifecycle, and hold an explicit nonblank frame after completion or in Reduced/Off. Do not assume third-party closing/reset frames remain visible. Retain native icon fallback and semantic status text.
- **Alternatives**: Runtime CDN downloads violate offline behavior; a WebView adds unnecessary runtime complexity; creating a new animation component for each screen duplicates playback policy. A rising chart does not indicate actual profit; cloud imagery must never imply that a local save or Google sign-in created a backup.
- **Affected components**: StateIllustration, EmptyCard, PlanScreen, ReportsScreen, StockSection, MoreScreen import/cloud backup step, About license notice, ten raw JSON resources, source/provenance checks and native tests. No dependency, financial schema, calculation or backup/import contract change.

### [DEC-025] Comprehensive Performance Optimization Across Compose, Dispatchers & Build Configuration
- **Date**: 2026-10-21
- **Status**: Accepted, implemented and verified with full Gradle test suite.
- **Context**: The mobile app exhibited heavy lag, UI stutter during scrolling, and cold-start delays. Root-cause analysis revealed that: (1) heavy domain calculations (PlanCalculator, ActualBepCalculator, accounting summary) ran synchronously on Dispatchers.Main.immediate; (2) un-remembered monthlyCashFlow in ReportsScreen caused the Vico cartesian chart to continuously re-layout and re-render on every frame; (3) Modifier.tourTarget executed onGloballyPositioned and mutated Compose snapshot state during every scroll frame even when no tour was active; (4) search filtering in EntryScreen and inventory calculations in StockSection allocated intermediate objects on every recomposition; (5) AppViewModel initialized selectedBusinessId and selectedProjectId as null, causing a 4-tier Room query cascade on cold start; and (6) release packaging failed due to missing root google-services.json, forcing debug build usage with isMinifyEnabled = false.
- **Decision**:
  1. Offload domain calculations: added .flowOn(Dispatchers.Default) to planContent, planAndActual, and accountingSummary.
  2. Memoize ReportsScreen monthlyCashFlow: wrapped with remember(state.cashFlowRecords, plan) to stabilize Vico chart model producer.
  3. Optimize search filtering: wrapped visibleEvents in EntryScreen with remember(state.events, eventsById, search, direction) and replaced intermediate collections with short-circuit boolean checks.
  4. Optimize stock row economics: replaced StockCalculation.computeInventoryValue(listOf(item)) with direct TransactionCalculation.computeTotalRupiah.
  5. Gate tour coordinates during scroll: added isActive flag to TourTargetRegistry and equality checks in onGloballyPositioned, avoiding snapshot state mutation during normal scrolling.
  6. Prevent cold-start cascade: initialized selectedBusinessId and selectedProjectId with rememberedSelection.
  7. Enable R8 minification and provide root google-services.json for production builds.
- **Rationale / alternatives**: Offloading calculations to background threads and eliminating recomposition/snapshot churn guarantees smooth 60/120 FPS rendering and eliminates startup latency without modifying financial math formulas.
- **Affected components**: AppViewModel.kt, ReportsScreen.kt, EntryScreen.kt, StockSection.kt, SpotlightTour.kt, FinanceRepository.kt, app/build.gradle.kts, app/google-services.json.

### [DEC-024] Complete Arina Agri Design System & Semantic Color Tokens Alignment on Mobile
- **Date**: 2026-10-21
- **Status**: Accepted, implemented and verified with scripts/verify_visual_system.py (284 checks PASS).
- **Context**: The web application (rina-agri) features a comprehensive design system documented in lib/theme.ts, lib/themeColors.ts, lib/weatherTokens.ts, StatusBadge.tsx, and docs/specs/font-dan-color-palette.md. While the mobile application had already adopted core primary and surface tokens, it lacked formal alignment for warning and info semantic tokens, agricultural domain category colors (pestisida, pupuk, tenaga, irigasi, alat), weather condition gradients, and the canonical StatusBadge capsule style.
- **Decision**: 
  1. Extended MankeuSemanticColors in Theme.kt with warning, onWarning, info, onInfo, infoContainer, onInfoContainer, and helper methods softContainer, softContent, and softBorder matching 	hemeColors.ts from the webapp.
  2. Implemented 
esolveAgricultureCategoryColor(category) in Theme.kt matching the exact domain palette mappings in rina-agri/components/dashboard/DashboardCharts.tsx: Pestisida -> Danger (Terracotta), Pupuk -> Success (Green), Tenaga Kerja -> Info (Sage), Irigasi -> Primary (Forest), Alat/Mesin -> Warning (Amber), and mapped HomeScreen's ExpenseDistributionCard to it.
  3. Added ArinaWeatherTokens providing atmospheric 3-stop gradients for sunny, cloudy, and rainy conditions across light and Slate Garden dark modes, aligned with weatherTokens.ts.
  4. Added MankeuStatusBadge to SharedComponents.kt with soft container, soft border, and WCAG-accessible text, and updated MetricTone to support all semantic roles (WARNING, INFO).
  5. Refactored FeasibilityStatusChip, event reversal badges, and low-stock count badges to use MankeuStatusBadge.
  6. Added all semantic brand tokens to alues/colors.xml and alues-night/colors.xml, and expanded scripts/verify_visual_system.py to assert XML presence and semantic contrast pairs.
- **Rationale / alternatives**: Eliminates ad-hoc colors across screens, guarantees 100% visual consistency between web and mobile products, and maintains strict WCAG AA contrast standards.
- **Affected components**: alues/colors.xml, alues-night/colors.xml, Theme.kt, SharedComponents.kt, PlanCards.kt, HomeScreen.kt, StockSection.kt, scripts/verify_visual_system.py.

### [DEC-023] Dynamic Splash Screen Theming Synced with App Settings & Android 12+ UiModeManager
- **Date**: 2026-10-21
- **Status**: Accepted, owner-confirmed bug fix and UX improvement.
- **Context**: The splash screen on cold start was previously detached from the user\'s in-app theme selection (ThemeMode in Settings: System, Light, Dark). When the user\'s device was in system dark mode, Android OS WindowManager rendered a dark splash screen even if the user explicitly chose Light mode in the app settings, causing a jarring transition.
- **Decision**: Synchronize the in-app theme selection with the Android OS night mode using UiModeManager.setApplicationNightMode(mode) on Android 12+ (API 31+). This persists the per-app night mode setting at the OS level so WindowManager renders the splash screen matching the user\'s selected theme (Light, Dark, or System) before the process starts. In ttachBaseContext, apply uiMode configuration override to ensure pre-API-31 and framework resources resolve to the matching theme. In alues/ and alues-night/, ensure splash_screen_background and splash_background match their respective surfaces (#FAFAF8 for Light, #1E2620 for Dark) and rina_brand_green has WCAG-compliant contrast in both themes.
- **Rationale / alternatives**: Hardcoding the splash screen to light mode broke dark mode users; only using Compose state failed because the Android OS splash screen renders before Compose is initialized. UiModeManager.setApplicationNightMode is Android\'s official API specifically created to solve this problem.
- **Affected components**: AppPreferences.kt, MainActivity.kt, MankeuApplication.kt, MankeuApp.kt, alues/themes.xml, alues-night/themes.xml, alues/colors.xml, alues-night/colors.xml, scripts/verify_visual_system.py, VisualSystemTest.kt.

### [DEC-022] Scope Restriction: Web App (arina-agri) as Read-Only Asset & Reference Source
- **Date**: 2026-10-21
- **Status**: Accepted, user-confirmed project scope invariant.
- **Context**: The workspace environment contains both the native Android Kotlin repository (`Mankeu`, branded as Arina Agri) and the Next.js web application repository (`arina-agri`). Without explicit boundaries, automated agents or developers could unintentionally modify the web codebase or confuse web patterns with native Android architecture.
- **Decision**: Formally establish that all active development, feature building, bug fixing, and test execution are strictly dedicated to the native Android mobile application in Kotlin (`d:\Arina Agri\Mankeu`). The sibling folder `d:\Arina Agri\arina-agri` is strictly designated as a read-only source for extracting assets (logos, SVGs, icons, color tokens) and reviewing reference material (financial calculations, Excel golden templates, copywriting, UX flows). Direct code changes, builds, or feature additions in `arina-agri` are strictly prohibited unless explicitly requested in a dedicated task.
- **Rationale / alternatives**: Prevents cross-repository pollution, accidental drift into web stack tooling, and ensures 100% of development effort strengthens the Kotlin / Jetpack Compose mobile product.
- **Affected components**: `AGENTS.md`, `.agents/rules/workspace_scope.md`, `../arina-agri/AGENTS.md`, engineering guidelines.

### [DEC-021] Optional First-Run Arina Welcome
- **Date**: 2026-10-21
- **Status**: Accepted, user-confirmed design and implementation request.
- **Context**: The owner requested a dedicated mobile welcome/login page following the reviewed Money Manage composition and Wise hierarchy. Local operation must remain independent of identity and network.
- **Decision**: Reuse the existing Google launcher and auth state. Store welcome completion in AppPreferences, not the financial schema. Empty installations see welcome once; offline entry and successful authentication finish it. Existing businesses and authenticated users bypass it. Cancellation/failure never traps the owner, and offline entry remains enabled during sign-in. Account settings remains available for later sign-in.
- **Rationale / alternatives**: A mandatory login would violate local-first requirements. Showing welcome every launch would interrupt bookkeeping. A new authentication provider or password form adds unnecessary scope. Sign-in must not imply automatic backup or active sync.
- **Affected components**: AppPreferences, MankeuApp, WelcomeScreen, localized strings, OperationSnackbarHost positioning, UX guide and WelcomeScreenTest. No database migration, new dependency, provider account, server or backup format change.

### [DEC-018] Offered, Versioned Page Tours and an In-App Help Center with "Show Me"
- **Date**: 2026-10-21
- **Status**: Accepted (owner, grill Q1–Q17); implemented and tested on the emulator.
- **Context**: The tours described removed mechanics (Riwayat tab, "Belum Lunas" credit filter, "Ubah Asumsi" calculator), Stok and Rencana Usaha had none, and there was no help beyond replaying a tour. Owners range widely in tech literacy; help must be thorough without getting in the way.
- **Decision**:
  1. Page tours stay per page (Beranda, Catatan, form catat, Rencana Usaha, Stok, Laporan, Berkas & Data, Usaha/Unit, Pengaturan), with one or two everyday sentences per step and "Pelajari lebih lanjut" to the matching article.
  2. A tour is **offered**, never started on its own: a small card "Lihat panduan (N langkah)" with Lihat / Nanti (until next launch) / Jangan tawarkan lagi; Pengaturan has the switch and "Ulangi semua panduan".
  3. Each tour has a version (`Tour.version`); the seen version is stored per tour, old boolean flags count as version 1. Raising a version re-offers only that tour.
  4. The ? button opens a sheet: replay this page's tour, articles about this page, open Pusat Bantuan (`Overlay.HELP`): task-first articles grouped Mulai / Mencatat / Merencanakan / Stok / Laporan / Data / Kenapa…?, local search, numbered steps, a screenshot, and "Tunjukkan di aplikasi" which opens the right page, book mode or overlay and highlights the element with the tour spotlight. Without an usaha/unit it opens Usaha dan Unit and says why. Empty states link "Pelajari caranya".
  5. Text ID+EN from one source (`docs/guide/help_strings.py` → GUIDE block in strings.xml). Screenshots: Indonesian, light theme, fictional data, generated by `HelpScreenshotTest` through the same "Tunjukkan" path (`scripts/capture_help_screenshots.ps1`), 540 px WebP in `res/drawable-nodpi` (kept from shrinking by `res/raw/keep.xml`). `GuidesTest` fails when an article's screenshot is missing.
  6. Spotlight targets in lazy lists: the page registers its `LazyListState`; a step whose target is not composed scrolls the page until it is. Target bounds are dropped when the target leaves composition. A target on a non-default tab is not used (the BEP status article points at the same card on Beranda).
- **Alternatives considered**: auto-starting tours with Skip (rejected by owner as intrusive); live component previews instead of screenshots (more code, kept for later); per-article bespoke navigation code (rejected: one `ShowSpec` per article covers every case).
- **Affected components**: `presentation/guide/` (new), `components/SpotlightTour.kt`, `AppPreferences`, `MankeuApp`, screens (targets, list state, empty states), `AccountSettingsScreen`, strings ID/EN, `res/drawable-nodpi/help_*.webp`, tests.

### [DEC-017] Seasonal Products, Per-Unit Stock and Reversible Plan Period (Schema 10)
- **Date**: 2026-10-21
- **Status**: Accepted (agent; the owner delegated technical decisions and asked for real-flow tests). Implemented and verified on the emulator (`docs/audit/2026-10-21_UJI_SCHEMA10.md`).
- **Context**: Open items 1–5 of the cross-business test (`docs/audit/2026-10-21_UJI_ALUR_LINTAS_USAHA.md`). A chilli harvested in month 3 was planned as selling from month 1, so capital need was understated; reversing an import left the plan period it had filled in; stock belonged to the business, so one branch's stock appeared and was exported in every unit; the selection was forgotten on restart; import counts included skipped rows.
- **Decision**:
  1. `products.sellFromMonth` (1 = first plan month). A product sells in s = n − start + 1 of the n plan months. The cash schedule uses the real months, so capital need sees the waiting months. "Per month" revenue, cost, profit and BEP rupiah are averages over the period (weight target × s), so profit/month × n = period profit. Unit BEP per product is per **selling** month: FC × target × n ÷ Σ(target × s × CM). With every product selling from month 1 the formulas are exactly the previous ones. A product starting after the period sells nothing and has no BEP. Screens, PDF and Excel say so when a plan is seasonal.
  2. Template v4 adds the optional column "Mulai Dijual (bulan ke-)" on Produk. Optional columns (`MankeuTemplate.OPTIONAL`) may be absent (v3 files still import) but, when present, must carry the exact header. This is the only relaxation of DEC-012's exact-header rule.
  3. An import batch records the period it filled in (`appliedStart/EndEpochDay`); reversal clears the unit's period only if it is still exactly that one.
  4. `stock_items.projectId`: a unit sees its own items plus shared ones (NULL). New items default to the selected unit; "Semua unit" makes them shared. Names are unique within what one unit can see. Import adds stock to the selected unit (DEC-012 bulk entry). Existing stock stays shared on upgrade. A unit holding its own stock is archived, never deleted, so the stock cannot be orphaned. Partly supersedes DEC-010 ("stock belongs to the business").
  5. `import_batches.incomingRupiah` so the batch shows money in and out separately; batch count, result message and preview button all count only rows actually added.
  6. The selected business and unit are kept in SharedPreferences (device UI state, not accounting data) and validated on start.
- **Alternatives considered**: per-product monthly target table (rejected: more input than a start month; seasons with gaps can be modelled as one-off lines until asked for); guessing the branch of existing stock from import batches (rejected: wrong for hand-entered items); requiring the new template column (rejected: breaks every v3 file the owner already has).
- **Affected components**: `domain/BusinessPlan.kt`, `domain/StockManagement.kt`, `data/` (entities, DAO, `AppDatabase.MIGRATION_9_10`, `FinanceRepository`), importer/exporters (template v4, PDF), `presentation/` (PlanScreen, StockSection, PlanCards, MoreScreen, AppViewModel, AppPreferences, MankeuApp), `MainActivity`.

### [DEC-016] Break-Even Base, Capital Need and Plan-vs-Actual Basis
- **Date**: 2026-10-21
- **Status**: Accepted (implementation, found by cross-business simulations).
- **Context**: Simulations of a farm, laundry, shop and tractor rental showed three definitional gaps: variable costs outside recipes were in profit but not in break-even (profit was negative at "BEP"); capital need was netted against planned capital, so planning capital hid the need; and Realisasi vs Target compared a 12-month plan total with one month of actuals.
- **Decision**: (1) Break-even base = fixed cost + variable costs not in any recipe − other income; profit is exactly zero there. (2) Capital need = deepest cumulative deficit of operations and investment before capital; planned capital and the remaining shortfall are shown next to it. (3) Plan-vs-actual compares averages per month (plan months vs recorded actual months), on a cash basis; the RAB total row is removed. (4) Mix BEP uses one exact division, FC × target × P ÷ Σ(target × CM). (5) The plan period travels in the template (`Petunjuk` row "Periode rencana") and fills a unit that has none.
- **Alternatives considered**: allocating non-recipe variable costs to units (rejected: needs an allocation key the owner never gave); comparing totals over the overlapping months only (rejected: plans are not split by calendar month yet).
- **Affected components**: `domain/BusinessPlan.kt`, `PlanCards.kt`, `ReportsScreen.kt`, exporters, importer, `FinanceRepository.commitImport`, template v3 guide sheet.

### [DEC-015] Sync Triggers Cover Only Tables That Exist at Migration Time
- **Date**: 2026-10-21
- **Status**: Accepted (implementation rule, found by `MigrationTest`).
- **Context**: `MIGRATION_7_8` calls `SyncSchema.createTriggers`, which walked today's `AppDatabase.DATA_TABLES`. Schema 9 added `products`, so a device upgrading 7→9 in one go crashed in the 7→8 step creating a trigger on a table that did not exist yet.
- **Decision**: `createTriggers` creates triggers only for `DATA_TABLES` present in `sqlite_master`. Every migration that adds or rebuilds a data table calls it again; `MIGRATION_8_9` calls it before converting legacy rows so they reach `sync_outbox`.
- **Alternatives considered**: freezing a per-schema table list inside each old migration (rejected: one more list to keep in step, and the filter fixes every caller at once).
- **Affected components**: `sync/SyncSchema.kt`, `AppDatabase.MIGRATION_8_9`, `docs/SYNC_ARCHITECTURE.md`, `docs/DATABASE_MIGRATIONS.md`.

### [DEC-014] Business Plan with Products, Recipes and Itemised Fixed/Variable Costs Drives BEP
- **Date**: 2026-10-20
- **Status**: Accepted (owner, brainstorm); design in `docs/design/2026-10-20_RENCANA_USAHA_DAN_BEP.md`. Implemented 2026-10-21 (R-1…R-5; R-6 except a hands-on PDF check), schema 9.
- **Context**: The BEP calculator took four typed numbers that were disconnected from the RAB and transactions, so diverse businesses (several products, many cost items) could not be modelled. "Saldo kas awal" and "Modal sendiri" duplicated recordable owner-capital transactions. The lecturer's own benchmark is itemised.
- **Decision** (owner answers B-1…B-10): cost nature is FIXED/VARIABLE per category with a per-line override; multi-product with sales mix; product cards with per-unit variable-cost recipes (with yield); fixed costs entered monthly and recurring; assets enter BEP as straight-line depreciation; opening cash and owner capital leave the calculator (recorded or planned as Setoran Modal; capital need is computed from the deepest cumulative deficit); realisation BEP uses recipe VC plus a variance against actual variable spend; product and quantity are optional on sales entries; recipes do not consume stock yet; all plan inputs live on one "Rencana Usaha" page in Catatan (Proyeksi mode), while Laporan only shows results with a lecturer-format breakdown.
- **Alternatives considered**: per-category-only or per-line-only cost nature; single reference product; per-unit RAB lines tagged with a product; actual-spend VC for realisation BEP; mandatory product on sales; excluding assets (benchmark style). Rejected by the owner.
- **Consequences**: schema 9 (products, product_cost_components, cost-nature/recurrence/useful-life/productId columns, scenario_assumptions removed with data conversion); template sheets change (Produk, Resep Produk replace Parameter BEP); supersedes fix-plan F-06 and F-11.

### [DEC-013] Multi-Device Sync Framework over Google Drive (End-to-End Encrypted)
- **Date**: 2026-10-20
- **Status**: Accepted (owner); framework implemented and tested, not wired to UI or scheduler.
- **Context**: The owner asked for sync infrastructure that is "framework-ready" without shipping the feature. The earlier design (`CLOUD_SYNC_AND_AUTH.md`) deliberately limited Drive to snapshot backups. The owner chose multi-device sync, with data on Drive encrypted by the user's passphrase.
- **Decision**:
  1. Change capture by SQLite triggers on every data table into `sync_outbox`, silenced by a `sync_meta.applying_remote` flag while applying remote changes or restoring. Created in `MIGRATION_7_8` and in a Room `onCreate` callback.
  2. A change set is a `PortableBackup` (existing codec) with `_op`/`_changedAt` columns and a `_sync` header, encrypted with the existing `PasswordBackupCipher`. No new format, no new crypto.
  3. Row-level last-writer-wins on `changedAt`; UPDATE-then-INSERT on apply (never `INSERT OR REPLACE`, which would cascade-delete journals); parents first for upserts, children first for deletes; constraint-rejected rows are counted, not fatal.
  4. Transport is an interface with a Drive `appDataFolder` implementation (`appProperties.kind=sync`). Backup listing and retention ignore sync files.
  5. Restores and the schema-8 migration set `requires_full_snapshot`; the next push sends every row.
- **Alternatives considered**: Per-row `updatedAt`/tombstone columns on every table (rejected: touches every entity and every write path; triggers capture all paths for free). A bespoke JSON change format (rejected: the backup codec already handles every column type and is fuzz-hardened). Server-side merge (rejected: local-first, no app-owned server).
- **Open (owner)**: passphrase storage, sync trigger, rejected-row merging, compaction, clock skew. Listed in `docs/SYNC_ARCHITECTURE.md`.
- **Affected components**: `sync/` (new), `AppDatabase` (schema 8, `DATA_TABLES`), `EncryptedBackupManager.restore`, `GoogleDriveBackupClient`, `CLOUD_SYNC_AND_AUTH.md` (superseded section).

### [DEC-012] Single Mankeu-Owned Excel Template for Import and Export
- **Date**: 2026-10-20
- **Status**: Accepted (owner) — implementation planned as F-01 in `docs/audit/2026-10-20_RENCANA_PERBAIKAN.md`.
- **Context**: The importer supports three shapes: Mankeu export, a five-view reference workbook with deterministic enrichment, and generic sheets via fuzzy semantic matching. The blank template (`MsmeTemplateExporter`) differs from the export file. This is the main source of non-cash rows (A-02), wrong business identity (A-09) and roughly 1,200 lines of guessing logic.
- **Decision** (owner answers K-6…K-13):
  1. Mankeu provides the template; blank template = export format (one format, lossless round trip).
  2. Non-template files are rejected with the exact missing/changed sheets or columns and a download-template action; no guessing.
  3. Support for the legacy reference workbook and automatic enrichment is removed; the verification workbook is regenerated in template format.
  4. Import is bulk entry into the selected business unit; backups (.aabk) remain the way to move data.
  5. Sheets: Transaksi (cash-book columns Tanggal · Keterangan · Kategori · Pemasukan · Pengeluaran), Rencana Anggaran, Parameter BEP, Stok Barang (+ Petunjuk, Kategori reference).
  6. Unknown categories are created as custom categories and listed in the preview.
  7. Identical files may be re-imported, with a duplicate warning (replaces the current hard refusal).
- **Supersedes**: the import/enrichment invariants previously in `AGENTS.md`; `docs/REFERENCE_WORKBOOK_CONTRACT.md`, `docs/IMPORT_RECONCILIATION_AND_NOTIFICATIONS.md` and `docs/SEMANTIC_MATCHING.md` (marked superseded).
- **Alternatives considered**: two formats (simple template + full export); best-effort reading of arbitrary workbooks; keeping the reference workbook as a one-shot converter; using import for full data migration. All rejected by the owner.
- **Affected components**: `FinanceWorkbookImporter`, `ImportModels`, `SemanticMatcher`, `CategoryClassifier`, `FinanceWorkbookExporter`, `MsmeTemplateExporter`, `FinanceRepository.commitImport`, `AppViewModel.commitImport`, import UI, importer tests, verification workbook script.

### [DEC-011] Post-Audit Product Decisions: Strict Cash, Category-Driven Types, Stock-Cash Link, RAB Editor, Basic Categories
- **Date**: 2026-10-20
- **Status**: Accepted (owner) — implementation planned in `docs/audit/2026-10-20_RENCANA_PERBAIKAN.md`, not yet started.
- **Context**: The business UI/UX + system audit (`docs/audit/2026-10-20_AUDIT_UIUX_BISNIS_DAN_SISTEM.md`) found that (a) the importer and ledger still carry credit/loan/interest types and live data contains them, (b) owner capital, drawings and asset purchases have no input path, (c) stock and cash are recorded twice, (d) the RAB has no editor so Proyeksi is import-only, (e) defaults plus imports produce 23 overlapping categories.
- **Decision** (owner answers, 2026-10-20):
  1. **K-1 Strict cash**: remove every loan, debt/receivable, instalment and interest concept from code, schema, UI and importer. Non-cash workbook rows are ignored, with a persistent notice. If a user deliberately records something like a loan as a plain "Uang masuk" under a custom category, the app leaves it alone.
  2. **K-2 Category-driven types**: the form keeps two buttons; the category role (Pemasukan, Pengeluaran, Setoran modal, Ambil pribadi, Beli aset) decides `CASH_INCOME`, `CASH_EXPENSE`, `OWNER_CAPITAL`, `OWNER_DRAWING`, `ASSET_PURCHASE_CASH`.
  3. **K-3 Optional stock–cash link**: stock-in can also post the cash expense, and expense/income entries can also move stock, in one transaction. Partly supersedes DEC-010 item 4.
  4. **K-4 RAB editor** in Proyeksi mode, reusing the existing repository functions.
  5. **K-5 Basic categories only** (11 defaults); anything specific goes through the existing custom-category feature. Imports match existing categories before creating new ones.
- **Alternatives considered**: converting credit/loan rows to cash equivalents, or restoring a minimal debt ledger (both rejected by the owner in favour of strict cash). Explicit "Jenis lain" chooser in the form (rejected for category-driven mapping). Keeping stock separate, or Proyeksi as BEP parameters only (rejected).
- **Affected components**: importer, `FinanceModels`, `LedgerPolicy`, report calculators, Room schema 8 migration, `EntryScreen`, category template/manager, `StockSection`, new RAB editor, exporters, PDF.

### [DEC-010] Stok Tab (4th Destination) & Pre-Stock Backup Compatibility
- **Date**: 2026-10-20
- **Status**: Accepted & Implemented
- **Context**:
  1. `StockManagement.kt` (entities, DAO, use cases) shipped in schema 6 with no UI, so the lecturer-required minimal inventory (item master, current stock, low-stock alert, stock in/out/opname) was unreachable.
  2. Backup validation required every table in `TABLES`, including `stock_items`/`stock_movements`, so every backup from v0.0.4–v0.0.6 (schemas 3–5) was refused despite `OLDEST_RESTORABLE_SCHEMA = 3`.
  3. Obligation stubs (`ObligationView`, `obligations()`, `settleObligation`, `onSettle`) survived the pure-cash migration as dead code.
- **Decision**:
  1. Stock is per business (not per book). `FinanceRepository.stockItems(businessId)` is a live Flow; `AppUiState.stockItems` replaces the dead `obligations` slot. The summary is derived in memory with `StockCalculation.summarize` rather than a one-shot query, so the card updates on every movement.
  2. Stock gets its own bottom-nav destination `Destination.STOCK` ("Stok"), so the bar is now Beranda · Catatan · Stok · Laporan. This **supersedes the "exactly 3 tabs" rule of DEC-008** at the owner's request (2026-10-20: stock management needs its own page). `StockScreen` shows the summary card, "Tambah Barang", and item cards; tapping an item opens a `MankeuSheet` with Masuk / Keluar / Opname. Beranda keeps a compact `StockCard` whose "Kelola Stok" button navigates to the tab. The record speed dial shows only on Beranda and Catatan. No spotlight tour for Stok yet.
  3. `createStockItem` and `recordStockMovement` run inside one Room transaction each (movement insert + level update can no longer half-apply). Opname may record a count of zero; in/out movements must be positive.
  4. Stock movements are inventory-only: they do not post journal entries. Cash for purchases/sales is still recorded as a cash transaction, keeping the pure-cash ledger the single source of money.
  5. Backups older than schema 6 may omit the stock tables (they restore empty); from schema 6 they are required.
  6. Assumption-entity → `AnalysisAssumptions` mapping extracted to `ScenarioAssumptionEntity?.toAnalysisAssumptions()` (was copied in 3 places, now 4 callers including the PDF).
- **Alternatives considered**: Auto-posting a cash event per stock movement (rejected: double-entry of money for UMKM users, and a stock count is not a cash flow). Sheets launched only from a Beranda card (first implementation; replaced by the tab because the owner wanted stock as a page of its own).
- **Affected components**: `StockManagement.kt`, `FinanceRepository.kt`, `AppViewModel.kt`, `HomeScreen.kt`, new `StockSection.kt` (`StockScreen`), `MankeuApp.kt` (`Destination.STOCK`), `EncryptedBackupManager.kt`, `Entities.kt`, exporters, `strings.xml` (id/en).

### [DEC-009] Dual BEP Metrics (Unit & Rupiah) & Interactive BEP Calculator Architecture
- **Date**: 2026-10-19
- **Status**: Accepted & Implemented
- **Context**: 
  1. `HomeScreen` retained recent transaction cards which duplicated the newly consolidated transaction ledger in `EntryScreen` (Catatan). Users felt having transaction cards in both Beranda and Catatan created visual clutter and redundancy.
  2. In `FeasibilityCard.kt` and `ReportsScreen.kt`, break-even analysis was previously hidden unless a user entered an explicit `targetQuantity > 0`. If a business owner provided unit price, unit variable cost, and fixed cost, but omitted the sales volume target, the BEP calculation failed to render and showed an error prompt (`feasibility_bep_missing`).
  3. BEP nominal revenue (omzet titik impas dalam rupiah) was never displayed on `FeasibilityCard`, showing only volume units or leaving users guessing how much sales revenue is required to cover costs.
  4. The assumptions dialog was titled generically ("Asumsi & Parameter Analisis") without live feedback, forcing users to guess how their price/cost changes impacted their break-even point until after saving.
- **Decision**:
  1. **Purge Recent Records from Beranda**: Remove `home_recent` block from `HomeScreen.kt`. Beranda is now dedicated entirely to the high-level cash pulse, net cash flow status, and category cost distribution.
  2. **Decouple BEP Display from Sales Target**: Render break-even metrics as long as $P > VC$ and $FC > 0$ (`breakEvenQuantity != null && breakEvenQuantity > 0`), independent of whether `targetQuantity` is specified.
  3. **Present Explicit Dual BEP (Unit & Rupiah)**: Display **BEP Volume (Unit)** (e.g. `1.363 unit`) alongside **BEP Nilai (Uang / Nominal Rupiah)** (e.g. `Rp136.306.815`) in dedicated side-by-side metric tiles. When a sales target is present, show a visual progress bar comparing target volume against BEP volume.
  4. **Dedicated Interactive BEP Calculator Dialog**:
     - Renamed Tab 2 in `ReportsScreen.kt` to **"Kelayakan & BEP"**.
     - Renamed action button to `[ 🧮 Kalkulator & Parameter BEP ]`.
     - Embedded a **Live Calculation Preview Card** at the top of the dialog recalculating and displaying BEP Unit, BEP Rupiah, and Contribution Margin % in real time as values change.
     - Separated fields into `1. Parameter Utama BEP (Titik Impas)` and `2. Kas & Modal Usaha (Opsional)`.
- **Consequences**:
  - Complete elimination of record duplication between Beranda and Catatan.
  - Zero ambiguity for business owners seeking break-even points: both unit volume and rupiah turnover are immediately visible.
  - Real-time simulation in the dialog provides instant feedback during business planning.
  - 100% test coverage for BEP edge cases (zero/negative margin, missing target volume, fallback fixed costs).

### [DEC-008] UI/UX Redundancy Elimination & 3-Tab Consolidation
- **Date**: 2026-10-19
- **Status**: Accepted & Implemented
- **Context**: Comprehensive user testing revealed serious navigational and visual redundancies:
  1. `HomeScreen` and `ReportsScreen` rendered identical 4 summary metric cards (Operating Cash Flow, Net Profit, Operating Income, Operating Expense), causing `ReportsScreen` to feel like an uninspired duplicate of `HomeScreen`.
  2. `EntryScreen` and `LedgerScreen` competed directly: `EntryScreen` had huge static entry buttons alongside a small 5-item recent list, while `LedgerScreen` was a separate tab for the full list with search/filter, and `RecordSpeedDial` (FAB +) was ALSO floating on every screen.
  3. `EntryScreen` bottom nav icon was `+` (Icons.Default.Add), colliding visually and conceptually with the persistent FAB `+`.
  4. `EntryScreen` contained an expandable "Jenis lain" dropdown (capital, drawings, asset) which contradicted the pure cash-only mental model ("uang masuk ya uang masuk, uang keluar ya uang keluar").
  5. The huge buttons for "Uang Masuk" and "Uang Keluar" took up 50% of the screen on `EntryScreen`, making the entry tab redundant with the FAB (+).
- **Decision**:
  1. **Consolidate Navigation to 3 Clean Tabs**: Drop `Destination.LEDGER` entirely. Bottom navigation now consists of:
     - `Destination.HOME` (Beranda - Cash pulse & operational overview)
     - `Destination.ENTRY` (Catatan - Icon: `Icons.AutoMirrored.Filled.ReceiptLong`, complete searchable & filterable ledger with empty state and undo/delete actions)
     - `Destination.REPORTS` (Laporan - Formal financial statements: Income Statement, Cash Flow, Monthly Trend, Feasibility & Variance analysis)
  2. **Eradicate Redundant Cards on Laporan**: Remove `SimpleMetricGrid` from `FinancialsSection` in `ReportsScreen.kt`. Laporan immediately presents structured, formal financial statements without re-rendering the 4 cards from Beranda.
  3. **Streamline Catatan & Purge Static Entry Buttons**: Remove the giant buttons and "Jenis lain" dropdown from `EntryScreen.kt`. The page is now a dedicated, responsive transaction history (with search, direction chips, and empty card CTA).
  4. **Unify Entry via FAB (+) & Bottom Sheet**: All new transactions are initiated via `RecordSpeedDial` (FAB +) or the `[Catat Sekarang]` CTA on empty state. The `ModalBottomSheet` includes a `SingleChoiceSegmentedButtonRow` between `Uang Masuk` and `Uang Keluar` at the top of the form for effortless direction switching.
- **Consequences**: Navigational clutter eliminated; screen space maximized; user flows for both recording (via FAB) and browsing (via Catatan) are clear and non-duplicative; all unit tests pass 100%.

### [DEC-007] Intuitive UX Alignment & Dual Simulation Feasibility Calibration
- **Date**: 2026-10-18
- **Status**: Accepted & Implemented
- **Context**: User testing by academic advisor surfaced key intuitive design flaws:
  1. Top hero cash balance card on `HomeScreen` was tapped by users expecting detailed cash flow breakdown, but was non-interactive.
  2. `EntryScreen` lacked recent transaction history directly beneath entry buttons, leaving users uncertain if transactions had successfully posted.
  3. Cost composition chart in `HomeScreen` was constrained to rigid presets rather than representing actual customized operational cost categories.
  4. Feasibility analysis in `FeasibilityCard.kt` evaluated B/C Ratio using an R/C threshold ($> 1.0$), erroneously marking profitable micro-businesses (e.g. Simulation 1 CVP benchmark with B/C 0.273) as "TIDAK LAYAK", while also ignoring projection transactions when volume/price assumptions were absent.
  5. `LedgerScreen` retained a dead "UNPAID" filter from the retired obligations schema, and `SharedComponents.kt` used a `Redo` icon for reversing transactions instead of `Undo`.
- **Decision**:
  1. Make `HeroCard` clickable, directly navigating to `Destination.REPORTS` with `"Rincian Arus Kas →"` visual affordance.
  2. Embed recent transactions directly inside `EntryScreen`'s `LazyColumn` below kind buttons, rendering the last 5 transactions with full reversal/deletion actions and a shortcut to full history (`Destination.LEDGER`).
  3. Introduce inline on-the-fly custom category creation directly in `EntryScreen` via `ChoiceMenu` (`"+ Tambah Kategori Baru..."`) and `viewModel.createCategory`, dynamically enriching the `ExpenseDistributionCard` donut chart with exact user-defined cost slices.
  4. Decouple R/C Ratio ($TR / TC$, threshold $\ge 1.00$) from B/C Ratio ($Net Profit / TC$, threshold $\ge 0.00$) in `FeasibilityCard.kt`, and implement automatic fallback in `BusinessAnalysis.kt` to calculate feasibility from recorded projection transactions (`summary.operatingIncomeRupiah`) when volume and price are omitted.
  5. Purge the dead "UNPAID" filter from `LedgerScreen.kt`, and switch reversal icon to `Icons.AutoMirrored.Filled.Undo` in `SharedComponents.kt`.
  6. Calibrate and verify against both real simulations:
     - **Simulation 1 (CVP Micro MSME Benchmark)**: Kedai Kopi Berkah ($P=10.000$, $VC=5.000$, $FC=2.000.000$, $Q=700 \to BEP=400$ units, $R/C=1.273 \ge 1.0$, $B/C=0.273 \ge 0.0 \to$ LAYAK).
     - **Simulation 2 (Sambal Producer Verification Workbook)**: 45 RAB budget lines, 56 transactions, Plan $R/C=1.308$, Actual $R/C=1.117 \to$ LAYAK.
- **Consequences**: User experience is seamless and intuitive; financial feasibility analysis correctly passes both CVP and workbook-based business models; all 65+ unit tests pass.

### [DEC-006] Pure Cash-Only Architecture (Retirement of Obligations & Settlements)
- **Date**: 2026-10-18
- **Status**: Accepted & Implemented
- **Context**: User confirmed explicit requirement to completely eradicate all debt, loan, interest, installment, and credit/obligation concepts ("aku mau hapus utang bunga dan sebagainya sama sekali"). In the micro-MSME target domain, managing accounts receivable/payable, credit terms, due dates, and partial settlements created extreme overengineering and confusion for simple cash-based business operators.
- **Decision**: 
  1. Drop Room database tables `obligations` and `settlements` entirely via non-destructive `MIGRATION_6_7`. Bump database version to 7.
  2. Remove `ObligationEntity`, `SettlementEntity`, and all DAO methods querying obligations and settlements.
  3. Simplify `EntryScreen.kt` to only 5 pure cash transaction types: `CASH_INCOME`, `CASH_EXPENSE`, `OWNER_CAPITAL`, `OWNER_DRAWING`, and `ASSET_PURCHASE_CASH`.
  4. Realise `HomeScreen.kt` and `ReportsScreen.kt` strictly around 4 pure cash metrics: Operating Cash Flow, Net Profit, Cash Income, Cash Expense. Remove due date reminder cards and debt settlement dialogs.
  5. Remove obligations export from PDF and XLSX workbooks, updating `EncryptedBackupManager` table lists.
- **Consequences**: Radically reduced codebase complexity, zero debt overhead, zero risk of foreign key lock contention from cascading settlements, and a 100% intuitive pure-cash accounting system tailored for grassroots MSMEs. All unit test suites pass 100%.

### [DEC-005] Room Database Schema 6 and Migration 5->6 for Stock & CVP Assumptions
- **Date**: 2026-10-18
- **Status**: Accepted & Implemented
- **Context**: Removal of loan/interest columns and introduction of inventory management required a database schema update with zero data loss on existing user devices.
- **Decision**: Bump Room database version to 6. Implement non-destructive `MIGRATION_5_6`: rebuild `scenario_assumptions` preserving all existing values while replacing debt fields with `fixedCostRupiah` and `variableCostPerUnitRupiah`, and create `stock_items` and `stock_movements` tables with foreign keys and cascade delete. Expose `StockDao` in `AppDatabase` and `FinanceRepository`.
- **Consequences**: Safe forward migration for existing app installs with full relational integrity and compile-time schema verification.

### [DEC-004] Architectural Design for Minimal Stock Management
- **Date**: 2026-10-18
- **Status**: Accepted & Implemented
- **Context**: Academic supervisor and UMKM users require lightweight stock tracking without enterprise ERP complexity. Room DB v5 had zero inventory tables.
- **Decision**: Define `StockItemEntity` (master barang with base price in exact integer rupiah, current stock, low stock alert threshold) and `StockMovementEntity` (immutable mutation ledger: IN, OUT, and ADJUSTMENT for stock opname). Encapsulate operations in lightweight domain use cases (`RecordStockMovementUseCase`, `GetStockSummaryUseCase`).
- **Consequences**: Lightweight inventory tracking with O(1) balance reads, atomic mutation history, and zero external dependencies.

### [DEC-003] Realignment of CVP (Break-Even Point) and Ratio Metrics
- **Date**: 2026-10-18
- **Status**: Accepted
- **Context**: The existing calculation used an oversimplified formula `BEP = totalCost / price` and misnamed Revenue-to-Cost ratio ($TR / TC$) as `benefitCostRatio`. In formal managerial accounting, BEP requires separation into Fixed Cost ($FC$) and Variable Cost per Unit ($VC$).
- **Decision**: Implement standard CVP formulas: Unit Contribution Margin ($CM = P - VC$), Contribution Margin Ratio ($CMR = CM / P$), $BEP_{unit} = FC / CM$, $BEP_{rupiah} = FC / CMR$. Distinguish $R/C\ Ratio$ ($TR / TC$, benchmark $> 1.0$) from $B/C\ Ratio$ ($NetProfit / TC$, benchmark $> 0.0$).
- **Consequences**: Formula verified against real micro-business benchmark (Kedai Kopi Berkah: P=10.000, VC=5.000, FC=2.000.000 yielding exact BEP 400 units). Full backward compatibility preserved for legacy workbook imports.

### [DEC-002] Deprecation and Removal of Loan and Interest Calculations in Micro-MSME Domain
- **Date**: 2026-10-18
- **Status**: Accepted & Implemented
- **Context**: `AnalysisAssumptions` and `scenario_assumptions` contained `loanAmountRupiah` and `interestRateBasisPoints`, artificially adding interest expense to production total cost. This contradicts micro-MSME cash-flow realities and adds unneeded complexity.
- **Decision**: Deprecate loan and interest properties in `AnalysisAssumptions` and domain calculators. Establish a clean deprecation and migration roadmap (Room Migration 5 to 6) to remove loan plan fields from data, export/import, and presentation layers.
- **Consequences**: Cleaner domain model focused on operational viability and self-sustaining working capital.

### [DEC-001] Adoption of Modular Logging Protocol
- **Date**: 2026-09-16
- **Status**: Accepted
- **Context**: Standardizing change tracking, observability, and auditability across all managed projects for AI agents and human contributors.
- **Decision**: Mandate `logs/ACTIVITY_LOG.md`, `logs/DECISION_LOG.md`, and `logs/CHANGELOG.md` updates across all repositories.
- **Consequences**: Consistent history, reduced context loss during multi-agent handoffs.
