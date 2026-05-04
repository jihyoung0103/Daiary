# Stack Research

**Domain:** Android Kotlin app — Firebase Auth (email/password) + Firestore + Navigation
**Researched:** 2026-05-04
**Confidence:** HIGH (AGP/Gradle/AndroidX versions verified from official Android docs; Kotlin version verified from kotlinlang.org; Firebase BoM version from training data cross-referenced with release cadence; Lifecycle/Navigation/AppCompat/Core-KTX from official AndroidX release pages)

---

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Kotlin | 2.3.21 | Primary language | Latest stable as of April 2026; K2 compiler default since 2.0, faster builds, better error messages. All new Android projects use Kotlin. |
| Android Gradle Plugin (AGP) | 9.2.0 | Android build system | Latest stable (April 2026); required for API 36 compileSdk, new unified test reports. Pair with Gradle 9.4.1. |
| Gradle | 9.4.1 | Build tool | Required minimum for AGP 9.2.0. Use `gradle-wrapper.properties` to pin this. |
| compileSdk / targetSdk | 36 | API level targeting | Android 16 (API 36) is current; required to publish to Play Store in 2025+. |
| minSdk | 26 | Minimum device support | API 26 (Android 8.0) covers ~95% of active devices as of 2026; lets you use modern APIs without compatibility shims. |
| Build script format | Kotlin DSL (`build.gradle.kts`) | Build configuration | Default in all Android Studio projects since Giraffe; type-safe, IDE completion, compile-time checks. Do not use Groovy for new projects. |
| Version catalogs (`libs.versions.toml`) | Gradle built-in | Dependency management | Default in new Android Studio projects; single source of truth for all versions; type-safe accessor in build scripts. |

### Firebase Libraries

| Library | Version | Purpose | Why Recommended |
|---------|---------|---------|-----------------|
| Firebase Android BoM | 33.7.0 | Bill of Materials — version alignment | Use BoM so you never specify individual Firebase lib versions; they stay in sync. This is Firebase's official recommendation. Verify latest at `firebase.google.com/support/release-notes/android`. |
| firebase-auth-ktx | (via BoM) | Email/password authentication | Kotlin-idiomatic extensions on top of `firebase-auth`. Use `auth.awaitSignIn()` via coroutines-play-services. |
| firebase-firestore-ktx | (via BoM) | Firestore document read/write | `-ktx` variant provides Kotlin-native extension functions and `Flow` adapters. Required for upsert on `users/{uid}`. |

> **Note on BoM version:** Firebase BoM 33.7.0 was released in late 2024 and 33.x continued quarterly into early 2025. As of May 2026, the BoM is likely 34.x or higher. When you create the Android Studio project, let Android Studio's Firebase Assistant generate the initial BoM version, or check `firebase.google.com/support/release-notes/android` and use the highest stable `33.x` or `34.x` entry. Never pin individual firebase-auth or firebase-firestore versions — always use the BoM and omit the version number on individual artifacts.

### AndroidX Core Libraries

| Library | Version | Purpose | Why Recommended |
|---------|---------|---------|-----------------|
| androidx.core:core-ktx | 1.18.0 | Kotlin extensions for Android framework APIs | Essential for idiomatic Kotlin; provides extension functions on Context, View, etc. Latest stable March 2026. |
| androidx.appcompat:appcompat | 1.7.1 | Backward-compatible Activity/Fragment base | `AppCompatActivity` base class; required for Material Components theming and ActionBar support. |
| androidx.activity:activity-ktx | 1.13.0 | Kotlin extensions for Activity | Provides `viewModels()` delegate, `registerForActivityResult` KTX, `OnBackPressedDispatcher`. Latest stable March 2026. |
| androidx.fragment:fragment-ktx | 1.8.9 | Kotlin extensions for Fragment | Provides `viewModels()`, `activityViewModels()` property delegates. Pairs with Navigation Component. |
| androidx.constraintlayout:constraintlayout | 2.2.1 | Complex UI layouts | Use for login screen layout — handles responsive positioning without nested LinearLayouts. |
| com.google.android.material:material | 1.12.0 | Material Design 3 components | `TextInputLayout`, `TextInputEditText`, `MaterialButton` — required for professional login form UX. Version 1.12.x is current stable. |

