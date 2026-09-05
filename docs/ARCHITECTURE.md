---
status: proposed-baseline
last_verified: 2026-08-15
audience: human-and-ai
sensitivity: internal
evidence_basis:
  - owner requirement to learn native development
  - official Android architecture and Compose guidance reviewed 2026-08-15
  - docs/DOMAIN_MODEL.md
---

# Architecture

## Decision

Build the Android application in **Kotlin with Jetpack Compose**, using native Android architecture and adaptive Material 3 components. Capacitor is excluded because its primary model is a web application running through a native container. Flutter is a credible compiled cross-platform option, but its own rendering/runtime model does not best serve the current goal of learning the Android-native stack.

This choice can be revisited if an iOS requirement becomes funded and concrete. It is not a judgment that Flutter is a WebView.

## Proposed stack

| Concern | Baseline |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 adaptive layouts |
| State | Immutable UI state, ViewModel, coroutines, and Flow |
| Local structured data | Room over SQLite |
| Preferences | DataStore |
| Background work | WorkManager for deferred backup/export/sync tasks |
| File selection/export | Android Storage Access Framework |
| Dependency injection | Select during skeleton ADR; avoid framework coupling in domain tests |
| Serialization | Versioned, explicit schema selected during implementation |
| Tests | Kotlin unit/property tests, Room integration/migration tests, Compose UI tests, device end-to-end tests |

Official references:

- [Jetpack Compose](https://developer.android.com/develop/ui/compose)
- [Adaptive Compose layouts](https://developer.android.com/develop/ui/compose/layouts/adaptive)
- [Android architecture guidance](https://developer.android.com/topic/architecture)
- [Offline-first data layer](https://developer.android.com/topic/architecture/data-layer/offline-first)
- [Room](https://developer.android.com/training/data-storage/room)
- [Android App Bundles](https://developer.android.com/guide/app-bundle)

## Layer boundaries

```text
Compose UI
  ↓ user intents / observable state
Application use cases
  ↓
Pure domain model and calculation policies
  ↓ ports
Repositories ── Room / files / importers / optional remote adapters
```

- **Domain:** money, balanced events, budgets, projection/realization, obligations, calculations, reconciliation classifications. No Android dependency.
- **Application:** use cases, transaction boundaries, authorization of destructive actions, import orchestration, backup/restore.
- **Data:** Room entities/mappers, files, import/export adapters, migrations, optional sync adapters.
- **Presentation:** Compose routes, state holders, formatting, adaptive layout, accessibility.

Import parsing must not write directly to production tables. It builds a candidate model and provenance set, then the application layer commits them atomically after reconciliation.

## Presentation-layer cross-cutting concerns

Three concerns cut across every screen and are therefore owned by the shell rather than duplicated:

- **Appearance.** `MankeuTheme` interpolates every `ColorScheme` and `MankeuSemanticColors` token
  towards its target instead of swapping schemes, so a Terang/Gelap change animates across the whole
  hierarchy without a second subcomposition and without discarding UI state. Language is the
  opposite case: it goes through `Activity.recreate()` behind a loading overlay, because an
  in-composition locale override does not reach `Dialog`/`Popup` subcompositions (D-017). Both read
  the duration budget from `LocalMotion`, and `LocalMotion` itself is `MankeuMotion.resolve(...)` of
  the user's Gerakan setting and the system animation scale, so both the app control and the
  platform accessibility setting are honoured (D-025).
- **Guided tour targets.** `LocalTourTargets` carries a `TourTargetRegistry` that composables write
  their root-space bounds into via `Modifier.tourTarget(...)`. `SpotlightTour` reads it to punch a
  real cutout over live UI. The registry is write-only from the screens' point of view and costs
  nothing when no tour is running, which keeps the tour out of the screens' own logic.
- **System back.** A single `BackHandler` in the shell states the whole precedence order — modal
  tour, open overlay, non-start destination, then the platform default. Screens do not register
  their own handlers, so the order cannot drift with composition order.
- **Asking the user something.** Every modal is `MankeuSheet` — one `ModalBottomSheet` with the title
  at the top, scrolling content in the middle and the actions at the bottom, where a thumb is
  (D-023). There is no `AlertDialog` anywhere in `app/src/main`. The sheet's content column
  `animateContentSize`s, so a section opening inside it grows the sheet rather than snapping it, and
  its `SheetState` is created internally so the experimental Material 3 opt-in does not spread to
  every call site.

Presentation state that is not accounting data (theme, motion, language, local display name, whether
the tour has been seen) lives in `SharedPreferences` via `AppPreferencesState`, deliberately outside
the audited Room schema, its migrations, and its export/backup contract.

## Suggested module shape

```text
:app
:core:model
:core:money
:core:database
:core:designsystem
:feature:business
:feature:transactions
:feature:budget
:feature:reports
:feature:receivables
:feature:payables
:feature:importexport
:feature:backup
:import:reference-workbook
```

Start with fewer Gradle modules if build overhead outweighs separation, but preserve dependency direction. Domain code must remain independently testable.

## Local-first data ownership

- The local Room database is authoritative for normal operation.
- Core functions require no account, network, Firebase, Drive, or application server.
- UI observes local data; optional remote work writes back through explicit repositories rather than bypassing the database.
- Backups have a version, database/schema version, creation time, checksum, encryption metadata, and manifest of included businesses.
- Restore validates integrity and compatibility before changing the active database and retains a recovery copy where storage permits.

## Backup, Drive, and authentication

The first backup is an encrypted local export chosen through the system file picker. A later Google Drive adapter may use each user's storage so the product does not require an application-owned financial database server.

Drive backup and multi-device synchronization are different products:

- **backup/restore** can begin as explicit snapshot upload/download;
- **synchronization** requires record identities, conflict policy, deletion semantics, encryption/key recovery, and observable sync state.

Firebase authentication may be explored for a future account experience, but local use cannot depend on it. No Firebase or Drive project should be created before an owner-approved ADR states data flow, cost, privacy, and failure behavior.

## Security and privacy baseline

- No real workbook, database, research response, backup, credential, API key, signing key, service configuration, or token belongs in Git.
- Use Android Keystore-backed key protection for secrets where applicable; select the database/file encryption implementation only after a small threat model and maintenance review.
- Export and backup are explicit user actions with destination and sensitivity warnings.
- Logs redact descriptions, counterparties, amounts where not required, file paths, tokens, and personal data.
- Analytics is off by default until events, consent, retention, and research separation are approved.
- Debug builds and test fixtures must not share production signing or cloud projects.

## Release format and platform requirements

- Use APKs for local installation and device testing.
- Publish new Google Play applications as Android App Bundles (`.aab`).
- As of the planning date, Google Play states that new apps and updates must target Android 16 / API level 36 from 31 August 2026. Reverify policy immediately before release: [target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878).
- Minimum SDK and representative low/mid-range device baseline remain open decisions requiring user-device evidence.
- Package identifier, application signing ownership, Play Console owner, privacy policy URL, and final product name remain intentionally unset.

## Architecture decision records still required

Before implementation passes the foundation phase, write ADRs for:

1. package identifier and minimum SDK;
2. dependency injection and serialization;
3. exact decimal and rounding policies;
4. backup schema and encryption;
5. database migration/rollback;
6. optional Drive authorization and sync conflict resolution;
7. analytics/crash reporting, if any;
8. signing custody and release ownership.
