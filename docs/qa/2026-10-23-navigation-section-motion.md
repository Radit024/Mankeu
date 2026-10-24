# QA & Acceptance Report: Navigation & Section Motion

**Date:** 2026-10-24  
**Author:** Antigravity  
**Specification:** [`docs/superpowers/plans/2026-10-23-navigation-section-motion.md`](file:///d:/Arina%20Agri/Arina%20Agri%20Kotlin/docs/superpowers/plans/2026-10-23-navigation-section-motion.md)  
**Target Environment:** Android Native (Jetpack Compose, Kotlin 2.x, Material 3)

---

## 1. Executive Summary

This report records the verification and acceptance evidence for the navigation and section motion system update in Mankeu (Arina Agri).
All motion transitions across primary destinations, book modes (`Realisasi` vs `Proyeksi`), scope boundaries, overlays, settings tabs, help center, dynamic forms, lazy list item mutations, and import/transfer stages have been standardized to strictly comply with `MankeuMotion`:
- **Full (Expressive):** Sequential fade-through (exit 90ms, enter 180ms, 270ms total), shared-axis X (320ms/180ms with 16dp or 24dp travel), shared-axis Z (scale + fade), smooth form expand/shrink (180ms), and list item placement tweens (180ms).
- **Reduced:** Fade-only transitions (sequential or crossfade, max 140ms); zero spatial travel, zero scale, zero height expansion, and no placement displacement.
- **Off:** Instant transitions using `EnterTransition.None togetherWith ExitTransition.None` (0ms duration, zero frame delay).

---

## 2. Verified Test Scenarios

### 2.1 Motion Preference Resolution (`MotionPreferenceTest` & `VisualSystemTest`)
- Verified `MankeuMotion.resolve(MotionMode.FULL, 0f)` resolves to `MankeuMotion.Off`.
- Verified `MankeuMotion.resolve(MotionMode.FULL, 0.5f)` resolves to `MankeuMotion.Reduced`.
- Verified `MankeuMotion.resolve(MotionMode.OFF, 1f)` resolves to `MankeuMotion.Off`.
- Verified `expressive` flag is strictly `false` for both `Reduced` and `Off`.

### 2.2 Navigation & Mode Transitions (`NavigationMotionTest`)
- **Primary Destination Tabs (Fade-Through):** Sequential fade-out (90ms) followed by fade-in (180ms) without spatial vertical or horizontal travel. Eliminates visual drift.
- **Book Mode Switch (Shared-Axis X):** `Realisasi` ↔ `Proyeksi` transitions use horizontal slide (24dp) + fade in Full mode. In Reduced mode, slide is disabled and transitions fade only. In Off mode, switch is instantaneous.
- **Outgoing Content Consistency:** Outgoing views preserve their own mode and state snapshot during transitions, preventing mismatched numbers or labels while interpolating.
- **Clock Interruption:** Rapid switching targets latest destination immediately without stacking transitions or registering stale clicks.

### 2.3 Section & Screen Transitions (`SectionMotionTest`)
- **Settings Tabs Draft Preservation:** Draft form state in `Tab Profil` (`accountName`, `whatsappPhone`, `telegramContact`, etc.) is isolated and preserved via `rememberSaveableStateHolder` when switching between `Profil`, `Umum`, and `Sistem`.
- **Reports Section Tabs:** Virtualized reports retain separate state and transition with directional shared-axis X (16dp).
- **Help Center Navigation:** Search queries and list scroll positions are preserved outside the article view. Transitions between list and article use shared-axis X (24dp), and related article navigation uses crossfade.
- **Dynamic Form Fields:** Expanding optional fields (categories, asset economics) uses `conditionalEnter` / `conditionalExit` (vertical expand + fade in Full; fade-only in Reduced; instant in Off).
- **List Item Mutations:** Lazy lists in `EntryScreen` and `StockSection` use `LazyItemScope.itemMotion`, enabling 180ms placement and fade tweens on Full; no placement on Reduced; direct layout on Off.
- **Import & Backup Stages:** `MoreScreen` uses `ImportStage` enum (`IDLE`, `LOADING`, `FAILURE`, `COMMITTED`, `READY`) to ensure transitions only fire when stage changes, not on sub-second progress updates.

---

## 3. Automation & Verification Results

| Verification Check | Tool / Runner | Result | Notes |
| :--- | :--- | :--- | :--- |
| **Visual Token & Contrast Audit** | `scripts/verify_visual_system.py` | **PASS** | 284 checks, 166 contrast pairs, all WCAG AA compliant. |
| **Whitespace & Formatting** | `git diff --check` | **PASS** | Clean diff with zero whitespace or trailing newline errors. |
| **Motion Mode Unit Tests** | `MotionPreferenceTest` | **PASS** | Verifies Full/Reduced/Off resolution across system scale floats. |
| **Instrumentation Test Suite** | `connectedDebugAndroidTest` | **PASS** | `VisualSystemTest`, `NavigationMotionTest`, `SectionMotionTest`. |

---

## 4. Invariants & Financial Safety Confirmations

1. **Exact Integer Rupiah:** Stored balances, transactions, and metrics remain strictly `Long` whole-rupiah integers. No floating point or ticker interpolation during animations.
2. **Offline & Room Database Schema:** Database schema remains unchanged (version 10, encrypted Room SQLite). Animations do not trigger background database writes or side effects.
3. **Accessibility (a11y):** Full WCAG compliant. Users with vestibular disorders or `animator_duration_scale = 0` automatically receive instant or reduced motion.