### Architecture Libraries (Jetpack)

| Library | Version | Purpose | Why Recommended |
|---------|---------|---------|-----------------|
| androidx.lifecycle:lifecycle-viewmodel-ktx | 2.10.0 | ViewModel with `viewModelScope` | `viewModelScope` is the correct scope for all Firebase async calls; auto-cancelled when screen is gone. Latest stable April 2026. |
| androidx.lifecycle:lifecycle-runtime-ktx | 2.10.0 | `lifecycleScope`, `repeatOnLifecycle` | Use `lifecycleScope.launch` in Activity/Fragment; `repeatOnLifecycle(STARTED)` for StateFlow collection. |
| androidx.lifecycle:lifecycle-livedata-ktx | 2.10.0 | LiveData (kept for reference) | Include only if you choose LiveData over StateFlow; see "What NOT to Use" section for guidance. |
| androidx.navigation:navigation-fragment-ktx | 2.9.8 | Fragment-based Navigation Component | Graph-driven navigation between Login, Register, and Calendar fragments. Latest stable April 2026. |
| androidx.navigation:navigation-ui-ktx | 2.9.8 | Navigation + ActionBar/BottomNav integration | Connects NavController to toolbar back button automatically. |

### Coroutines

| Library | Version | Purpose | Why Recommended |
|---------|---------|---------|-----------------|
| org.jetbrains.kotlinx:kotlinx-coroutines-android | 1.9.0 | Coroutines on Android (Main dispatcher) | Bridge between Firebase Tasks and Kotlin suspend functions; `Dispatchers.Main` for UI updates. |
| org.jetbrains.kotlinx:kotlinx-coroutines-play-services | 1.9.0 | `Task.await()` extension | Converts Firebase `Task<T>` to suspend functions. This is the key bridge: `auth.signInWithEmailAndPassword(email, pw).await()`. Without this, you must use callbacks. |

> **Version note on coroutines:** 1.9.0 was released in late 2024. As of May 2026 there may be a 1.10.x release. Check `github.com/Kotlin/kotlinx.coroutines/releases` before pinning. Coroutines version must be compatible with Kotlin version — use the coroutines release that matches your Kotlin 2.3.x series.

### Google Services Plugin

| Plugin | Version | Purpose | Notes |
|--------|---------|---------|-------|
| com.google.gms:google-services | 4.4.2 | Processes `google-services.json` into app resources | Applied in `app/build.gradle.kts` as a plugin. Without this, Firebase cannot initialize. |

---

## Build Script Setup (build.gradle.kts)

### `settings.gradle.kts` (project-level)

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
```

### `libs.versions.toml` (version catalog — in `gradle/`)

```toml
[versions]
kotlin = "2.3.21"
agp = "9.2.0"
coreKtx = "1.18.0"
appcompat = "1.7.1"
activityKtx = "1.13.0"
fragmentKtx = "1.8.9"
lifecycle = "2.10.0"
navigation = "2.9.8"
constraintlayout = "2.2.1"
material = "1.12.0"
firebaseBom = "33.7.0"
coroutines = "1.9.0"
googleServices = "4.4.2"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
androidx-activity-ktx = { group = "androidx.activity", name = "activity-ktx", version.ref = "activityKtx" }
androidx-fragment-ktx = { group = "androidx.fragment", name = "fragment-ktx", version.ref = "fragmentKtx" }
androidx-lifecycle-viewmodel-ktx = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-ktx", version.ref = "lifecycle" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-navigation-fragment-ktx = { group = "androidx.navigation", name = "navigation-fragment-ktx", version.ref = "navigation" }
androidx-navigation-ui-ktx = { group = "androidx.navigation", name = "navigation-ui-ktx", version.ref = "navigation" }
androidx-constraintlayout = { group = "androidx.constraintlayout", name = "constraintlayout", version.ref = "constraintlayout" }
material = { group = "com.google.android.material", name = "material", version.ref = "material" }
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-auth-ktx = { group = "com.google.firebase", name = "firebase-auth-ktx" }
firebase-firestore-ktx = { group = "com.google.firebase", name = "firebase-firestore-ktx" }
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
coroutines-play-services = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-play-services", version.ref = "coroutines" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
```

### `build.gradle.kts` (project-level / root)

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.services) apply false
}
```

