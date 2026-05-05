---
phase: 01-project-foundation-firebase-setup
plan: 02
subsystem: auth
tags: [kotlin, android, mvvm, stateflow, firebase-auth, firestore, viewbinding, navigation]

# Dependency graph
requires:
  - phase: 01-project-foundation-firebase-setup
    plan: 01
    provides: Gradle build system with Firebase BoM, ViewBinding, and all dependency aliases
provides:
  - MVVM architecture skeleton with 10 Kotlin source files across model/util/data/ui layers
  - Domain User data class (no Android/Firebase deps) — safe for unit tests
  - Generic Result<T> sealed class wrapper for repository operations
  - FirebaseAuthDataSource with lazy FirebaseAuth initialization (Pitfall 2 prevention)
  - FirestoreUserDataSource with lazy FirebaseFirestore initialization (Pitfall 2 prevention)
  - AuthRepository combining Auth + Firestore DataSources via constructor injection
  - AuthUiState sealed class (Idle/Loading/Success/Error) for StateFlow-driven UI
  - AuthViewModel with private MutableStateFlow and read-only StateFlow<AuthUiState>
  - LoginFragment stub with ViewBinding, by viewModels(), and repeatOnLifecycle(STARTED) observer
  - CalendarFragment placeholder stub with ViewBinding null-cleanup
  - MainActivity single-activity shell with ViewBinding and NavHostFragment comment placeholders
affects:
  - 01-03 (Firebase Console setup + google-services.json — this skeleton is the source layer that requires it)
  - Phase 2 Auth Core (builds directly on these stubs — implements actual Firebase calls)
  - Phase 3 Navigation (fills in commented-out NavController and auth-state routing in MainActivity)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Layer boundary: only data/source/ files call FirebaseAuth.getInstance() and FirebaseFirestore.getInstance()"
    - "Lazy initialization: `private val auth by lazy { FirebaseAuth.getInstance() }` prevents pre-init crash"
    - "StateFlow encapsulation: private MutableStateFlow exposed as read-only StateFlow via asStateFlow()"
    - "ViewBinding null-cleanup: _binding = null in onDestroyView() prevents Fragment memory leaks"
    - "by viewModels() delegate: no Hilt or manual factory needed for capstone scope"
    - "repeatOnLifecycle(STARTED): safe StateFlow collection respecting Fragment lifecycle"

key-files:
  created:
    - app/src/main/kotlin/com/example/capstone_login/model/User.kt
    - app/src/main/kotlin/com/example/capstone_login/util/Result.kt
    - app/src/main/kotlin/com/example/capstone_login/data/source/FirebaseAuthDataSource.kt
    - app/src/main/kotlin/com/example/capstone_login/data/source/FirestoreUserDataSource.kt
    - app/src/main/kotlin/com/example/capstone_login/data/repository/AuthRepository.kt
    - app/src/main/kotlin/com/example/capstone_login/ui/auth/AuthUiState.kt
    - app/src/main/kotlin/com/example/capstone_login/ui/auth/AuthViewModel.kt
    - app/src/main/kotlin/com/example/capstone_login/ui/auth/LoginFragment.kt
    - app/src/main/kotlin/com/example/capstone_login/ui/calendar/CalendarFragment.kt
    - app/src/main/kotlin/com/example/capstone_login/ui/MainActivity.kt
  modified: []

key-decisions:
  - "AuthRepository imports FirebaseUser and FirebaseAuthException as type references — this is acceptable; the prohibition is on calling FirebaseAuth.getInstance() outside DataSource classes"
  - "AuthUiState.Success holds FirebaseUser — this is per plan; Fragment uses it only for navigation trigger, not for display"
  - "Stub implementations throw NotImplementedError — Phase 2 replaces stubs with actual Firebase calls"

patterns-established:
  - "Pattern: lazy initialization for all Firebase SDK instances to prevent pre-init crash"
  - "Pattern: Repository wraps DataSource exceptions in Result.Error — ViewModel never catches raw Firebase exceptions"
  - "Pattern: StateFlow<AuthUiState> as single source of truth for UI state — no LiveData, no multiple fields"

requirements-completed: [SETUP-03]

# Metrics
duration: 8min
completed: 2026-05-05
---

# Phase 1 Plan 02: MVVM Architecture Skeleton Summary

**10 Kotlin source files establishing MVVM skeleton with lazy Firebase DataSources, Result<T> wrapper, and StateFlow<AuthUiState> contract — no Firebase calls yet, correct layer boundaries enforced**

## Performance

- **Duration:** 8 min
- **Started:** 2026-05-05T00:00:00Z
- **Completed:** 2026-05-05T00:08:00Z
- **Tasks:** 3
- **Files modified:** 10

## Accomplishments

- Created complete MVVM architecture skeleton spanning model, util, data, and ui layers
- Enforced layer boundary invariant: only `data/source/` classes call `FirebaseAuth.getInstance()` and `FirebaseFirestore.getInstance()`
- Applied Pitfall 2 mitigation: all Firebase SDK instances use `by lazy { }` initialization to prevent pre-init crashes
- Established StateFlow-based state contract: private `MutableStateFlow` in AuthViewModel exposed as read-only `StateFlow<AuthUiState>` via `asStateFlow()`

## Task Commits

Each task was committed atomically:

1. **Task 1: Create model/User.kt and util/Result.kt foundation classes** - `7857f10` (feat)
2. **Task 2: Create data layer — FirebaseAuthDataSource, FirestoreUserDataSource, AuthRepository** - `e5af891` (feat)
3. **Task 3: Create UI layer — AuthUiState, AuthViewModel, LoginFragment, CalendarFragment, MainActivity** - `9787dfd` (feat)

