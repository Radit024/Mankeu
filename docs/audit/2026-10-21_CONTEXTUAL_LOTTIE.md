# Contextual Lottie acceptance evidence

Date: 2026-10-21, Asia/Jakarta. Agent: Codex.

## Behavior and scope

User-confirmed: apply the five LottieFiles candidates reviewed in the preceding browser research.
Verified: Business Plan is used for an empty business plan; Growth Chart for empty reports;
Cardboard Box for empty inventory; Upload File for Excel import; Cloud Data Backup only inside
the cloud backup/restore step. Local backup and the backup choice page do not show cloud artwork.
Original general empty/success illustrations are retained.

The shared StateIllustration owns native RawRes playback, explicit app-theme variants, semantic
icon fallback, visible-window/lifecycle gating and a meaningful held frame for Reduced/Off or
finished one-shot playback. No new dependency, remote runtime resource, financial calculation,
database migration, authentication permission or import/backup contract change.

## Asset verification

`python scripts/prepare_lottiefiles.py`: PASS, five pinned source archives, ten deterministic
offline light/dark derivatives, 306,587 JSON bytes; license and creator credits included.
Sources are vector-only (including vector precompositions), without external font, image or
expression dependencies. Original archives, URLs, source/derivative SHA-256 hashes and author
names are retained in `docs/design/lottie-sources/manifest.json`.

Official license verified in the browser: https://lottiefiles.com/page/license,
Lottie Simple License (FL 9.13.21). The complete terms and credits ship as app assets and can be
opened offline in Settings > About > Animation licenses. Third-party derivatives retain that
license and are not relicensed as GPL application code.

## Named acceptance

Isolated `codex_arina_welcome` API-37 emulator, `emulator-5580`, 1080 x 2400 / 420 dpi, network
disabled. Instrumentation uses fictional Usaha Contoh / Unit Contoh in an in-memory Room database,
saves/restores UI preferences and restores simulated auth state. No owner device was changed.

- Gradle `assembleDebug assembleDebugAndroidTest`: BUILD SUCCESSFUL.
- Gradle `testDebugUnitTest lintDebug --max-workers=2`: BUILD SUCCESSFUL; 64 unit tests,
  zero failures/errors/skipped; lint zero errors (warnings retained in the generated report).
- `ContextualLottieTest`: 4/4 passed:
  - `allLightAndDarkAssetsRenderMeaningfulHeldFramesAndRealMotion`: all seven artwork roles
    parse and draw nonblank colored held frames in both theme variants. Rendered pixel hashes
    at three timeline positions differ, proving actual motion rather than a nominal duration.
  - `reducedAndOffArtworkDoNotAdvanceWithTheClock`: after composition readiness, native Compose
    captures remain identical across two seconds of test-clock advancement in Reduced and Off.
  - `animationLicenseAndAllCreatorCreditsAreAvailableOffline`: packaged terms and all five
    creator names appear without network access; the notice dismisses normally.
  - `realNavigationUsesEachArtworkOnlyInItsRelevantContext`: real shell navigation through
    Laporan, Stok, Proyeksi/Catatan, Berkas dan Data and backup steps. Import/add-item actions
    remain enabled. Cloud artwork is absent from local backup and appears in the cloud step.
    The fixture's financial event and stock datasets remain empty.
- Additional runs of the real-navigation test: dark theme 1/1; system font scale 200% 1/1.
- `git diff --check`: passed; line-ending notices are Windows normalization notices.

Reproduce after installing both debug APKs on an isolated device:

```powershell
adb -s emulator-5580 shell am instrument -w -e class com.mankeu.presentation.ContextualLottieTest com.mankeu.test/androidx.test.runner.AndroidJUnitRunner
adb -s emulator-5580 shell am instrument -w -e lottieTheme dark -e class com.mankeu.presentation.ContextualLottieTest#realNavigationUsesEachArtworkOnlyInItsRelevantContext com.mankeu.test/androidx.test.runner.AndroidJUnitRunner
```

For the large-text run, set that emulator's `font_scale` to 2.0 and restore its original value
afterwards. Tests write captures to `files/lottie-proof` in the isolated installation.

## Visual review and limits

Native asset samples and actual screen captures were inspected. The Business Plan source has
blue gradient fills in addition to static colors; visual review identified these, and the
deterministic adapter now recolors their exact stops as well. Paths/timing/skin tones stay intact.

During fixture development, localization of only the Compose context left new dialog windows in
the system language. The fixture now localizes/restores its host Activity resources like the real
MainActivity. The motion check now waits for a composition read in composition, before comparing
frames. Capture selection asserts the root containing the intended artwork, then uses UiAutomation
full-display screenshots so separate modal windows are included. Compose root capture alone sampled
the underlying Activity. Final light/dark navigation reruns passed and modal captures were inspected.

Not verified: physical-device usability/TalkBack, frame-time or battery benchmarks, background and
off-screen pause timing under a measured lifecycle trace, and actual cloud backup/restore. Native
multi-frame rendering is verified; it does not establish performance on low-end physical devices.
The cloud test only opens the form using a simulated identity and performs no network operation.