### `app/build.gradle.kts` (module-level)

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.capstone_login"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.capstone_login"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true   // Use ViewBinding — replaces findViewById, no Compose needed
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // Lifecycle + ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Navigation Component
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Firebase (BoM manages all Firebase versions)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)

    // Coroutines (for Task.await() + Firebase integration)
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.play.services)
}
```

---

## Firebase Project Setup Steps

1. **Create Firebase project** at `console.firebase.google.com`.
   - Add Android app with your package name (e.g., `com.example.capstone_login`).
   - Download `google-services.json` and place it in `app/` (not project root).

2. **Enable Email/Password authentication.**
   - Firebase Console → Authentication → Sign-in method → Email/Password → Enable.

3. **Create Firestore database.**
   - Firebase Console → Firestore Database → Create database.
   - Start in **test mode** for development (allows all reads/writes without rules).
   - Region: `asia-northeast3` (Seoul) for lowest latency from Korea.

4. **Firestore Security Rules (development only).**
   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /{document=**} {
         allow read, write: if true;   // ONLY for development
       }
     }
   }
   ```
   Lock down before any production use: restrict `users/{uid}` to `request.auth.uid == uid`.

5. **Verify `google-services.json`** is excluded from version control if the repo is public.
   Add to `.gitignore`: `app/google-services.json`.

---

## State Management: StateFlow, not LiveData

Use `StateFlow<UiState>` in ViewModel exposed to the Fragment via `collectWithLifecycle`.

**Use StateFlow because:**
- Google's official architecture guidance (2025-2026) shows StateFlow as the primary state holder in all new examples.
- StateFlow is coroutine-native — no `Observer` boilerplate.
- Works correctly with `repeatOnLifecycle(STARTED)` for lifecycle-safe collection.
- LiveData remains supported but is no longer the recommended pattern for new code.

**Correct pattern:**

```kotlin
// ViewModel
class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(email, password)
                    .await()                          // requires kotlinx-coroutines-play-services
                // Firestore upsert here
                _uiState.value = LoginUiState.Success
            } catch (e: FirebaseAuthException) {
                _uiState.value = LoginUiState.Error(e.errorCode)
            }
        }
    }
}

// Fragment
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
            when (state) {
                is LoginUiState.Loading -> showProgress()
                is LoginUiState.Success -> navigateToCalendar()
                is LoginUiState.Error   -> showError(state.code)
                is LoginUiState.Idle    -> Unit
            }
        }
    }
}
```

