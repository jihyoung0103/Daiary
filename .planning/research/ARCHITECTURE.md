# Architecture Research

**Domain:** Android Kotlin Firebase Auth + Firestore login module
**Researched:** 2026-05-04
**Confidence:** HIGH (Android official docs + Firebase docs)

## Standard Architecture

### System Overview

```
┌─────────────────────────────────────────────────────────────┐
│                       UI LAYER                               │
│                                                              │
│  ┌──────────────────┐         ┌─────────────────────┐        │
│  │   LoginFragment  │         │  CalendarFragment   │        │
│  │  (email/pw form) │         │  (placeholder stub) │        │
│  └────────┬─────────┘         └──────────┬──────────┘        │
│           │ events up                    │ events up          │
│           ▼                              ▼                    │
│  ┌──────────────────────────────────────────────────────┐    │
│  │               AuthViewModel                           │    │
│  │  - uiState: StateFlow<AuthUiState>                   │    │
│  │  - login(email, pw)                                  │    │
│  │  - checkAuthState()                                  │    │
│  └──────────────────────┬───────────────────────────────┘    │
│                         │ state down                          │
│  ┌──────────────────────▼───────────────────────────────┐    │
│  │               NavController (MainActivity)            │    │
│  │  - Observes auth state → routes to login or calendar │    │
│  └──────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────┤
│                     DATA LAYER                               │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │                  AuthRepository                         │  │
│  │  - signIn(email, pw): Result<FirebaseUser>             │  │
│  │  - signOut()                                           │  │
│  │  - currentUser(): FirebaseUser?                        │  │
│  │  - authStateFlow(): Flow<FirebaseUser?>                │  │
│  └────────────┬───────────────────┬───────────────────────┘  │
│               │                   │                           │
│  ┌────────────▼──────┐  ┌────────▼──────────────────────┐   │
│  │ FirebaseAuth      │  │  FirestoreUserDataSource       │   │
│  │ (Data Source)     │  │  - upsertUser(uid, email)      │   │
│  │ - signInWith...   │  │  - set(merge=true)             │   │
│  │ - currentUser     │  └───────────────────────────────┘   │
│  │ - addAuthState    │                                        │
│  │   Listener        │                                        │
│  └───────────────────┘                                        │
├─────────────────────────────────────────────────────────────┤
│                  FIREBASE BACKEND (Cloud)                    │
│  ┌──────────────────────┐  ┌──────────────────────────┐     │
│  │  Firebase Auth       │  │  Firestore               │     │
│  │  (email/password)    │  │  users/{uid} document    │     │
│  └──────────────────────┘  └──────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility | Typical Implementation |
|-----------|----------------|------------------------|
| LoginFragment | Renders email/password form, forwards events to ViewModel | Fragment + ViewBinding |
| CalendarFragment | Placeholder main screen after login | Fragment (stub) |
| AuthViewModel | Holds AuthUiState, calls repository, drives navigation | ViewModel + StateFlow |
| NavController (MainActivity) | Observes auth state, routes login↔main, clears back stack | Single-Activity with nav graph |
| AuthRepository | Wraps Firebase Auth + Firestore calls, exposes clean API | Plain class, no Android deps |
| FirebaseAuth DataSource | Direct Firebase Auth SDK calls | FirebaseAuth singleton |
| FirestoreUserDataSource | Direct Firestore SDK calls for user upsert | FirebaseFirestore instance |

## Recommended Project Structure

```
app/src/main/kotlin/com/example/capstone/
├── ui/
│   ├── auth/
│   │   ├── LoginFragment.kt          # email/password form UI
│   │   └── AuthViewModel.kt          # auth state + login action
│   ├── calendar/
│   │   └── CalendarFragment.kt       # placeholder main screen
│   └── MainActivity.kt               # single activity, NavHostFragment
│
├── data/
│   ├── repository/
│   │   └── AuthRepository.kt         # combines Auth + Firestore logic
│   └── source/
│       ├── FirebaseAuthDataSource.kt  # wraps FirebaseAuth SDK
│       └── FirestoreUserDataSource.kt # wraps Firestore SDK
│
├── model/
│   └── User.kt                        # data class: uid, email, displayName
│
└── util/
    └── Result.kt                      # sealed class: Success, Error, Loading
```

```
app/src/main/res/
├── layout/
│   ├── activity_main.xml              # NavHostFragment container
│   ├── fragment_login.xml             # email/pw fields + login button
│   └── fragment_calendar.xml          # placeholder
└── navigation/
    └── nav_graph.xml                  # login + calendar destinations
