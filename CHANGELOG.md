# Changelog

## Unreleased

### Added

- Native Kotlin/Jetpack Compose Android app with adaptive phone/tablet navigation.
- Room schema for businesses, projects, financial events, balanced journals, postings, budgets,
  obligations, settlements, import batches, provenance, and notices.
- Local multi-business and multi-project workflows. Nothing is created on the user's behalf: a first
  run legitimately has no scope and offers explicit "Tambah Usaha" / "Tambah Lahan" (D-015).
- Full create/select/edit/archive of usaha and lahan from a bottom sheet opened at the scope picker
  itself (D-024).
- Exact-rupiah transaction entry and derived profit, operating cash, financing cash, and cash
  balance. Money is stored as integer rupiah; no floating point anywhere on the money path.
- Budget (RAB) per book, receivables/payables with settlement, and plan-versus-actual comparison.
- Native OOXML workbook reader, import preview, deterministic enrichment with provenance, atomic
  commit, duplicate-hash guard, and full batch reversal.
- On-device categorisation of imported rows — keyword then fuzzy match, no model, no network — so an
  imported workbook arrives with a real cost breakdown rather than one bucket (D-034).
- Excel and PDF export, and encrypted `.aabk` backup/restore with a pre-restore recovery export.
- Indonesian and English throughout, including messages produced outside composition (D-021).
- Guided tour drawn as a real canvas cutout over the live UI.
- A Material 3 colour system generated from the brand green by Material Color Utilities, with the
  full role set for light and dark, money-specific roles harmonised toward the seed, and a
  categorical chart palette; 96 measured contrast pairs, all passing (D-028, D-029).
- A Gerakan setting that also honours the system animation scale (D-025).
- Golden integration tests against the verified facilitator workbook and a sanitized enrichment
  fixture, plus an end-to-end test that runs the real exporter and importer over the golden case.

### Changed

- Every modal in the app is one `MankeuSheet` bottom sheet; `AlertDialog` is no longer used (D-023).
- Sheets have a half-expanded snap point, actions that do not scroll away, and resize for the
  keyboard instead of hiding the field being typed into (D-031).
- Depth is carried by surface-container tones; drop shadows are light-mode only (D-030).
- Version string restarted at `0.0.1` and the term "SemVer" removed from user-facing text (D-016).

### Removed

- `saved_mappings` and the write-only `projects.status`, `templateKind` and `templateMetadata`
  columns. Database version 3 (D-026).
- The three disabled sign-in buttons on the account card; the notice explaining that sync is not
  built yet remains (D-027).

### Boundaries

- Package identity migrated to `com.mankeu`.
- Cloud sync, authentication, release signing, and Play publication are not implemented.
- Cloud sync is designed but not built: see `docs/CLOUD_SYNC_AND_AUTH.md` (D-033).
- Backups written before database version 3 cannot be restored by this build; the app is
  pre-release and uses a destructive migration fallback.
