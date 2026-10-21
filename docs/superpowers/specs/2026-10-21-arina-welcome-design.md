# Arina welcome and optional sign-in

User-confirmed: implement the recommended Money Manage composition and Wise hierarchy: Arina logo, compact bookkeeping artwork, the heading “Kelola usaha, mulai dari sini.”, Google sign-in, and a visible “Lanjut tanpa akun”. Use existing light/dark theme tokens and local Sora/Plus Jakarta Sans fonts.

Verified: Google sign-in already belongs to MankeuApp and is reused by account settings. Local data and reports must remain available without authentication. Authentication does not itself mean a Drive backup exists.

Decision: show the welcome screen only before the first entry into an empty installation. Persist completion as device UI state in AppPreferences; skip for existing businesses, authenticated users, and help captures. Both successful authentication and the offline action complete welcome. Cancelled/failed authentication does not complete it. The offline action stays available during sign-in. Do not change account, backup, database, or financial semantics.

Presentation: one bounded, scrollable column with 24 dp padding, native decorative bookkeeping artwork, accessible 56 dp buttons, and localized ID/EN copy. No registration/password form, additional provider, downloaded artwork, automatic backup, or new dependency. Errors use the existing snackbar host while the welcome page remains visible.

Acceptance: fresh launch offers both paths; offline action reaches the real home/setup and persists on relaunch; existing books and signed-in users bypass welcome; cancellation/failure preserves the page; repeated Google taps are disabled while busy; phone, dark, tablet, landscape, and 200% font layouts keep both actions reachable by scrolling. Run Compose instrumentation, unit tests, lint, and debug build; record actual evidence and any provider limitation.