```

### Structure Rationale

- **ui/auth/**: All login-screen concerns (Fragment + ViewModel) colocated. ViewModel never imported by other UI packages — prevents tight coupling.
- **data/repository/**: AuthRepository is the only class the ViewModel talks to. Keeps ViewModel free of Firebase SDK imports.
- **data/source/**: One class per external service. Makes each replaceable or mockable independently.
- **model/**: Plain Kotlin data classes with no Android or Firebase dependencies — safe to use in tests.
- **navigation/nav_graph.xml**: Single nav graph with both destinations. Start destination determined at runtime by AuthViewModel auth state check.

## Architectural Patterns

### Pattern 1: Repository Wraps Firebase Callbacks as Suspending Functions

**What:** Firebase SDK uses Task-based callbacks. The repository converts them to `suspend` functions using `kotlinx.coroutines.tasks.await()`, which makes the ViewModel code sequential and readable.

**When to use:** Always — wrap every Firebase call at the data source boundary, never in ViewModel.

**Trade-offs:** Pro — ViewModel code reads like normal sequential logic. Con — exceptions from Firebase must be caught and mapped.

**Example:**
```kotlin
// FirebaseAuthDataSource.kt
class FirebaseAuthDataSource {
    private val auth = FirebaseAuth.getInstance()

    suspend fun signIn(email: String, password: String): FirebaseUser {
        // .await() from kotlinx-coroutines-play-services
        return auth.signInWithEmailAndPassword(email, password).await().user
            ?: throw IllegalStateException("Auth succeeded but user is null")
    }

    fun currentUser(): FirebaseUser? = auth.currentUser
}
```

```kotlin
// AuthRepository.kt
class AuthRepository(
    private val authSource: FirebaseAuthDataSource,
    private val userSource: FirestoreUserDataSource
) {
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val user = authSource.signIn(email, password)
            userSource.upsertUser(user.uid, user.email ?: "")
            Result.Success(user)
        } catch (e: FirebaseAuthException) {
            Result.Error(e)
        }
    }

    fun currentUser(): FirebaseUser? = authSource.currentUser()
}
```

### Pattern 2: Firestore Upsert with SetOptions.merge()

**What:** On every successful login, write user data to `users/{uid}` using `set()` with `SetOptions.merge()`. This creates the document on first login and updates it on subsequent logins without overwriting fields not in the payload.

**When to use:** Always on login success, after Firebase Auth completes.

**Trade-offs:** Pro — idempotent, no "user not found" vs "user exists" branching logic needed. Con — partial updates only; fields not included are preserved (desired behavior here).

**Example:**
```kotlin
// FirestoreUserDataSource.kt
class FirestoreUserDataSource {
    private val db = FirebaseFirestore.getInstance()

    suspend fun upsertUser(uid: String, email: String) {
        val data = mapOf(
            "email" to email,
            "lastLoginAt" to FieldValue.serverTimestamp()
        )
        db.collection("users")
            .document(uid)
            .set(data, SetOptions.merge())
            .await()
    }
}
```

### Pattern 3: StateFlow-Based UI State in AuthViewModel

**What:** ViewModel exposes a single `StateFlow<AuthUiState>` data class. Fragment collects it inside `repeatOnLifecycle(STARTED)` and reacts to each state variant. Navigation is triggered from the fragment when it observes a `Success` state.

**When to use:** Standard pattern for all screen state. Covers loading, error, and success without multiple LiveData fields.

**Trade-offs:** Pro — single observable, no state synchronization bugs. Con — Fragment must handle `repeatOnLifecycle` boilerplate (one-time setup).

**Example:**
```kotlin
// AuthUiState.kt
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: FirebaseUser) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

// AuthViewModel.kt
class AuthViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            _uiState.value = when (val result = repo.signIn(email, password)) {
                is Result.Success -> AuthUiState.Success(result.data)
                is Result.Error   -> AuthUiState.Error(result.exception.message ?: "Login failed")
            }
        }
    }
}
```

```kotlin
// LoginFragment.kt — consuming state
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
            when (state) {
                is AuthUiState.Loading -> showProgress(true)
                is AuthUiState.Error   -> { showProgress(false); showError(state.message) }
                is AuthUiState.Success -> {
                    showProgress(false)
                    findNavController().navigate(
                        R.id.action_loginFragment_to_calendarFragment,
                        null,
                        NavOptions.Builder()
                            .setPopUpTo(R.id.loginFragment, true)  // removes login from back stack
                            .build()
                    )
                }
                else -> showProgress(false)
            }
        }
    }
}
```

### Pattern 4: Auto-Login via currentUser Check at App Start

**What:** Firebase Auth persists the session token on device. `FirebaseAuth.getInstance().currentUser` is non-null after a previous login, even after the app restarts. MainActivity checks this before inflating the nav graph's start destination and navigates directly to the calendar if a user exists.

**When to use:** In `MainActivity.onCreate()`, before `setContentView()` or as the first action after nav graph inflation.

**Trade-offs:** Pro — no round-trip to Firebase servers needed; purely local token check. Con — token may be expired; Firebase auto-refreshes it silently on next SDK call (no action required by developer).

**Example:**
```kotlin
// MainActivity.kt
class MainActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navController = findNavController(R.id.nav_host_fragment)

        // currentUser is a local cache check — no network call
        if (FirebaseAuth.getInstance().currentUser != null) {
            navController.navigate(
                R.id.calendarFragment,
                null,
                NavOptions.Builder()
                    .setPopUpTo(R.id.loginFragment, true)
                    .build()
            )
        }
        // If currentUser == null, nav graph starts at loginFragment (default start destination)
    }
}
```

**Alternative — AuthStateListener approach:**
`AuthStateListener` fires asynchronously whenever auth state changes (login, logout, token refresh). For this project's scope, the synchronous `currentUser` check in `onCreate` is simpler and sufficient. `AuthStateListener` is more appropriate when you need to react to mid-session sign-outs or token revocations.

## Data Flow

### Login Request Flow

```
User taps "Login" button
    ↓