## Files Created/Modified

- `app/src/main/kotlin/com/example/capstone_login/model/User.kt` - Domain User data class (uid, email, displayName) — no Android/Firebase imports
- `app/src/main/kotlin/com/example/capstone_login/util/Result.kt` - Sealed Result<T> with Success<T>, Error, Loading variants — pure Kotlin
- `app/src/main/kotlin/com/example/capstone_login/data/source/FirebaseAuthDataSource.kt` - Firebase Auth wrapper with lazy init; stub signIn/signUp throw NotImplementedError
- `app/src/main/kotlin/com/example/capstone_login/data/source/FirestoreUserDataSource.kt` - Firestore wrapper with lazy init; stub upsertUser throws NotImplementedError
- `app/src/main/kotlin/com/example/capstone_login/data/repository/AuthRepository.kt` - Combines Auth + Firestore DataSources; wraps all exceptions in Result.Error
- `app/src/main/kotlin/com/example/capstone_login/ui/auth/AuthUiState.kt` - Sealed class with Idle/Loading/Success(FirebaseUser)/Error(String) variants
- `app/src/main/kotlin/com/example/capstone_login/ui/auth/AuthViewModel.kt` - ViewModel with private MutableStateFlow, public read-only StateFlow, login/register/signOut functions
- `app/src/main/kotlin/com/example/capstone_login/ui/auth/LoginFragment.kt` - Fragment stub with ViewBinding, by viewModels(), and repeatOnLifecycle(STARTED) observer
- `app/src/main/kotlin/com/example/capstone_login/ui/calendar/CalendarFragment.kt` - Placeholder Fragment with ViewBinding null-cleanup
- `app/src/main/kotlin/com/example/capstone_login/ui/MainActivity.kt` - Single Activity with ActivityMainBinding and commented Phase 3 auth routing

## Decisions Made

- AuthRepository imports `FirebaseUser` and `FirebaseAuthException` as type references (return types and catch clauses) — this does not violate the layer boundary rule, which prohibits calling `FirebaseAuth.getInstance()` outside DataSource classes
- AuthUiState.Success holds `FirebaseUser` per plan specification — Fragment uses it only as a navigation trigger, never logs auth tokens
- All Firebase calls in DataSource stubs throw `NotImplementedError` with "Phase 2: implement" messages — placeholder pattern that makes Phase 2 integration points explicit

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

The following stubs are intentional and tracked for Phase 2 resolution:

| File | Stub | Phase to Resolve |
|------|------|-----------------|
| `FirebaseAuthDataSource.kt` | `signIn()` throws NotImplementedError | Phase 2 |
| `FirebaseAuthDataSource.kt` | `signUp()` throws NotImplementedError | Phase 2 |
| `FirestoreUserDataSource.kt` | `upsertUser()` throws NotImplementedError | Phase 2 |
| `LoginFragment.kt` | Button click handler commented out | Phase 2 |
| `LoginFragment.kt` | Navigation on Success commented out | Phase 2 |
| `MainActivity.kt` | Auth state check commented out | Phase 3 |

These stubs do not prevent plan completion — the skeleton's purpose is establishing correct layer boundaries and state contracts. Phase 2 implements the actual Firebase calls.

## Issues Encountered

None. All acceptance criteria passed on first verification.

## Architecture Invariants Confirmed

1. **Layer boundary:** Firebase imports only in `data/source/` files:
   - `FirebaseAuthDataSource.kt`: imports `com.google.firebase.auth.*`
   - `FirestoreUserDataSource.kt`: imports `com.google.firebase.firestore.*`
   - `AuthRepository.kt`: imports `FirebaseUser` and `FirebaseAuthException` as types (NOT `FirebaseAuth.getInstance()`)
   - `AuthUiState.kt`: imports `FirebaseUser` as Success state carrier (per plan design)
   - `AuthViewModel.kt`: zero Firebase imports
   - `LoginFragment.kt`: zero Firebase imports
   - `CalendarFragment.kt`: zero Firebase imports
   - `MainActivity.kt`: zero Firebase imports

2. **Lazy initialization confirmed:**
   - `FirebaseAuthDataSource`: `private val auth by lazy { FirebaseAuth.getInstance() }`
   - `FirestoreUserDataSource`: `private val db by lazy { FirebaseFirestore.getInstance() }`

3. **StateFlow encapsulation confirmed:**
   - `private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)` (private)
   - `val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()` (read-only public)

4. **ViewBinding null-cleanup confirmed:**
   - `LoginFragment.onDestroyView()`: `_binding = null`
   - `CalendarFragment.onDestroyView()`: `_binding = null`

## Threat Surface Scan

No new security-relevant surface beyond the plan's threat model. The threat model mitigations are implemented:
- T-02-01 (MutableStateFlow exposure): Private `_uiState`; read-only `StateFlow` exposed via `asStateFlow()` — applied
- T-02-04 (uncaught exception in viewModelScope): All repository calls wrapped in try/catch returning `Result.Error`; ViewModel maps to `AuthUiState.Error` — applied

## Next Phase Readiness

- Architecture skeleton is complete. Phase 2 (Auth Core) can directly replace stub `throw NotImplementedError` lines with actual Firebase calls
- All import chains are correct — Phase 2 only needs to uncomment the `.await()` patterns in DataSource files
- Blocker: `google-services.json` still required (plan 01-02 / 01-03 concern) before any Firebase call can succeed at runtime

---
*Phase: 01-project-foundation-firebase-setup*
*Completed: 2026-05-05*
