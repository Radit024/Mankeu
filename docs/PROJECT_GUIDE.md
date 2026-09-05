---
status: active-planning
last_verified: 2026-08-15
audience: human-and-ai
sensitivity: internal
evidence_basis:
  - owner-confirmed workshop and product context dated 2026-08-15
  - ../arina-agri/AUDIT_REVISI_MANAJEMEN_KEUANGAN.md
  - ../arina-agri/references/CATATAN KEUANGAN PADI 1 Ha ADE.xlsx
---

# Project Guide

## Purpose

Build a standalone Arina Agri-branded Android application for practical financial management on phones and tablets. The product should serve people who may not own a laptop and may not have stable internet access.

The initial need came from workshop participants who asked for a mobile version of financial management. Their need was broader than cultivation: financial management should also work for agricultural machinery and equipment services, rentals, trading, and other small-business activities.

That request is evidence of a real problem. It is not yet evidence that a particular interface or feature set is usable; the product must be validated independently.

## Product thesis

Small-business operators need a low-friction way to record money movement, plan costs and revenue, understand cash position, compare plans with reality, and recover useful information from existing workbooks without becoming accountants or database operators.

The product must preserve accounting meaning while presenting familiar language such as `Uang Masuk`, `Uang Keluar`, `Anggaran`, `Piutang`, `Utang`, and `Ringkasan`.

## Relationship to Arina Agri web

The web application supplies three kinds of evidence:

1. **Intent evidence:** projection, realization, comparison, RAB/budget, daily transactions, income statement, cash flow, financing, import, export, and reports.
2. **Golden-case evidence:** the Padi 1 Ha workbook and its corrected expected values.
3. **Failure evidence:** domain assumptions, implementation gaps, and quality failures that the new application must not inherit.

The new product may reuse validated formulas, fixtures, terms, and test ideas. It must not copy the legacy module wholesale or treat visual parity as a requirement.

## Primary users

- Farmers managing one or more cultivation cycles.
- Agricultural machinery or equipment service providers.
- Small agricultural traders and processors.
- Other micro and small businesses that need basic project and cash management.
- Workshop facilitators who teach financial management using phones or tablets.

## Jobs to be done

- Record a transaction in seconds while working in the field or workshop.
- Know current cash, income, expense, obligations, and receivables without manual recapitulation.
- Create an estimate before starting a project and compare it with actual results.
- Import a known workbook and receive a correct, explained result.
- Export or back up data without being locked into an application server.
- Review richer reports on a tablet without losing the quick-entry experience on a phone.

## Current state

- Product direction and initial requirements: documented.
- Android implementation: not started.
- Package name and final brand name: intentionally undecided.
- Cloud sync and authentication: future experiments, not MVP dependencies.
- Play Store account, listing, signing, privacy declarations, and release artifacts: not created or verified.
- Research protocol: planned, not approved or executed.

## Success definition

Success is not the number of screens or features. A release succeeds when representative users can complete core tasks, every financial output can be traced and reconciled, offline use is dependable, phone and tablet layouts are usable, and import corrections are transparent.

## Sensitive boundary

The private history that motivated stronger governance is not part of this repository. The repository records only product requirements, engineering rules, objective legacy evidence, and contribution boundaries. Do not add private chat transcripts or personal accusations to product history.
