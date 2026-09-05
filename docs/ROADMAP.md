---
status: active-planning
last_verified: 2026-08-15
audience: human-and-ai
sensitivity: internal
evidence_basis:
  - docs/PRODUCT_REQUIREMENTS.md
  - docs/QUALITY_GATES.md
---

# Roadmap

## Scheduling rule

This roadmap is a dependency map, not a fixed queue. Independent tasks may proceed in parallel and priorities may change without implying that lower rows are less valuable. No delivery date is currently committed for this product.

## Workstreams

| ID | Workstream | Outcome | Status | Depends on |
| --- | --- | --- | --- | --- |
| AAC-001 | Product baseline | Requirements, domain, import, UX, architecture, quality, research, and governance are documented | complete 2026-08-15 | none |
| AAC-002 | Open product decisions | Resolve final name timing, package ID, minimum SDK/device baseline, and release ownership | ready for owner decision | AAC-001 |
| AAC-003 | Native foundation | Reproducible Kotlin/Compose project with static checks and adaptive shell | MVP implemented; device matrix pending | AAC-001, provisional non-release foundation values |
| AAC-004 | Domain kernel | Exact money, balanced events, businesses/projects, books, budget, and report calculations | partial: event/posting and core reports unit-verified; books/budget pending | AAC-001 |
| AAC-005 | Local persistence | Room schema, repositories, audit events, migrations, and recovery tests | partial: schema 1 and atomic writes implemented; migration/recovery suite pending | AAC-004 |
| AAC-006 | Core mobile workflows | Onboarding, business/project scope, money in/out, budgets, and reports on phone/tablet | partial: scope, entry, and reports implemented; budgets and device UX evidence pending | AAC-003–005 |
| AAC-007 | Golden importer | Five-sheet reference import, reconciliation, provenance, notifications, and reversal | partial: preview, canonical enrichment, atomic commit, provenance/notice, duplicate guard; conflict UI and reversal pending | AAC-004–005 |
| AAC-008 | Export and round trip | Five-sheet export, comparison export, reimport preservation, and safe sharing | planned | AAC-005, AAC-007 |
| AAC-009 | Receivables/payables | Simple obligations, due states, partial settlement, and linked cash events | planned for pre-1.0 | AAC-004–006 |
| AAC-010 | Backup/restore | Encrypted versioned local backup, restore, and failure recovery | planned for pre-1.0 | AAC-005 |
| AAC-011 | Drive/auth experiments | Evidence-backed prototypes for optional Drive snapshot/sync and Firebase auth | optional later | AAC-010, owner ADR |
| AAC-012 | Beta readiness | Privacy, signing, target API, device matrix, release evidence, support/rollback | planned | core release scope complete |
| AAC-013 | Research validation | Formative tests, pilot/workshop, SUS, DOI analysis, and publication-quality evidence | planned; protocol first | usable beta |

## Milestone outcomes

### M0 — Documented baseline

Exit evidence:

- authoritative documents exist and cross-link;
- owner-confirmed decisions and open decisions are distinguishable;
- reference workbook hash, corrections, and golden values are fixed;
- private interpersonal context is excluded from product history.

### M1 — Trustworthy core

Exit evidence:

- pure Kotlin domain tests pass independently of Android;
- every user event balances;
- reports reconcile exactly;
- projection and realization remain independent;
- golden values can be produced from sanitized domain fixtures.

### M2 — Useful offline application

Exit evidence:

- APK installs on representative phone and tablet devices;
- no login/network is required;
- business/project, entry, budget, and report workflows survive process restart;
- accessibility and adaptive-layout checks pass.

### M3 — Safe data mobility

Exit evidence:

- supported workbook imports atomically with enrichment notices;
- export/reimport preserves semantics;
- encrypted backup/restore survives a loss simulation;
- migration and rollback behavior are reproducible.

### M4 — Public beta candidate

Exit evidence:

- simple receivables/payables are complete or explicitly moved by owner decision;
- privacy, signing custody, Play requirements, support, known limitations, and release rollback are approved;
- no stop-ship condition remains.

### M5 — Validated product and research basis

Exit evidence:

- representative participants complete core tasks;
- SUS is calculated from raw responses with a reproducible pipeline;
- Rogers Diffusion of Innovation constructs are operationalized before analysis;
- product, workshop, and journal claims match the collected evidence.

## Best first implementation slice

Build a vertical but non-published slice:

1. create one business and one project;
2. record cash income, cash expense, owner capital, and loan;
3. persist them with balanced internal entries;
4. show reconciled profit/loss and cash flow;
5. run the same domain rules against golden fixture values;
6. render the flow at phone and tablet widths.

This slice validates the architecture without prematurely creating Play, Firebase, Drive, or final branding assets.