LoginFragment.loginButton.setOnClickListener
    ↓
AuthViewModel.login(email, password)
    ↓  (viewModelScope.launch)
AuthRepository.signIn(email, password)
    ↓  (suspend)
FirebaseAuthDataSource.signIn()
    ↓  (.await() on Task)
Firebase Auth Service  ←→  Firebase Cloud
    ↓  returns FirebaseUser
FirestoreUserDataSource.upsertUser(uid, email)
    ↓  (.await() on Task)
Firestore Service  ←→  Firebase Cloud (users/{uid} set/merge)
    ↓  returns
AuthRepository  →  Result.Success(user)
    ↓
AuthViewModel  →  _uiState.value = AuthUiState.Success
    ↓
LoginFragment  ←  StateFlow emit (repeatOnLifecycle collects)
    ↓
NavController.navigate(calendarFragment, popUpTo=loginFragment)
    ↓
CalendarFragment displayed, login screen removed from back stack
```

### Auto-Login Flow (App Restart)

```
App launches → MainActivity.onCreate()
    ↓
FirebaseAuth.getInstance().currentUser != null?
    YES → NavController.navigate(calendarFragment, popUpTo loginFragment)
    NO  → Nav graph default start = loginFragment → show login UI
```

### State Management

```
AuthUiState (StateFlow in AuthViewModel)
    ↓ (asStateFlow — read-only exposure)
LoginFragment  ←  repeatOnLifecycle(STARTED).collect { state → ... }

State transitions:
  Idle → Loading (on login() called)
  Loading → Success (on repo returns Success)
  Loading → Error (on repo returns Error)
  Error → Loading (on retry)
