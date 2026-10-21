# Material 3 Arina UI Implementation Plan

> For agentic workers: execute the approved design in this session; use independent asset work and spec/code review where useful. Preserve unrelated workspace changes.

**Goal:** Apply the owner's Arina palette and typography throughout Mankeu, reuse Material 3/Icons/Compose Animation, and add offline Lottie Compose illustrations.

**Architecture:** Extend the existing shared theme, motion policy and cross-screen components. No financial model, schema, network service or navigation redesign.

**Tech Stack:** Kotlin, Jetpack Compose Material 3 and Animation, local TTF resources, Lottie Compose with local JSON.

## Tasks

- [ ] 1. Bundle fonts and original Lottie JSON with provenance/licenses in res/font, res/raw and docs/design. Verify each binary and JSON can be read and uses no external assets.
- [ ] 2. Replace blue Material roles in presentation/Theme.kt with complete reference mappings, add Typography.kt with local fonts, update splash colors and replace stale hero colors. Review all color foreground/container uses. Add a runnable scripts/verify_visual_system.py check that parses actual Theme.kt, tests contrast of used pairs and validates font/animation resources.
- [ ] 3. Add pinned Lottie Compose dependency in app/build.gradle.kts without changing the pre-existing version-catalog upgrade. Implement StateIllustration.kt with RawRes loading, Material icon fallback, one-shot playback and static Reduced/Off modes. Integrate EmptyCard and a result Snackbar preserving existing messages/actions. Normalize unsupported font weights and shared card spacing. Make existing screen transitions respect Reduced/Off without discarding their current destinations or state.
- [ ] 4. Add instrumented VisualSystemTest checks for both theme roles, offline illustrations, actions and snackbar behavior. Run `gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest --console=plain`; use a configured local JDK. Run `python scripts/verify_visual_system.py` and `git diff --check`. On an available emulator execute instrumented tests and capture phone/tablet light/dark evidence using fictional test data only.
- [ ] 5. Spec compliance review, code quality review, fix issues, then record exact evidence and limitations in docs/design, logs/ACTIVITY_LOG.md, logs/DECISION_LOG.md (DEC-019), logs/CHANGELOG.md and nearest theme/accessibility docs. Commit only this task's paths and logs atomically; leave unrelated files unstaged.

## Initial review

The owner approved the design and explicitly selected Lottie Compose. Current code already implements Material 3, Material Icons and Compose Animation. Existing Gradle upgrades are user workspace changes, not part of this UI commit. Tests must assess real color pairs rather than assuming every raw brand main color is valid as small text.
