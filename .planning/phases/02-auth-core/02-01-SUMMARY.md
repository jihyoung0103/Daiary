---
phase: 02-auth-core
plan: 01
subsystem: auth
tags: [android, firebase, firestore, kotlin, coroutines]

# Dependency graph
requires:
  - phase: 01-project-foundation-firebase-setup
    plan: 02
    provides: FirebaseAuthDataSource and FirestoreUserDataSource stubs
provides:
  - FirebaseAuthDataSource.signIn() — real signInWithEmailAndPassword().await() call
  - FirebaseAuthDataSource.signUp() — real createUserWithEmailAndPassword().await() call
  - FirestoreUserDataSource.upsertUser() — set(merge=true) + FieldValue.serverTimestamp(), non-fatal
affects:
  - Phase 2 plan 03 (LoginFragment): Fragment can now trigger real auth via ViewModel → Repository → DataSource

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "kotlinx.coroutines.tasks.await() — Task<T>.await() extension converts Firebase Task to suspend fun"
    - "SetOptions.merge() — idempotent upsert; safe to call on every login, preserves other user fields"
    - "Non-fatal Firestore write — catch(Exception) + Log.w(), never rethrow; auth success not blocked by Firestore"

key-files:
  created: []
  modified:
    - app/src/main/kotlin/com/example/capstone_login/data/source/FirebaseAuthDataSource.kt
    - app/src/main/kotlin/com/example/capstone_login/data/source/FirestoreUserDataSource.kt

key-decisions:
  - "upsertUser() catches all exceptions internally — DataSource-level non-fatal guarantees auth flow is never blocked"
  - "FieldValue.serverTimestamp() used for lastLoginAt — server-side timestamp avoids client clock skew"
  - "IllegalStateException thrown when Firebase returns null user after success — guards against unexpected SDK behavior"

requirements-completed: [AUTH-01, AUTH-02]

# Metrics
duration: ~5min
completed: 2026-05-06
---

# Phase 2 Plan 01: Firebase DataSource Implementation

**NotImplementedError stubs replaced with real Firebase Auth and Firestore calls — signIn/signUp use .await() pattern, upsertUser writes to users/{uid} with merge and non-fatal error handling.**

## Performance

- **Duration:** ~5 min
- **Completed:** 2026-05-06
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- `FirebaseAuthDataSource.signIn()`: `throw NotImplementedError` → `auth.signInWithEmailAndPassword(email, password).await().user ?: throw IllegalStateException(...)`
- `FirebaseAuthDataSource.signUp()`: `throw NotImplementedError` → `auth.createUserWithEmailAndPassword(email, password).await().user ?: throw IllegalStateException(...)`
- `FirestoreUserDataSource.upsertUser()`: `throw NotImplementedError` → try/catch with `db.collection("users").document(uid).set(mapOf("email" to email, "lastLoginAt" to FieldValue.serverTimestamp()), SetOptions.merge()).await()`; exceptions caught and logged, never rethrown

## Task Commits

1. **Task 1+2: Replace DataSource stubs** — `27580f6` (feat)

## Files Created/Modified

- `app/src/main/kotlin/com/example/capstone_login/data/source/FirebaseAuthDataSource.kt` — signIn/signUp now call Firebase Auth SDK
- `app/src/main/kotlin/com/example/capstone_login/data/source/FirestoreUserDataSource.kt` — upsertUser now writes to Firestore with merge + non-fatal handling

## Decisions Made

- Non-fatal handling at DataSource level (in addition to Repository's `runCatching`) for defense-in-depth
- Added `android.util.Log` and `com.google.firebase.firestore.FieldValue` imports to FirestoreUserDataSource

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## Next Phase Readiness

Plan 02-02 (AuthViewModel error mapping) is independent of this plan and was executed in parallel.
Plan 02-03 (Fragment UI) depends on this plan — DataSource layer is now complete.

---
*Phase: 02-auth-core*
*Completed: 2026-05-06*