```

### Key Data Flows

1. **Login + Firestore upsert:** Auth sign-in and Firestore write are sequential in the same coroutine. If Firestore write fails, treat as non-fatal (user is authenticated; log the error, navigate anyway). If Auth fails, emit Error state and stay on login screen.
2. **Error propagation:** Repository catches `FirebaseAuthException` and maps to `Result.Error`. ViewModel maps `Result.Error` to `AuthUiState.Error(message)`. Fragment shows the message string — no raw Firebase exceptions reach the UI layer.

## Scaling Considerations

| Scale | Architecture Adjustments |
|-------|--------------------------|
| Capstone / 1-10 users | Current architecture is complete. No caching layer needed. |
| 100-1k users | No architecture change. Firebase scales transparently. Consider adding Firestore Security Rules enforcement. |
| 10k+ users | Extract Firestore writes to a Cloud Function triggered on Auth user creation — removes client-side Firestore write from critical login path. |

### Scaling Priorities

1. **First bottleneck:** Firestore write on every login adds latency. At scale, move upsert to a Firebase Auth `onCreate` Cloud Trigger — completely removes it from the mobile login flow.
2. **Second bottleneck:** If offline-first becomes required, add a local Room database as a cache layer behind the repository. The current architecture supports this without changing the ViewModel.

## Anti-Patterns

### Anti-Pattern 1: Firebase SDK calls directly in Fragment or Activity

**What people do:** Call `FirebaseAuth.getInstance().signInWithEmailAndPassword(...)` directly inside `onClick` in a Fragment.

**Why it's wrong:** Fragment lifecycle kills the callback mid-flight on rotation. Error handling is scattered. Cannot be unit tested. Violates separation of concerns.

**Do this instead:** All Firebase calls go through ViewModel → Repository → DataSource. Fragment only sends events and observes state.

### Anti-Pattern 2: Not clearing LoginFragment from back stack after login

**What people do:** Navigate to CalendarFragment without `popUpTo` — user presses Back from calendar and lands back on the login screen.

**Why it's wrong:** After a successful login, the user should never be able to navigate back to the login screen using the system back button.

**Do this instead:** Always use `NavOptions.Builder().setPopUpTo(R.id.loginFragment, inclusive = true)` when navigating to the calendar on success.

### Anti-Pattern 3: Exposing MutableStateFlow to the Fragment

**What people do:** Declare `val uiState = MutableStateFlow(...)` as `public` in ViewModel.

**Why it's wrong:** Fragment (or any external class) can write state directly, bypassing ViewModel logic. Breaks single source of truth.

**Do this instead:** Private `_uiState: MutableStateFlow`, exposed as read-only `val uiState: StateFlow = _uiState.asStateFlow()`.

### Anti-Pattern 4: Blocking the main thread with Firebase Tasks

**What people do:** Call `.result` on a Firebase Task synchronously, or use `runBlocking` in a Fragment.

**Why it's wrong:** Blocks the UI thread, causes ANRs (Application Not Responding) errors.

**Do this instead:** Use `.await()` (from `kotlinx-coroutines-play-services`) inside a `viewModelScope.launch` coroutine. Always main-safe.

### Anti-Pattern 5: Treating Firestore upsert failure as fatal

**What people do:** If Firestore write fails, show an error and block navigation to the calendar.

**Why it's wrong:** The user is authenticated. Blocking them from the app because of a secondary write failure creates unnecessary friction. Firestore has built-in offline persistence and will retry.

**Do this instead:** Log the Firestore error, but still navigate to the calendar. The upsert will succeed on next login attempt. Firebase offline persistence can also be enabled to auto-retry.

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| Firebase Auth | `FirebaseAuthDataSource` wraps `FirebaseAuth` singleton; all calls converted to `suspend` via `.await()` | Token persistence is automatic; `currentUser` is safe to read synchronously on main thread |
| Firestore | `FirestoreUserDataSource` wraps `FirebaseFirestore`; `set(..., SetOptions.merge())` for upsert | Enable offline persistence with `FirebaseFirestoreSettings` for retry resilience |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| Fragment ↔ ViewModel | Fragment observes `StateFlow`; sends events via `fun login()` | Fragment never reads from ViewModel's private state or triggers navigation except via state observation |
| ViewModel ↔ Repository | ViewModel calls `suspend fun signIn()` inside `viewModelScope.launch` | ViewModel holds no Firebase types — only domain types (`User`, `Result`) |
| Repository ↔ DataSources | Repository calls `suspend` methods on DataSource classes | DataSources are the only classes that import `com.google.firebase.*` |
| NavController routing | MainActivity or LoginFragment drives navigation; CalendarFragment is a passive destination | Use `popUpTo` on every navigation away from login to prevent back-stack buildup |

## Component Build Order

Build in this order to avoid import-before-exists errors and to validate each layer before building on top of it:

1. **model/User.kt + util/Result.kt** — No dependencies; needed by everything above.
2. **data/source/FirebaseAuthDataSource.kt** — Thin wrapper; can be manually tested by calling `signIn()` from a debug button.
3. **data/source/FirestoreUserDataSource.kt** — Independent of Auth; test by writing a hardcoded document.
4. **data/repository/AuthRepository.kt** — Combines both sources; test the sequential auth→upsert flow here.
5. **ui/auth/AuthViewModel.kt** — Wire to repository; verify StateFlow emissions in isolation.
6. **res/navigation/nav_graph.xml** — Define loginFragment (start) and calendarFragment destinations + action.
7. **ui/auth/LoginFragment.kt** — Wire form → ViewModel → observe state → navigate.
8. **ui/MainActivity.kt** — Auto-login currentUser check + NavHostFragment setup.
9. **ui/calendar/CalendarFragment.kt** — Placeholder last; just needs to exist as a navigation destination.

## Sources

- Android App Architecture — Official Guide: https://developer.android.com/topic/architecture
- Android UI Layer — StateFlow, UiState: https://developer.android.com/topic/architecture/ui-layer
- Android Data Layer — Repository Pattern: https://developer.android.com/topic/architecture/data-layer
- Conditional Navigation (login flow + popUpTo): https://developer.android.com/guide/navigation/use-graph/conditional
- Navigation Principles (back stack, start destination): https://developer.android.com/guide/navigation/principles
- Firebase Auth — email/password (Android): https://firebase.google.com/docs/auth/android/password-auth
- Firestore — set with merge (upsert): https://firebase.google.com/docs/firestore/manage-data/add-data

---
*Architecture research for: Android Kotlin Firebase Auth + Firestore login module*
*Researched: 2026-05-04*
