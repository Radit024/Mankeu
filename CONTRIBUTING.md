---
status: active
last_verified: 2026-08-15
audience: human-and-ai
sensitivity: internal
---

# Contributing

The product owner holds final authority over product meaning, source precedence, branding, publication, research claims, and release approval.

## Contribution workflow

1. Select a bounded task from `docs/ROADMAP.md` or create an issue with explicit acceptance criteria.
2. Identify the source evidence and affected domain invariants.
3. Make a small branch and keep unrelated changes out of the diff.
4. Add the required tests and evidence before requesting review.
5. Explain what was verified, what was not verified, and how a reviewer can reproduce the result.
6. Merge only after the product meaning and technical evidence both pass review.

## Pull-request evidence

A pull request that changes behavior must state:

- user problem and intended outcome;
- source or decision supporting the behavior;
- affected business rules;
- tests run and their result;
- phone and tablet behavior when UI changes;
- migration, backup, and rollback impact when data changes;
- privacy and security impact;
- remaining uncertainty.

Screenshots may support UI evidence but cannot substitute for functional tests. AI-generated summaries cannot substitute for direct inspection.

## Communication standard

Uncertainty and mistakes are allowed. Concealing uncertainty, dismissing a reproducible failure, or claiming completion without evidence is not.

Review product behavior rather than defending authorship. A technically large rework is acceptable when the current model contradicts the documented business intent.
