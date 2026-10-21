---
status: proposed-baseline
last_verified: 2026-08-15
audience: human-and-ai
sensitivity: internal
evidence_basis:
  - owner-confirmed phone and tablet requirement
  - workshop-origin problem statement
  - docs/PRODUCT_REQUIREMENTS.md
---

# UX and Accessibility

## Experience principles

1. **Record first, organize progressively.** A user in the field can save a valid transaction with a few required inputs and add detail later.
2. **Use business language.** Prefer `Uang Masuk`, `Uang Keluar`, `Belum Dibayar`, and `Jatuh Tempo`; accounting detail remains available as explanation.
3. **No silent repair.** Every import enrichment or correction is visible and reviewable.
4. **No dead ends.** Error states explain the impact and the next safe action.
5. **One meaning everywhere.** Amount, category, project, and financing terms must not change meaning across entry, reports, import, and export.
6. **Offline is normal.** Network state is secondary; core screens do not look broken when offline.

## Information architecture

Recommended compact-width destinations:

- `Beranda`: cash position, due items, recent activity, active business/project;
- `Catat`: fast money-in, money-out, receivable, and payable flows;
- `Anggaran`: projection, budget items, and comparison;
- `Laporan`: profit/loss, cash flow, financing, and variances;
- `Lainnya`: businesses/projects, categories, import/export, backup, settings, and help.

The final navigation requires prototype testing. `Catat` should remain reachable in one deliberate action from the primary screen; it must not be buried in an overflow menu.

## Phone behavior

- Use a bottom navigation bar for primary destinations and a prominent quick-entry action.
- Default to the most recently selected business and project, while always showing the current scope.
- Transaction entry initially asks for direction, amount, date, description/category, and scope. Optional details expand progressively.
- Amount entry uses an IDR-aware numeric keyboard and formatted confirmation without floating-point behavior.
- Reports use summaries and drill-down cards; core understanding must not require a wide spreadsheet or horizontal scrolling.
- Destructive actions use clear object names and recoverability, not generic `Apakah Anda yakin?` dialogs.

## Tablet behavior

- Use adaptive navigation rail or pane layouts at suitable widths.
- Prefer list-detail interaction: transactions beside detail/edit, projects beside summaries, notices beside provenance.
- Support keyboard navigation, visible focus, and hardware-keyboard entry.
- Do not merely stretch phone cards across the screen; take advantage of additional context without creating a separate product.
- Preserve the same domain actions and terminology as phone layouts.

## First-run flow

### Arina welcome (2026-10-21)

User-confirmed design: a bounded, scrollable welcome screen uses the supplied Arina mark,
local bookkeeping artwork, Sora headings and Plus Jakarta Sans controls. It offers
`Lanjut dengan Google` and `Lanjut tanpa akun`; both buttons are at least 56 dp high.
All copy is available in Indonesian and English and follows the selected light/dark theme.

The welcome screen is offered once on an empty installation. Successful authentication or
choosing offline entry stores `welcome_complete` in device UI preferences, outside financial
data, exports and backups. Existing businesses and already authenticated users bypass it.
Signing out never forces a new login. Google cancellation/failure leaves welcome available,
and the offline button stays enabled while sign-in is busy. Errors use the existing snackbar.
Signing in does not create a backup or prove that Drive is active; the page directs owners to
`Berkas dan Data` to create backups. Account settings retains its existing Google sign-in action.

Runnable acceptance: `WelcomeScreenTest` covers callbacks, busy/offline entry, 200% text,
first entry into the real empty-database shell, preference persistence, failed/successful auth
state transitions, and bypass for an existing business. Provider authentication still needs
a configured Google account and live OAuth validation; simulated auth state is not evidence of it.

1. Explain in one screen that data stays on this device by default and no login is required.
2. Ask for a business name. The application never creates one on the user's behalf — see D-015.
3. Ask for a first field/season (`lahan`). It is not optional and no `Umum` project is invented; a
   book pair only exists once the user has named the scope it belongs to.
4. Offer `Catat transaksi`, `Buat anggaran`, or `Impor workbook` as concrete starts.
5. Keep account sign-in optional on welcome or in settings; defer backup actions, research consent,
   and optional templates until relevant.

No onboarding page may block offline use merely to obtain analytics, authentication, or marketing data.

## Import experience

The preview groups results by outcome:

- siap diimpor;
- dinormalisasi;
- dipulihkan otomatis;
- perlu keputusan;
- konflik yang harus diselesaikan;
- tidak didukung.

