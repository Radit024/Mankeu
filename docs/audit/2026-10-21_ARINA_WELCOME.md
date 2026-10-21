# Arina welcome acceptance evidence

Date: 2026-10-21, Asia/Jakarta. Agent: Codex.

## Claim and changed behavior

User-confirmed: implement the approved mobile welcome recommendation. Verified: the native
screen uses the existing theme/fonts, the supplied Arina brand vector, local bookkeeping artwork,
Google sign-in and offline entry. The first entry choice is saved locally. Existing businesses
bypass welcome; failed authentication leaves offline entry usable. Sign-in itself is not a backup.

No financial schema, database migration, report calculation, new dependency or cloud account change.
`welcome_complete` is a UI preference; removing it only re-offers welcome on an empty installation.

## Named automated acceptance

Verified on the isolated `codex_arina_welcome` Android API 37 emulator (`emulator-5580`), with Wi-Fi
and mobile data disabled. No tests installed on or changed the connected owner's physical device.
Room fixtures are in-memory and UI preferences are saved/restored by the integration fixture.

- `gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest --console=plain`:
  BUILD SUCCESSFUL; 64 unit tests, 0 failures/errors/skipped. Lint: 0 errors, 84 warnings, 2 hints.
  Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Phone: 1080 x 2400, 420 dpi; `WelcomeScreenTest`: 6/6 passed.
  - `bothChoicesAreVisibleAndGoogleDoesNotCompleteOfflineChoice`: both visible actions invoke only their own callback.
  - `busyGoogleKeepsOfflineEntryEnabledInDarkTheme`: Google disabled while busy, offline remains actionable.
  - `largeTextKeepsBothChoicesReachable`: 200% font, both actions scroll into view.
  - `offlineChoiceReachesRealHomeAndSurvivesCompositionRestart`: real MankeuApp reaches home, completion persists,
    and no business or authenticated identity is silently created.
  - `failedSignInStaysOnWelcomeAndSuccessfulAuthEntersHome`: injected network failure is shown without completing
    welcome; injected authenticated state enters home and persists completion.
  - `existingBusinessBypassesWelcomeWithoutAuthentication`: existing in-memory business goes straight to home.
- Tablet: 1600 x 2560, 240 dpi; first three screen/layout tests above: 3/3 passed.
- Landscape: 1920 x 1080, 420 dpi; large-text reachability test: 1/1 passed.
- `git diff --check`: passed. Remaining line-ending notices concern existing Windows working copies.

Reproduce with the installed JDK (`E:/Android Studio/jbr`) and SDK (`E:/Android SDK`). After installing
both debug APKs on an isolated test device, run:

```powershell
adb -s emulator-5580 shell am instrument -w -e class com.mankeu.presentation.WelcomeScreenTest com.mankeu.test/androidx.test.runner.AndroidJUnitRunner
```

`-e welcomeCapture tablet` labels screenshots. The tests write isolated captures to
`files/welcome-ui` in the test installation. Tablet/landscape dimensions use `adb shell wm size` and
`wm density` only on that emulator; reset both after execution.

## Visual inspection and limits

Verified by image inspection: phone light/dark, 200% text and tablet captures; the column remains
bounded to 480 dp and 900 dp high, centered on taller tablets. Both actions remain visible or
reachable by scrolling. The supplied logo is lifted to the theme primary in dark mode, including
when app theme differs from the OS resource night configuration.

Preview files: `docs/design/previews/arina-welcome-light.png`, `arina-welcome-dark.png`,
`arina-welcome-preview.png` (scaled contact sheet). No live financial records appear in these captures.

Not verified: real Google account selection/OAuth/Firebase exchange, account-picker cancellation on
a real provider, TalkBack traversal and physical-device usability. The existing Google API is
deprecated; migration to a new authentication API is outside this design change. Provider error
handling and duplicate-launch protection are implemented; injected auth tests do not prove live OAuth.

During test development, replacing the host Activity context outright for localization caused
three fixture failures (missing ActivityResultRegistryOwner). Wrapping the host Activity's resources
fixed that fixture; the final six app/screen tests all pass. This was not a runtime app defect.

Unrelated pre-existing splash-theme changes, screenshot-test edits and visual-system script edits
remain in the working tree. The existing supplied logo/vector and their color tokens are reused
and included as required assets for this feature; no credentials or financial data are committed.
