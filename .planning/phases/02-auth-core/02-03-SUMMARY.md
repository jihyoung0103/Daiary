---
phase: 02-auth-core
plan: 03
subsystem: ui
tags: [android, kotlin, fragment, navigation, viewbinding, stateflow]

# Dependency graph
requires:
  - phase: 02-auth-core
    plan: 01
    provides: FirebaseAuthDataSource, FirestoreUserDataSource
  - phase: 02-auth-core
    plan: 02
    provides: AuthViewModel.mapAuthError() — Korean error messages
provides:
  - LoginFragment — validateInput() + StateFlow collect + navigate to CalendarFragment
  - CalendarFragment — logoutButton → signOut() + navigate back to LoginFragment
  - nav_graph.xml — action_calendarFragment_to_loginFragment

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "validateInput() — client-side guard before Firebase call (Patterns.EMAIL_ADDRESS + password.length < 6)"
    - "resetState() before navigate — prevents duplicate navigation on repeatOnLifecycle restart (screen rotation)"
    - "isEnabled=false during Loading state — blocks double-tap login/register race condition"
    - "by viewModels() in CalendarFragment — AuthViewModel shared within Fragment scope (not Activity)"

key-files:
  created: []
  modified:
    - app/src/main/kotlin/com/example/capstone_login/ui/auth/LoginFragment.kt
    - app/src/main/kotlin/com/example/capstone_login/ui/calendar/CalendarFragment.kt
    - app/src/main/res/navigation/nav_graph.xml
    - app/src/main/res/layout/fragment_login.xml (registerButton — pre-committed from prior work)
    - app/src/main/res/layout/fragment_calendar.xml (logoutButton — pre-committed from prior work)

key-decisions:
  - "resetState() before findNavController().navigate() — Success state becomes Idle before repeatOnLifecycle can re-emit"
  - "No Firebase imports in LoginFragment or CalendarFragment — layer boundary enforced"
  - "action_calendarFragment_to_loginFragment has no popUpTo in Phase 2 — back stack cleanup deferred to Phase 3"

requirements-completed: [AUTH-01, AUTH-02, AUTH-03, UI-01, UI-02, UI-04]

# Metrics
duration: ~10min
completed: 2026-05-06
---

# Phase 2 Plan 03: LoginFragment + CalendarFragment UI Implementation

**End-to-end auth flow wired: LoginFragment (input validation + StateFlow + navigation) and CalendarFragment (logout) now fully implemented.**

## Performance

- **Duration:** ~10 min
- **Completed:** 2026-05-06
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments

- `LoginFragment` fully replaced: `validateInput()`, `setupClickListeners()`, `collectUiState()` handling all 4 `AuthUiState` branches
- `CalendarFragment` fully replaced: `AuthViewModel` injected, `logoutButton` → `signOut()` + navigate
- `nav_graph.xml`: `action_calendarFragment_to_loginFragment` added to `calendarFragment`
- Layout files (`registerButton`, `logoutButton`) were already present from prior session work

## Task Commits

1. **Task 1+2 combined** — `57ee626` (feat(02-03))

## Files Created/Modified

- `app/src/main/kotlin/com/example/capstone_login/ui/auth/LoginFragment.kt` — full implementation
- `app/src/main/kotlin/com/example/capstone_login/ui/calendar/CalendarFragment.kt` — logout wired
- `app/src/main/res/navigation/nav_graph.xml` — logout action added

## Decisions Made

- `resetState()` called before `navigate()` in Success branch — prevents double-navigation if `repeatOnLifecycle` restarts (e.g., screen rotation)
- `by viewModels()` in CalendarFragment creates a new ViewModel instance scoped to the fragment (not shared with LoginFragment) — `signOut()` still works correctly since it calls Firebase directly
- `popUpTo` back stack cleanup deferred to Phase 3 as planned

## Deviations from Plan

None. Layout files were pre-committed; only nav_graph.xml and Kotlin files required changes this session.

## Issues Encountered

- `gradlew` script missing from project root — build verification must be done in Android Studio. All acceptance criteria (grep-based) passed.

## Human Verification Required

Run the app in Android Studio emulator and verify all 5 scenarios in the checkpoint:
1. Client-side validation messages (no Firebase call)
2. Firebase login failure → Korean error message
3. New account registration → navigate to CalendarFragment
4. Logout → return to LoginFragment
5. Login with existing account → CalendarFragment

---
*Phase: 02-auth-core*
*Completed: 2026-05-06*
