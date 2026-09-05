---
status: baseline
last_verified: 2026-08-15
audience: human-and-ai
sensitivity: internal
evidence_basis:
  - owner-confirmed decisions dated 2026-08-15
  - docs/PROJECT_GUIDE.md
  - docs/REFERENCE_WORKBOOK_CONTRACT.md
---

# Product Requirements

## Product shape

Arina Agri Calculator is a standalone Android application with a universal financial core and optional business templates. It is not limited to cultivation and does not require another Arina Agri product to operate.

## Functional requirements

### Businesses and projects

- **FR-BIZ-001:** A user can create, rename, archive, restore, and delete multiple businesses.
- **FR-BIZ-002:** A business can contain multiple projects, jobs, seasons, or activities.
- **FR-BIZ-003:** Transactions that do not belong to a specific project are assigned to an explicit `Umum` workspace, never an invisible null bucket.
- **FR-BIZ-004:** Agriculture-specific attributes are optional template metadata, not required business fields.

### Simple financial entry

- **FR-TXN-001:** The primary entry flow supports money in and money out with date, amount, description, category, business, and optional project.
- **FR-TXN-002:** Quantity, unit, unit price, counterparty, payment method, attachment, and notes are optional.
- **FR-TXN-003:** The UI does not expose debit and credit as required user input, while the internal model preserves balanced financial meaning.
- **FR-TXN-004:** Editing or deleting a posted transaction leaves an audit event and updates every derived report deterministically.
- **FR-TXN-005:** Loan principal, owner capital, loan repayment, interest, revenue, and expense remain semantically distinct.

### Budget, projection, and realization

- **FR-PLAN-001:** Each project can contain a complete projection and a complete realization dataset.
- **FR-PLAN-002:** Both datasets can contain budget/recap items, dated transaction details, income statement, cash flow, financing, and unit-economics assumptions.
- **FR-PLAN-003:** Comparison reads final projection and realization outputs and never acts as a transaction source.
- **FR-PLAN-004:** A user can duplicate a projection as the starting structure for realization without linking future edits implicitly.

### Reports

- **FR-RPT-001:** Provide income and expense summary, profit/loss, cash flow, cumulative cash, financing cash flow, and budget-versus-actual variance.
- **FR-RPT-002:** Show HPP, break-even production, and B/C ratio only when their required unit and production assumptions exist.
- **FR-RPT-003:** Every report offers a path back to its contributing records.
- **FR-RPT-004:** An unexplained reconciliation difference is visible and cannot be labeled complete.
- **FR-RPT-005:** Reports are read-only outputs; corrections happen at the originating record or through an explicit adjustment.

### Receivables and payables

- **FR-ARAP-001:** Before public 1.0, support simple receivables and payables with counterparty, issue date, due date, original amount, remaining amount, status, and notes.
- **FR-ARAP-002:** Partial settlement is supported and creates linked cash transactions.
- **FR-ARAP-003:** Status is derived from balances and dates: draft, open, partially paid, paid, overdue, or cancelled.
- **FR-ARAP-004:** A receivable is not cash income until collected, and a payable is not cash outflow until paid.
- **FR-ARAP-005:** Full invoice generation, tax, inventory valuation, and purchase-order workflow are outside the first implementation milestone.

### Import, export, and backup

- **FR-IMP-001:** A supported workbook is read as a coordinated multi-sheet source.
- **FR-IMP-002:** Import runs preview, validation, reconciliation, enrichment classification, confirmation when required, and atomic commit.
- **FR-IMP-003:** Automatic enrichment produces a visible notification and provenance record.
- **FR-IMP-004:** Import can be reviewed and reversed by batch.
- **FR-IMP-005:** Reimport offers replace, merge, or cancel with explicit consequences and duplicate detection.
- **FR-EXP-001:** Export can reproduce budget, transactions, income statement, cash flow, post-financing cash flow, and comparison.
- **FR-BKP-001:** Local backup and restore work without an account.
- **FR-BKP-002:** Backups are versioned, integrity-checked, and encrypted before public release.
- **FR-SYNC-001:** Google Drive sync/backup and Firebase authentication are optional later experiments, not core dependencies.

### Notifications and recovery

- **FR-NOT-001:** Notifications distinguish information, automatic recovery, action required, conflict, and failure.
- **FR-NOT-002:** A recovery notification describes the changed record, amount, source evidence, assumptions, and review/undo action.
- **FR-NOT-003:** Offline, interrupted import, failed restore, and storage errors provide a concrete recovery path.

## Non-functional requirements

- **NFR-001 Exactness:** Stored money uses integer rupiah and deterministic rounding policies.
- **NFR-002 Offline:** Core entry, reports, import review, and backup creation remain usable without network access.
- **NFR-003 Performance:** Common entry and report interactions remain responsive on representative lower-middle Android hardware; exact device baseline is pending field confirmation.
- **NFR-004 Adaptivity:** Core tasks work at compact, medium, and expanded widths without mandatory horizontal scrolling.
- **NFR-005 Accessibility:** Touch targets, contrast, labels, text scaling, keyboard navigation on tablets, and reduced motion are tested.
- **NFR-006 Privacy:** Financial data is private by default; no analytics or cloud copy is enabled silently.
- **NFR-007 Auditability:** Every derived number and import mutation has traceable inputs.
- **NFR-008 Recoverability:** Destructive actions, migrations, imports, and restores have explicit rollback or recovery behavior.
- **NFR-009 Testability:** Domain behavior can be tested without Android UI or network services.

## Out of scope for the first code milestone

- iOS implementation;
- full inventory and cost-of-goods engine;
- tax filing or regulated accounting compliance claims;
- payroll;
- marketplace, payments, or banking integration;
- multi-user collaboration and server-authoritative sync;
- AI financial advice;
- final Play Store branding and monetization.

These boundaries may change through an owner-approved decision record; they must not expand silently during implementation.