---

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| View System (XML layouts + ViewBinding) | Jetpack Compose | Compose is the future, but this capstone has a fixed scope. View system is faster to set up for a login-only module and produces no K2/Compose compatibility surprises. Use Compose if the calendar screen is also being built from scratch in this project. |
| Navigation Component (Fragment nav graph) | Activity-per-screen | Activity-per-screen works, but Navigation Component handles the back stack automatically, is the current Google recommendation, and lets Login + Register share a single `NavHostFragment` with clean transitions. |
| StateFlow | LiveData | Use LiveData only if you are integrating with an existing codebase already using LiveData, or if teammates are unfamiliar with Flows. For all new code, StateFlow is the recommendation. |
| Firebase BoM | Individual Firebase version pins | Never pin individual Firebase library versions — they have internal dependencies. The BoM guarantees compatibility between firebase-auth, firebase-firestore, firebase-common, and the Google Play services libraries. |
| `kotlinx-coroutines-play-services` (`Task.await()`) | Callback-style `addOnSuccessListener` | Callbacks work, but make error handling messy (two paths: `addOnSuccessListener` + `addOnFailureListener`), complicate ViewModel state updates, and cannot be cancelled by `viewModelScope`. Use `await()` in all new code. |
| No Hilt (manual ViewModel factory) | Hilt DI | For a single-module capstone with 2-3 ViewModels, Hilt adds setup overhead (annotating Application class, all Activities/Fragments, writing `@Module` classes). The `by viewModels()` delegate works without Hilt. Add Hilt only if the project grows beyond this module or if the grader specifically evaluates DI. |
| minSdk 26 | minSdk 21 | minSdk 21 targets ~99% of devices but requires `appcompat` wrappers for many APIs that are built-in from API 26. API 26 is the practical minimum for modern development without compatibility pain. |

---

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| `build.gradle` (Groovy DSL) | No IDE type checking, no autocompletion for dependency names, will be removed from new Android Studio projects in future versions | `build.gradle.kts` (Kotlin DSL) — default since Android Studio Giraffe |
| `LiveData` for new ViewModels | Not deprecated, but Google's own architecture samples no longer use it for primary state. It runs on the main thread by default, does not support backpressure, and requires `Observer` boilerplate | `StateFlow<UiState>` with `collectWithLifecycle` |
| `AsyncTask` | Removed from Android API 30; throws `NoSuchMethodError` on modern devices | `viewModelScope.launch { }` with coroutines |
| `findViewByID` directly | Null-unsafe, verbose, no compile-time check that the ID exists | `ViewBinding` — enable via `buildFeatures { viewBinding = true }` |
| Firebase `addOnSuccessListener` / `addOnFailureListener` callbacks | Callback hell; cannot cancel on ViewModel scope cleared; error handling split across two lambda paths | `task.await()` from `kotlinx-coroutines-play-services` in a `viewModelScope.launch` block |
| `startActivity` + `finish()` for all navigation | Loses back stack management, no animated transitions, breaks predictive back gesture | Navigation Component with a `NavGraph`; use `navigate(actionId)` and `NavController` |
| Calling Firebase Auth from Activity/Fragment directly | Bypasses ViewModel lifecycle; async result can arrive after `onDestroy`, causing crashes or memory leaks | All Firebase calls go in ViewModel; Fragment/Activity only observes `StateFlow` |
| `Kotlin 1.x` (e.g., keeping the existing `.iml` Kotlin 1.9 config) | The current project has a bare stub using Kotlin 1.9 in an IDEA module; this is NOT an Android project. This entire project must be recreated as an Android Gradle project — the existing stub cannot be converted | Create a new Android project in Android Studio from scratch; do not try to add Android build configuration to the existing IntelliJ module |
| JVM target 1.8 (`jvmTarget = "1.8"`) | AGP 9.x requires JVM target 17 minimum; setting 1.8 generates a deprecation warning and will be an error in AGP 10 | `jvmTarget = "17"` in `kotlinOptions` |
| `firebase-firestore` (without `-ktx`) | The non-ktx variant requires Java-style `get()` calls; the `-ktx` variant adds Kotlin extensions and coroutine helpers | `firebase-firestore-ktx` |

---

## Version Compatibility Matrix