An enrichment such as `Sewa Lahan Rp7.000.000` appears in the preview and, after commit, in the persistent activity/notification center. The summary states the number and total value of enriched records. Users can open source details without reading parser logs.

The UI must not imply that the source workbook was wrong when the application merely needed to reconcile information across sheets.

## Empty, loading, and error states

- Empty states describe the first useful action and provide it directly.
- Local calculations normally use skeletons only when genuine work is pending; do not animate unnecessarily.
- Storage, import, restore, or migration failures preserve the previous safe state.
- Reconciliation errors show affected totals and source evidence.
- Sync status, if later introduced, distinguishes `tersimpan di perangkat`, `menunggu cadangan`, and `dicadangkan`; it must not conflate local save with cloud copy.

## Colour system (DEC-019, 2026-10-21)

The owner's `font-dan-color-palette.md` defines the Arina visual identity. The current native
Material 3 mapping lives in `presentation/Theme.kt`: green primary (#2D6A4F light / #52B788 dark),
terracotta secondary (#E07A5F / #D4836A), warm off-white background (#FAFAF8), and Slate Garden
dark background (#1E2620). Cards use #FFFFFF / #262D28. XML splash and logo colors follow the
same day/night reference. Wallpaper-based dynamic colors are not enabled.

Typography is explicit in `Typography.kt`: bundled Plus Jakarta Sans 400/500/600/700 for body,
labels and controls; Sora 600/700 for display/headline/title and KPI roles. No font download is
needed on first launch. Tabular numerals keep financial figures steady. Fonts and illustrations
are documented with licenses/hashes in `docs/design/ASSET_PROVENANCE.md`.

The reference is a web palette, not a complete Material color scheme. Raw light success #52B788
and error #E07A5F fail normal text contrast on white. Brand secondary fills retain terracotta,
while text/error roles use dark green #2D6A4F and dark terracotta #9A442D. Every container has its
own on-color. Dark raised surfaces stay below the luminance where secondary text and terracotta
fall below 4.5:1. The reference divider is decorative; controls retain a stronger outline.

## Measured colour contrast

Run `python scripts/verify_visual_system.py`. It parses the actual Kotlin constants rather than
a separate generated palette, audits normal text at 4.5:1 and chart/control graphics at 3:1,
and validates local fonts, offline animations and splash parity. The old
`docs/evidence/theme/generate_palette.py` remains historical evidence for the superseded blue
scheme; its generated output must not be applied to the current theme.

The current audit checks 160 actual-token pairs. Its tightest pair is dark terracotta on the
highest/variant surface, approximately 4.55:1. These token measurements do not establish TalkBack,
font-scaling or complete workflow acceptance; see the implementation verification report.

## Animation and illustrations

Compose Animation owns page/book transitions, FAB, sheets and functional meters. Lottie Compose
6.6.7 renders two bundled original JSON compositions: a ledger illustration for empty states and
a success check beside confirmed operation results. No WebView or remote asset URL is used.
Loading or corrupt-asset fallback is a Material icon; adjacent localized text remains readable.

Full motion uses brief spatial transitions and one-shot Lottie playback. Reduced uses brief fades
and static completed illustrations, with no skeleton/spotlight pulse. Off uses instant state
changes and static illustrations. A system animation scale of zero overrides the app setting.
Decorative playback pauses below the STARTED lifecycle state. Numeric amounts are never counted
through fictitious intermediate values, and theme switching swaps tokens without replacing the
composition tree or clearing draft forms.

## Book-mode colour identity

Proyeksi and Realisasi must be distinguishable at a glance, not only during the transition between
them. An early attempt paired `secondaryContainer` with `successContainer`; in a green-seeded
palette those are two near-identical pale greens and the difference was invisible on device. The
shipped pairing is semantic: a plan is provisional and wears the warning container, a realisation is
confirmed and wears the success container. Each carries its own
on-container foreground. The label and hero wording also identify the selected book, so color is
not the sole signal.

## Elevation and depth

Light mode separates layers with tone *and* a shadow; dark mode uses tone alone. A drop shadow is a
light-mode device — on a dark surface a black shadow either disappears or turns into a hard grey rim
— so the navigation bar and the scope header drop their `shadowElevation` when the dark scheme is in
effect and rely on `surfaceContainer` / `surfaceContainerLow` sitting above `surface`. The check is
`isDarkScheme()`, which reads the luminance of the scheme actually in effect rather than the stored
preference, so it reads the active scheme consistently when the theme changes.

Content cards sit on `surfaceContainerLow`, bottom sheets on `surfaceContainerLow` with tonal
elevation off, and the navigation bar on `surfaceContainer`. Nothing draws a flat white or flat grey
any more.

## Accessibility baseline

### Contextual animation

Business Plan decorates the empty Rencana Usaha introduction, Growth Chart the empty report,
and Cardboard Box the empty inventory summary. Upload File decorates the import card; visible
labels, validation and preview/commit controls retain all meaning. Cloud Data Backup appears
only inside the cloud backup/restore step, never for local backup, general sign-in or a saved
transaction. A rising chart is an introduction to reporting, not a claim that a business is profitable.

All five additions have bundled light/dark Arina variants; the two existing general empty/success
compositions remain available. StateIllustration owns one-shot native playback, pauses outside
the visible window or below STARTED lifecycle, and shows a reviewed held frame for Reduced/Off
and completed playback. Its Material icon fallback preserves the footprint if parsing fails.
Decorative nodes have no spoken label; nearby text describes the state and available actions.
Animation credits and the complete Lottie Simple License are available offline under About.

- Meet current Android accessibility guidance and target WCAG 2.2 AA where applicable.
- Interactive targets are at least 48 dp and have meaningful accessibility labels.
- Color is never the only indicator of income/expense, due status, variance, or failure.
- Text supports Android font scaling without clipping critical amounts or actions.
- Currency and dates have readable spoken labels.
- Charts have text summaries and selectable data alternatives.
- Motion is non-essential and respects reduced-motion preferences.
- Screen-reader order follows task order; modal focus is contained and restored.
- Tablet flows are fully usable with keyboard and switch-style navigation where supported.

## Content style

- State the result first: `Impor selesai: 84 catatan, 1 dipulihkan otomatis`.
- Explain assumptions plainly: `Tanggal harian tidak ada; periode Agustus digunakan`.
- Avoid blame, parser jargon, and false certainty.
- Use consistent Indonesian labels; English technical terms may appear only in advanced help.

## Touch targets, tooltips and haptics

These rules apply to every new screen. The reasoning is in D-084 to D-087.

### Touch targets

- Every interactive element is at least **48 × 48 dp**. Use the Material 3 defaults (`IconButton`, `FilterChip`, `NavigationBarItem`); never shrink them with `Modifier.size` below 48dp. Shrink the icon inside instead.
- A read-out is not a control. Status badges and metric cards have no `onClick` and no ripple. An `AssistChip` with `onClick = {}` is not allowed.
- Destructive triggers use the error colour and sit apart from the primary action. Primary actions are filled buttons, secondary ones tonal or text buttons.

### Tooltips

- Use `MankeuTooltip` (Material 3 `TooltipBox` + `PlainTooltip`), never a custom popup.
- Required on: icon-only actions, status badges (DIBALIK, feasibility, obligation status), and figures that can be ellipsized (metric cards).
- The tooltip text says what the element does or what a status is based on. It does not repeat a label that is already visible.

### Haptic taxonomy

| Event | `HapticFeedbackType` | When |
| --- | --- | --- |
| `TAP` | `SegmentTick` | Changing tab, segment or filter; picking a date; dismissing a sheet |
| `LONG_PRESS` | `LongPress` | A tooltip appears |
| `WARN` | `GestureThresholdActivate` | Starting delete, reverse or archive |
| `CONFIRM` | `Confirm` | A save, import commit, export or backup succeeded |
| `REJECT` | `Reject` | Form validation or a ledger rule refused the action |

- Fire feedback through `LocalFeedback.current(event)`, never `Vibrator` directly. It respects the phone's vibration settings and the in-app switch.
- No feedback on scrolling, typing, or re-selecting what is already selected.
- ViewModel messages built with `UiText.fromError` are refusals (`isError = true`) and get `REJECT`.

### Forms and the keyboard

- A validation error belongs on the field at fault (`isError` + `supportingText`), and that field takes focus. A line above the submit button is reserved for errors with no single field.
- Text fields set an IME action: `Next` inside a form, `Done` on the last field.
- Money fields use `KeyboardType.Number`. Rupiah is stored as whole integers, so there is no decimal key.
- A sheet containing inputs applies `imePadding()` before `verticalScroll`, so the focused field scrolls above the keyboard.
- Money is never ellipsized. A figure in a fixed-width card uses `autoSize = TextAutoSize.StepBased(...)` with `maxLines = 1`. Check every new money read-out at 130% font scale.

## UX acceptance evidence

A screen is not accepted from a mockup alone. Evidence includes task-based testing on representative phone and tablet sizes, TalkBack checks, font scaling, keyboard navigation for tablet flows, offline behavior, and user comprehension of at least one enrichment notification and one blocked conflict.
