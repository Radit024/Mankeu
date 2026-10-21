# Mankeu Material 3 / Arina visual system

Status: user-confirmed on 2026-10-21; the owner selected Lottie Compose after reviewing the proposed fonts, palette and motion scope.

## Intent and boundaries

Refresh the native Android UI through the shared Material 3 theme and components. Keep every financial rule, schema, import format, navigation destination and offline capability intact. Existing Compose Animation and Material Icons are reused. Lottie Compose renders bundled JSON; no WebView or network-loaded illustrations or fonts.

## Visual contract

- Authoritative brand reference: `font-dan-color-palette.md` supplied by the owner. Plus Jakarta Sans serves body, labels and controls; Sora serves display/headline/title and KPI roles. Bundle licensed static font weights locally. Replace unsupported Black overrides with supported weights.
- Light: primary #2D6A4F, secondary/error #E07A5F, background #FAFAF8, cards #FFFFFF, text #2C2A29 / #6B6866. Dark: primary #52B788, secondary/error #D4836A, background #1E2620, cards #262D28, text #DDE8DF / #8BA08E (Slate Garden).
- Map the web reference into complete Material 3 roles. Brand fills keep their exact colors; use a darker semantic foreground for light-theme success/danger text where the raw main color fails 4.5:1. Choose accessible on-colors rather than copying an inaccessible white-on-success pairing. Distinguish warning, error and expense with text and icons as well as color.
- Shared cards use a consistent paper token, modest corners and a 4dp spacing grid. The hero uses primary/container tones with accessible foregrounds, including negative amounts. Phone bottom navigation and expanded navigation rail remain.

## Motion and Lottie

Reuse the existing LocalMotion policy for page/book transitions, FAB and sheets. Full permits short spatial transitions and Lottie playback; Reduced uses short fades and static illustrations; Off uses instant state changes and static illustrations. System animation scale zero wins. Do not animate financial amounts through invented intermediate values. Theme switching must preserve form and scroll state.

Use a small original document illustration for EmptyCard and a check illustration for successful operation feedback, each bundled in res/raw. Rendering failure falls back to a Material icon. Success playback runs once, driven by existing confirmed result messages, without extending snackbar lifetime or blocking interaction. Errors keep an explicit error icon and message. Decorative visuals have no accessibility announcement; readable text remains the source of meaning.

## Ownership

Theme.kt owns palette, semantic roles, shape and motion policy. Typography.kt owns fonts/type scales. A shared StateIllustration composable owns local Lottie rendering and fallback. SharedComponents owns empty states; the shared snackbar owns operation feedback. Domain/data/import/auth/sync code is outside this change.

## Acceptance evidence

Run the JVM suite, Android lint and debug/test APK compilation. Add runnable palette contrast/asset verification and instrumented checks for theme/font roles, static reduced/off Lottie, fallback behavior, snackbar action/accessibility and real empty-state actions. Inspect phone/tablet light/dark rendering on an available emulator. Explicitly report any unavailable device evidence. Update activity, decision and changelog records in the same commit as implementation. Preserve unrelated Gradle and skill files.