| Component | Version | Compatible With | Notes |
|-----------|---------|-----------------|-------|
| AGP 9.2.0 | — | Gradle 9.4.1+, JDK 17, compileSdk 36 | Requires JDK 17 in `compileOptions` / `kotlinOptions` |
| Kotlin 2.3.21 | — | AGP 9.2.0, coroutines 1.9.0 | K2 compiler is default; no Kapt — use KSP if you add annotation processors |
| lifecycle 2.10.0 | — | minSdk 23+ (changed from 21 in this release) | If you use minSdk 21-22, drop to lifecycle 2.8.x |
| Navigation 2.9.8 | — | fragment-ktx 1.8.9, activity-ktx 1.13.0 | Requires `android.useAndroidX=true` in `gradle.properties` |
| Firebase BoM 33.7.0 | — | `kotlinx-coroutines-play-services` 1.9.0 | BoM manages firebase-auth and firebase-firestore versions internally |
| `kotlinx-coroutines-play-services` 1.9.0 | — | Kotlin 2.3.x, coroutines-android 1.9.0 | Must use same version as `kotlinx-coroutines-android` |

---

## Key Architecture Decision: No Hilt for This Capstone

For this specific project (2-3 screens, 1-2 ViewModels, no repository layer complexity), manual ViewModel creation via the `by viewModels()` delegate is sufficient. Hilt is excellent but adds ~15-20 lines of boilerplate setup and requires annotating every Activity/Fragment. Skip it unless the grader explicitly evaluates DI or the project grows to multiple modules.

**Pattern without Hilt:**
```kotlin
// In Fragment
private val viewModel: LoginViewModel by viewModels()
```
This uses the default `ViewModelProvider.Factory` which calls the no-arg constructor. Firebase singleton instances (`FirebaseAuth.getInstance()`, `FirebaseFirestore.getInstance()`) are accessed inside the ViewModel directly — no injection needed.

---

## Sources

- AndroidX Lifecycle releases page (`developer.android.com/jetpack/androidx/releases/lifecycle`) — Lifecycle 2.10.0 confirmed, April 22 2026 — HIGH confidence
- AndroidX Navigation releases page (`developer.android.com/jetpack/androidx/releases/navigation`) — Navigation 2.9.8 confirmed, April 22 2026 — HIGH confidence
- AndroidX Activity releases page — activity-ktx 1.13.0 confirmed, March 11 2026 — HIGH confidence
- AndroidX Fragment releases page — fragment-ktx 1.8.9 confirmed, August 13 2025 — HIGH confidence
- AndroidX Core releases page — core-ktx 1.18.0 confirmed, March 11 2026 — HIGH confidence
- AndroidX AppCompat releases page — appcompat 1.7.1 confirmed, June 4 2025 — HIGH confidence
- AndroidX ConstraintLayout releases page — constraintlayout 2.2.1 confirmed, February 26 2025 — HIGH confidence
- Android Gradle Plugin releases page (`developer.android.com/build/releases/gradle-plugin`) — AGP 9.2.0 + Gradle 9.4.1 confirmed — HIGH confidence
- Kotlin releases page (`kotlinlang.org/docs/releases.html`) — Kotlin 2.3.21 confirmed, April 23 2026 — HIGH confidence
- Hilt docs (`developer.android.com/training/dependency-injection/hilt-android`) — Hilt 2.57.1 confirmed — HIGH confidence
- Android Architecture docs (`developer.android.com/topic/architecture`) — StateFlow + UDF pattern confirmed as 2025-2026 recommendation — HIGH confidence
- Kotlin DSL migration docs (`developer.android.com/build/migrate-to-kotlin-dsl`) — Kotlin DSL is default since Android Studio Giraffe — HIGH confidence
- Firebase BoM version 33.7.0 — MEDIUM confidence (training data; Firebase release cadence is ~quarterly; verify current version at `firebase.google.com/support/release-notes/android` when setting up)
- Material Components version 1.12.0 — MEDIUM confidence (training data; official MDC release page was not accessible during research)
- `kotlinx-coroutines` version 1.9.0 — MEDIUM confidence (training data; verify at `github.com/Kotlin/kotlinx.coroutines/releases`)

---
*Stack research for: Android Kotlin capstone — Firebase Auth + Firestore + Navigation*
*Researched: 2026-05-04*
