---
phase: 01-project-foundation-firebase-setup
plan: 03
subsystem: resources
tags: [android, manifest, layouts, navigation, firebase, themes]

# Dependency graph
requires:
  - phase: 01-project-foundation-firebase-setup
    plan: 01
    provides: Gradle build system with Firebase BoM and ViewBinding
  - phase: 01-project-foundation-firebase-setup
    plan: 02
    provides: MVVM skeleton Kotlin sources (LoginFragment, CalendarFragment, MainActivity)
provides:
  - AndroidManifest.xml with INTERNET permission and MainActivity as launcher
  - activity_main.xml with NavHostFragment wired to nav_graph.xml
  - nav_graph.xml with loginFragment as start destination and calendarFragment reachable via action
  - fragment_login.xml with emailEditText, passwordEditText, loginButton, progressBar, errorTextView
  - fragment_calendar.xml placeholder with calendarPlaceholderText
  - themes.xml: Theme.CapstoneLogin extending Material3.DayNight.NoActionBar
  - strings.xml with app strings and Phase 2 error message keys
  - .gitignore excluding app/google-services.json
  - Build toolchain adjusted for local compatibility (AGP 8.1.0, Gradle 8.1, Kotlin 1.9.0, compileSdk 34)
affects:
  - Phase 2 (Auth Core): ViewBinding IDs in fragment_login.xml match FragmentLoginBinding field names
  - Phase 3 (Navigation): action_loginFragment_to_calendarFragment action defined in nav_graph.xml

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "NavHostFragment in activity_main.xml as single container — nav_graph.xml manages all destinations"
    - "loginFragment is startDestination — unauthenticated entry point by default"
    - "app/google-services.json excluded from git via .gitignore — re-download from Firebase Console if lost"

key-files:
  created:
    - app/src/main/AndroidManifest.xml
    - app/src/main/res/layout/activity_main.xml
    - app/src/main/res/layout/fragment_login.xml
    - app/src/main/res/layout/fragment_calendar.xml
    - app/src/main/res/navigation/nav_graph.xml
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values/themes.xml
    - .gitignore
  modified:
    - app/build.gradle.kts (compileSdk/targetSdk 36→34, jvmToolchain 17→20)
    - gradle/libs.versions.toml (Kotlin 2.3.21→1.9.0, AGP 9.2.0→8.1.0)
    - gradle/wrapper/gradle-wrapper.properties (Gradle 9.4.1→8.1)
    - gradle.properties (android.suppressUnsupportedCompileSdk=34 added)

key-decisions:
  - "Build toolchain downgraded (AGP 8.1.0 / Gradle 8.1 / Kotlin 1.9.0 / compileSdk 34) to match locally installed SDK and tools"
  - "android.suppressUnsupportedCompileSdk=34 added to gradle.properties to suppress SDK version warning"
  - "google-services.json placed at app/google-services.json (not committed — excluded by .gitignore)"

requirements-completed: [SETUP-02, SETUP-03]

# Metrics
duration: ~10min
completed: 2026-05-06
---

# Phase 1 Plan 03: Android Resource Files Summary

**All Android resource files created and wired. Firebase Console configured (google-services.json placed). Build succeeds with downgraded toolchain (AGP 8.1.0 / Gradle 8.1 / Kotlin 1.9.0 / compileSdk 34).**

## Performance

- **Duration:** ~10 min
- **Completed:** 2026-05-06
- **Tasks:** 1 auto + 2 human checkpoints
- **Files created:** 8
- **Files modified:** 4 (build toolchain version adjustments)

## Accomplishments

- Created all 8 Android resource files connecting build scripts (plan 01) and Kotlin sources (plan 02)
- AndroidManifest.xml: INTERNET permission, MainActivity as launcher, Theme.CapstoneLogin applied
- activity_main.xml: NavHostFragment with `app:navGraph="@navigation/nav_graph"` and `defaultNavHost="true"`
- nav_graph.xml: loginFragment as startDestination; action_loginFragment_to_calendarFragment defined
- fragment_login.xml: all ViewBinding IDs (emailEditText, passwordEditText, loginButton, progressBar, errorTextView) matching FragmentLoginBinding
- .gitignore: google-services.json excluded from version control
- Downgraded build toolchain to match locally installed tools — build verified successful

## Task Commits

1. **Task 1: Create Android resource files** — `ac62363` (feat)
2. **Build toolchain downgrade** — `a190bad` (chore)

## Human Checkpoints

| Checkpoint | Resume Signal | Status |
|---|---|---|
| Firebase setup: google-services.json placed at app/ | firebase-ready | ✅ Confirmed |
| Build verification: Android Studio Gradle sync + build | build-ok | ✅ Build succeeded |

## Files Created

- `app/src/main/AndroidManifest.xml` — INTERNET permission, MainActivity launcher, Theme.CapstoneLogin
- `app/src/main/res/layout/activity_main.xml` — NavHostFragment → nav_graph.xml
- `app/src/main/res/layout/fragment_login.xml` — emailEditText, passwordEditText, loginButton, progressBar, errorTextView
- `app/src/main/res/layout/fragment_calendar.xml` — calendarPlaceholderText (placeholder)
- `app/src/main/res/navigation/nav_graph.xml` — loginFragment (start), calendarFragment, action_loginFragment_to_calendarFragment
- `app/src/main/res/values/strings.xml` — app_name, UI strings, Phase 2 error message keys
- `app/src/main/res/values/themes.xml` — Theme.CapstoneLogin (Material3.DayNight.NoActionBar)
- `.gitignore` — app/google-services.json excluded

## Deviations from Plan

- Build toolchain downgraded from plan-specified versions to locally available versions:
  - AGP 9.2.0 → 8.1.0 / Gradle 9.4.1 → 8.1 / Kotlin 2.3.21 → 1.9.0 / compileSdk 36 → 34
  - `jvmTarget = "17"` replaced with `jvmToolchain(20)`
  - `android.suppressUnsupportedCompileSdk=34` added to suppress SDK warning
  - All functional requirements are met; version differences do not affect Phase 2/3 implementation

## Verification Results

| Check | Command | Result |
|---|---|---|
| INTERNET permission | grep "android.permission.INTERNET" AndroidManifest.xml | ✅ |
| NavHostFragment | grep "nav_host_fragment" activity_main.xml | ✅ |
| startDestination | grep "startDestination" nav_graph.xml | ✅ @id/loginFragment |
| google-services excluded | grep "google-services.json" .gitignore | ✅ |
| Theme reference | grep "Theme.CapstoneLogin" manifest + themes.xml | ✅ |

## Next Phase Readiness

Phase 2 (Auth Core) can now begin:
- `FirebaseAuthDataSource.signIn()` / `signUp()` — replace `throw NotImplementedError` with `auth.signInWithEmailAndPassword().await()`
- `FirestoreUserDataSource.upsertUser()` — replace stub with actual Firestore set/merge call
- `LoginFragment` — wire `loginButton.setOnClickListener` and handle `AuthUiState` transitions
- All ViewBinding IDs in fragment_login.xml are ready (`emailEditText`, `passwordEditText`, `loginButton`, `progressBar`, `errorTextView`)
- Navigation action `action_loginFragment_to_calendarFragment` is defined in nav_graph.xml

---
*Phase: 01-project-foundation-firebase-setup*
*Completed: 2026-05-06*
