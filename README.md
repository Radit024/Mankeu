---
status: mvp-implemented
last_verified: 2026-08-15
audience: human-and-ai
sensitivity: internal
evidence_basis:
  - owner-confirmed product decisions dated 2026-08-15
  - ../arina-agri/AUDIT_REVISI_MANAJEMEN_KEUANGAN.md
  - ../arina-agri/references/CATATAN KEUANGAN PADI 1 Ha ADE.xlsx
  - ../arina-agri current finance source and tests at commit 949a9da
  - ../arina-finance-workbook-fixtures validated corpus at commit fcfd97d
---

# UMKM Man Keu (Manajemen Keuangan UMKM)

> Aplikasi Android native pencatatan dan manajemen keuangan untuk pelaku UMKM (Usaha Mikro, Kecil, dan Menengah).

UMKM Man Keu adalah aplikasi manajemen keuangan mobile-first yang dirancang khusus untuk memenuhi kebutuhan pembukuan praktis bagi pengusaha UMKM di berbagai sektor:
- **UMKM Umum / Dagang**: Toko kelontong, kios, warung, reseller, retail, dan e-commerce.
- **Kuliner / F&B**: Kafe, warung makan, resto, katering, bakery, dan pedagang jajanan.
- **Jasa & Servis**: Bengkel, salon, laundry, rental/persewaan, jasa reparasi, dan freelance.
- **Pertanian & Agribisnis**: Usaha tani, perkebunan, peternakan, dan sarana produksi pertanian.

This is an independent native Android finance application built for MSMEs and small businesses. The application preserves intuitive workflows while replacing immature domain assumptions, incomplete reconciliation, desktop-shaped interaction, and weak end-to-end guarantees.

## Product commitment

The application must be:

- significantly more usable and reliable than legacy accounting tools;
- universal at its core, with sector-specific behavior supplied by optional templates;
- native Android, mobile-first, and adaptive on tablets;
- local-first and usable without an account or reliable internet connection;
- transparent when it infers, repairs, or enriches imported data;
- exact and auditable: financial outputs must reconcile to their sources;
- simple for non-accountants without sacrificing sound internal accounting semantics.

## Owner-confirmed decisions

| Decision | Status |
| --- | --- |
| One user may manage multiple businesses and each business may manage multiple projects/jobs | user-confirmed |
| Version one is local-first and does not require login | user-confirmed |
| Optional sync may later use each user's Google Drive instead of requiring an application-owned database server | user-confirmed direction; design pending |
| Authentication may be explored later with Firebase | user-confirmed direction; experiment pending |
| The product is Mankeu, an independently publishable multi-sector MSME finance app | user-confirmed |
| Android phone is the primary device; tablets must receive an adaptive layout | user-confirmed |
| Kotlin and Jetpack Compose are the implementation baseline | decided for the native Android goal |
| Simple receivables and payables are included before the public 1.0 release | accepted planning baseline |
| Full inventory management and full invoicing are not part of the first implementation milestone | scope boundary |

## Current state

The repository contains an installable **native Android MVP** built with Kotlin, Jetpack Compose, and Room. The implemented vertical slice can:

- create and switch local businesses and projects, including the explicit `Umum` workspace;
- record cash income, cash expense, owner capital, and loan receipts as exact integer rupiah;
- create balanced internal postings and derive profit/loss, operating cash, financing cash, and cash balance;
- adapt primary navigation between phone and tablet widths;
- preview a five-view `.xlsx` workbook through a native OOXML reader;
- reconcile split daily transactions against coordinated workbook evidence;
- recover deterministic missing facts such as `Sewa Lahan Rp7.000.000` and persist a visible provenance-backed notice;
- commit an accepted import atomically and reject a duplicate workbook hash.

This is an evidence-labeled MVP, not a public release. Budget/projection flows, receivables/payables, export, encrypted backup, import reversal, migrations beyond schema 1, final branding, production package ID, signing, Firebase, Drive, and Play publication remain unimplemented or undecided. See [MVP Implementation Report](docs/MVP_IMPLEMENTATION_REPORT.md).

## Read in this order

1. [Implementation Progress](docs/IMPLEMENTATION_PROGRESS.md) — live interruption-recovery checkpoint
2. [Web Finance Capability Matrix](docs/WEB_FINANCE_CAPABILITY_MATRIX.md)
3. [V1 Implementation Specification](docs/V1_IMPLEMENTATION_SPEC.md)
4. [Project Guide](docs/PROJECT_GUIDE.md)
5. [Product Requirements](docs/PRODUCT_REQUIREMENTS.md)
6. [Domain Model](docs/DOMAIN_MODEL.md)
7. [Reference Workbook Contract](docs/REFERENCE_WORKBOOK_CONTRACT.md)
8. [Import, Reconciliation, and Notifications](docs/IMPORT_RECONCILIATION_AND_NOTIFICATIONS.md)
9. [UX and Accessibility](docs/UX_AND_ACCESSIBILITY.md)
10. [Architecture](docs/ARCHITECTURE.md)
11. [Quality Gates](docs/QUALITY_GATES.md)
12. [Roadmap](docs/ROADMAP.md)
13. [Research and Validation](docs/RESEARCH_AND_VALIDATION.md)
14. [Governance](docs/GOVERNANCE.md)
15. [Legacy Web Evidence](docs/LEGACY_WEB_EVIDENCE.md)
16. [Decision Log](docs/DECISION_LOG.md)
17. [Evidence Index](docs/EVIDENCE_INDEX.md)
18. [MVP Implementation Report](docs/MVP_IMPLEMENTATION_REPORT.md)
19. [Semantic Matching Strategy](docs/SEMANTIC_MATCHING.md)
20. [Manual MVP Test Guide](docs/MANUAL_TEST_GUIDE.md)

## Run the MVP

Requirements: JDK 17 or newer and Android SDK Platform 36. Keep SDK paths in local environment configuration; never commit `local.properties` from one machine.

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk` with the `com.mankeu` package identity.

The golden workbook integration tests use the private sibling repository `../arina-finance-workbook-fixtures` when available. They are explicitly skipped in an isolated checkout rather than silently substituting different evidence.

See [Quality Gates](docs/QUALITY_GATES.md) before calling any milestone complete.

For owner-facing installation, transaction, persistence, scope-isolation, and synthetic workbook checks, follow the [Manual MVP Test Guide](docs/MANUAL_TEST_GUIDE.md).

## Privacy and licensing

Financial records, imports, backups, research responses, and user identity data are private by default. Never commit real user data, credentials, signing material, Firebase configuration, or Drive tokens.

The software license has not been selected. Do not assume the repository is open source merely because Git is used.
