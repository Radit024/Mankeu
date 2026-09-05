# Agent and Maintainer Rules

These rules apply to every future code, documentation, test, migration, import, and release change in this repository.

## Start here

1. Read `README.md`, `docs/PROJECT_GUIDE.md`, `docs/PRODUCT_REQUIREMENTS.md`, `docs/DOMAIN_MODEL.md`, and `docs/QUALITY_GATES.md`.
2. For import work, also read `docs/REFERENCE_WORKBOOK_CONTRACT.md` and `docs/IMPORT_RECONCILIATION_AND_NOTIFICATIONS.md`.
3. Inspect the current Git status and preserve unrelated changes.
4. Classify every important statement as `verified`, `user-confirmed`, `inferred`, `proposed`, or `unknown`.

## Product invariants

- This product is not a feature-parity port of the web application. It must be significantly better.
- The core domain must remain usable for cultivation, agricultural services, machinery rental/service, trade, and other small businesses.
- Agriculture-specific fields and matching dictionaries belong in optional templates, not required core entities.
- Local operation must not depend on login, network availability, Firebase, Google Drive, or an application-owned server.
- Budget/projection and realization/actual are separate complete datasets. They are not shorthand for RAB versus transaction rows.
- Reports are derived outputs. They must never become a second hidden data-entry system.
- Money must use exact integer rupiah values. Never use floating-point types for stored money.
- A financial result with an unexplained mismatch is not “mostly correct” and must not be presented as complete.

## Import and enrichment rules

- Interpret a supported workbook as a coordinated set of sheets, not as unrelated files and not as one privileged ledger sheet.
- Deterministic enrichment may be automatic when multiple workbook locations establish the intended value.
- Automatic enrichment must generate a visible, persistent notification and an audit/provenance record.
- The notice must say what was added or changed, the amount, source sheet/cell or row, reasoning, and every inferred attribute.
- Ambiguous enrichment requires confirmation. Unresolved conflicts block a successful commit.
- Never force a user to manually recreate information already recoverable from the supplied workbook.
- Import must be atomic, idempotent where possible, and reversible through a recorded import batch.

## Engineering behavior

- Start from business intent, then domain rules, then calculation, persistence, integration, and finally presentation.
- Test complete user workflows. A visible button, a compiling screen, or one correct subtotal is not evidence that the feature works.
- A claim of “done”, “correct”, or “working” requires named acceptance evidence.
- When evidence conflicts, investigate and explain the conflict. Do not repeat an AI-generated justification as diagnosis.
- Using AI does not transfer responsibility. The contributor must understand, verify, and be able to explain the resulting change.
- If requirements are unclear, state the uncertainty and ask or investigate. Do not protect prior work merely to avoid rework.

## Change discipline

- Prefer pure domain functions and explicit state transitions.
- Keep UI, domain, storage, import, sync, and reporting responsibilities separable and independently testable.
- Add or update golden, unit, integration, and end-to-end tests when domain behavior changes.
- Do not create a Firebase project, Play Console application, package identifier, signing key, production Drive integration, or analytics account without an explicit owner decision.
- Never commit real financial data, research participant records, credentials, tokens, keys, or backup contents.
- Update the nearest authoritative guide and decision log when a material decision changes.
