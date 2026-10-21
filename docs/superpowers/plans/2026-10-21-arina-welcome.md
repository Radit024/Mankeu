# Arina Welcome Implementation Plan

> Execute inline using executing-plans; the owner has approved implementation of the previously presented design.

**Goal:** Add a first-run welcome with optional existing Google authentication and uninterrupted offline entry.

**Architecture:** AppPreferences owns the device completion flag. MankeuApp owns authentication and welcome routing. WelcomeScreen renders localized theme-aware content and callbacks without owning account or financial data.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, existing SharedPreferences and Google sign-in.

## Tasks

- [x] Add WelcomeScreen, local bookkeeping artwork and ID/EN strings. Reuse the supplied Arina vector and its light/dark color resources.
- [x] Add persistent completion in AppPreferences and wire first-run routing in MankeuApp. Keep its existing snackbar effects, Google launcher, and settings path. Suppress duplicate launches, catch launcher errors, and preserve offline entry after cancellation/failure.
- [x] Add runnable Compose tests covering both callbacks, busy state, errors, large text, persistence, and the real empty-database offline route. Use only isolated test data.
- [x] Run `gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest` with the installed JDK. Run the welcome instrumentation tests on an isolated emulator, inspect captures, then update the authoritative UX guide and all relevant logs. Commit only this feature and required brand resources, preserving unrelated working changes.
