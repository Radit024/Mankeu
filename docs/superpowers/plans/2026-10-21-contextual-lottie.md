# Contextual Lottie Implementation Plan

**Goal:** Apply all five approved animations to their real app contexts with local native playback.

**Architecture:** Extend StateArtwork/StateIllustration; explicit artwork selection belongs to screen call sites. EmptyCard accepts an optional role. Source archives, deterministic palette conversion and a manifest own provenance; the full license ships in app assets and is accessible from settings.

**Tech Stack:** Existing Kotlin, Compose Material 3 and lottie-compose; Python standard-library asset preparation/checks.

- [x] Acquire the reviewed public asset URLs, validate vector-only compositions, retain exact source archives and license, and create deterministic light/dark resources plus SHA-256 manifest and runnable checker.
- [x] Add contextual artwork roles, icon fallbacks, meaningful held frames and visible-window/lifecycle playback gating. Extend EmptyCard without changing existing default behavior.
- [x] Wire PlanScreen, ReportsScreen, StockSection and MoreScreen import/cloud backup steps. Add offline animation license/creator notice under About.
- [x] Add/run asset, native render and screen/callback tests; inspect emulator captures in light/dark and motion modes. Build APK and lint; record evidence, update UX/provenance/logs and commit only this task's changes, preserving the independent working diff.
