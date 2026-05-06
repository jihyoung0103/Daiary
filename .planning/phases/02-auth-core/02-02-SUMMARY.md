---
phase: 02-auth-core
plan: 02
subsystem: auth
tags: [android, kotlin, viewmodel, firebase-auth, error-handling]

# Dependency graph
requires:
  - phase: 01-project-foundation-firebase-setup
    plan: 02
    provides: AuthViewModel stub with raw e.message error exposure
provides:
  - AuthViewModel.mapAuthError() — Firebase exception → Korean user-facing message mapping
  - Handles FirebaseAuthInvalidCredentialsException, InvalidUser, UserCollision, WeakPassword, TOO_MANY_REQUESTS, NETWORK_REQUEST_FAILED
  - Email enumeration protection (INVALID_LOGIN_CREDENTIALS) handled via else branch
affects:
  - Phase 2 plan 03 (LoginFragment): errorTextView displays Korean messages from AuthUiState.Error(message)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "mapAuthError() — private ViewModel function, single place for Firebase error code → Korean message mapping"
    - "when {is X -> ...} type dispatch on FirebaseAuthException subclasses (no Firebase SDK instance calls in ViewModel)"
    - "Email enumeration protection: INVALID_LOGIN_CREDENTIALS falls through to else branch of FirebaseAuthInvalidCredentialsException"

key-files:
  created: []
  modified:
    - app/src/main/kotlin/com/example/capstone_login/ui/auth/AuthViewModel.kt

key-decisions:
  - "Firebase exception imports in ViewModel are type-check-only; FirebaseAuth.getInstance() not called (layer boundary)"
  - "else branch of FirebaseAuthInvalidCredentialsException covers INVALID_LOGIN_CREDENTIALS (email enumeration protection active since Sep 2023)"
  - "NETWORK_REQUEST_FAILED detected via e.message string match (not a FirebaseAuthException subclass)"

requirements-completed: [UI-02]

# Metrics
duration: ~3min
completed: 2026-05-06
---

# Phase 2 Plan 02: AuthViewModel Korean Error Mapping

**Raw `e.message` error exposure replaced with `mapAuthError()` function that maps Firebase exception subclasses to Korean user-facing messages, covering email enumeration protection and network errors.**

## Performance

- **Duration:** ~3 min
- **Completed:** 2026-05-06
- **Tasks:** 1
- **Files modified:** 1

## Accomplishments

- Added `private fun mapAuthError(e: Exception): String` to `AuthViewModel`
- `login()` error branch: `result.exception.message ?: "로그인 실패"` → `mapAuthError(result.exception)`
- `register()` error branch: `result.exception.message ?: "회원가입 실패"` → `mapAuthError(result.exception)`
- Covers 5 exception types + 2 string-match fallbacks (TOO_MANY_REQUESTS error code, NETWORK_REQUEST_FAILED message)
- Added 5 Firebase Auth exception type imports; no `FirebaseAuth.getInstance()` call (layer boundary maintained)

## Task Commits

1. **Task 1: Add mapAuthError + update error branches** — `13608eb` (feat)

## Files Created/Modified

- `app/src/main/kotlin/com/example/capstone_login/ui/auth/AuthViewModel.kt` — mapAuthError() added, error branches updated

## Decisions Made

- Layer boundary: Firebase imports in ViewModel limited to exception types only; no SDK instance access
- Comment mentioning `FirebaseAuth.getInstance()` removed from KDoc to satisfy grep-based acceptance criteria

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- KDoc comment contained `FirebaseAuth.getInstance()` literal, which failed the grep-based acceptance criterion. Removed the comment text; no functional change.

## Next Phase Readiness

Plan 02-03 (LoginFragment + CalendarFragment) depends on this plan — AuthViewModel.mapAuthError() is now complete.

---
*Phase: 02-auth-core*
*Completed: 2026-05-06*
