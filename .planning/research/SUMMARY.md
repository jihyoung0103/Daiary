# Project Research Summary

**Project:** Capstone Login - Android Firebase Auth
**Domain:** Android Kotlin mobile app - Firebase Auth (email/password) + Firestore, no backend server
**Researched:** 2026-05-04
**Confidence:** HIGH

## Executive Summary

This is a serverless Android login module built entirely on Firebase, no Spring Boot backend. The standard approach is a single-Activity architecture with Jetpack Navigation Component managing a LoginFragment and a CalendarFragment (placeholder), backed by MVVM with ViewModel + StateFlow, a Repository layer that wraps Firebase SDK calls as suspend functions, and coroutines (viewModelScope) for all async work. This architecture is the current Google recommendation and is directly suited to a 2-3 screen capstone: it avoids over-engineering (no Hilt, no Room) while producing code that is readable, testable, and grader-friendly.

The recommended stack is fully pinned: Kotlin 2.3.21, AGP 9.2.0 / Gradle 9.4.1, compileSdk/targetSdk 36, minSdk 26, Firebase BoM 33.7.0 (verify latest at project creation), Lifecycle 2.10.0, Navigation 2.9.8, and kotlinx-coroutines-play-services 1.9.0 for Task.await(). All dependencies should be managed via a version catalog (libs.versions.toml) and Kotlin DSL build scripts. The project must be created fresh in Android Studio as a proper Android Gradle project - the existing IntelliJ stub with Main.kt cannot be converted and must be discarded.

The biggest risks in this project are operational, not architectural. Placing google-services.json in the wrong directory silently breaks Firebase at runtime despite a green build. Using a synchronous currentUser check for auto-login causes a race condition on cold start. Leaving login on the navigation back stack allows users to press Back from the calendar and return to the login screen. All three are easy to prevent if addressed at the correct phase, and all three are difficult to discover after the fact without explicit verification steps.

---

## Key Findings

### Recommended Stack

Build a fresh Android Gradle project using Kotlin DSL and version catalogs. All AndroidX versions are pinned from official release pages (HIGH confidence). Firebase BoM version should be verified against firebase.google.com/support/release-notes/android at project creation time - the catalog entry of 33.7.0 is the research baseline; actual latest may be 34.x.

**Core technologies:**

- **Kotlin 2.3.21** - primary language; K2 compiler default since 2.0, faster builds
- **AGP 9.2.0 + Gradle 9.4.1** - required pairing for compileSdk 36; Kotlin DSL only
- **compileSdk/targetSdk 36, minSdk 26** - API 36 required for 2025+ Play Store; minSdk 26 covers ~95% of devices without compatibility shims
- **Firebase BoM 33.7.0+** - use BoM so firebase-auth-ktx and firebase-firestore-ktx versions stay in sync; never pin individual Firebase libs
- **kotlinx-coroutines-play-services 1.9.0** - provides Task.await(); converts all Firebase Task callbacks to suspend functions; without this, callbacks must be used everywhere
- **Lifecycle 2.10.0 + Navigation 2.9.8** - viewModelScope, StateFlow, repeatOnLifecycle(STARTED), NavController with popUpTo
- **Material Components 1.12.0** - TextInputLayout + TextInputEditText + MaterialButton; required for professional login form UX
- **ViewBinding** - buildFeatures { viewBinding = true }; replaces findViewById; no Compose needed for this scope
- **No Hilt** - by viewModels() delegate + Firebase singletons in ViewModel is sufficient; Hilt adds boilerplate not justified by 2-3 ViewModels

Full libs.versions.toml and app/build.gradle.kts templates are in .planning/research/STACK.md.

---

### Expected Features

**Must have (P1 - table stakes, grader requirements):**

- Firebase project configured, google-services.json in app/ - hard blocker; nothing else works
- Login screen: email + password fields, login button, loading indicator
- Registration screen: email + password + optional display name
- Client-side email format validation (android.util.Patterns.EMAIL_ADDRESS) and password length check (min 6 chars)
- Loading/progress state while auth call is in flight - disable button to prevent duplicate requests
- Specific error messages per FirebaseAuthException error code (ERROR_WRONG_PASSWORD, ERROR_USER_NOT_FOUND, ERROR_EMAIL_ALREADY_IN_USE, ERROR_NETWORK_REQUEST_FAILED, ERROR_TOO_MANY_REQUESTS) - Korean strings appropriate for this project
- Firestore upsert on login success (users/{uid} document with set(..., SetOptions.merge()))
- Auto-login routing at app start (auth state listener, not synchronous currentUser read)
- Navigate to CalendarFragment placeholder on success - login screen removed from back stack
- Sign-out from the calendar screen

**Should have (P2 - polish pass before submission):**

- Password visibility toggle - one line with TextInputLayout endIconMode
- Display name stored in Firestore on registration
- IME Next/Done actions on input fields
- Email verification enforcement after registration

**Defer to v2+:**

- Biometric unlock, password reset flow, user profile edit screen, Google Sign-In / OAuth (explicitly out of scope per PROJECT.md)

---

### Architecture Approach

Use single-Activity MVVM with a layered data path: Fragment -> ViewModel -> Repository -> DataSources -> Firebase. The ViewModel holds a single StateFlow<AuthUiState> (sealed class: Idle, Loading, Success, Error). The Fragment collects it inside repeatOnLifecycle(STARTED) and drives navigation from the Success state. The Repository is the only class the ViewModel talks to; it catches FirebaseAuthException and returns Result<T>. DataSource classes are the only classes that import com.google.firebase.*.

**Major components (build in this order):**

1. **model/User.kt + util/Result.kt** - no dependencies; foundation for all layers above
2. **FirebaseAuthDataSource** - wraps FirebaseAuth singleton; converts Tasks to suspend via .await()
3. **FirestoreUserDataSource** - wraps FirebaseFirestore; implements upsertUser() with set(merge=true)
4. **AuthRepository** - sequences auth sign-in + Firestore upsert; wraps exceptions as Result.Error
5. **AuthViewModel** - holds StateFlow<AuthUiState>; calls repository in viewModelScope.launch
6. **nav_graph.xml** - defines loginFragment (start destination) and calendarFragment + navigation action
7. **LoginFragment** - wires form to ViewModel; collects state; navigates on Success with popUpTo
8. **MainActivity** - NavHostFragment container; checks auth state via listener for auto-login
9. **CalendarFragment** - stub; needs to exist only as a navigation destination

Full package structure and code examples for each pattern are in .planning/research/ARCHITECTURE.md.

---

### Critical Pitfalls

1. **google-services.json in wrong directory** - Place in app/ (not project root); verify package_name in JSON matches applicationId in app/build.gradle.kts. Phase 1. A green build with this misconfiguration still crashes at runtime with FirebaseApp is not initialized.

2. **Synchronous currentUser race condition** - FirebaseAuth.getInstance().currentUser is unreliable on cold start. Use FirebaseAuth.authStateChanges() Flow (from firebase-auth-ktx) or addAuthStateListener. Phase 3. Symptom: logged-in users see the login screen every launch.

3. **Login screen left on back stack after auth** - Always use NavOptions.Builder().setPopUpTo(R.id.loginFragment, inclusive = true) when navigating on Success. Phase 3.

4. **Firebase Task callbacks in lifecycleScope instead of viewModelScope** - lifecycleScope is destroyed on rotation; Task listeners fire after onDestroy, causing crashes or memory leaks. All Firebase calls must use .await() in viewModelScope. Phase 2. Retrofitting after the fact takes 1-2 hours.

5. **Missing INTERNET permission** - Without android.permission.INTERNET in AndroidManifest.xml, all Firebase network calls fail with FirebaseNetworkException. Phase 1. Add before writing any Firebase code.

6. **Firestore Security Rules left in test mode** - Replace open rules with allow write: if request.auth.uid == userId before any demo. Phase 4.

7. **R8/ProGuard stripping Firebase classes in release builds** - Firebase BoM ships consumer ProGuard rules that apply automatically; custom Firestore model classes need @Keep annotations. Test assembleRelease before submission. Phase 4.

---

## Implications for Roadmap

All research converges on a 4-phase structure derived from the pitfall-to-phase mapping in PITFALLS.md, the component build order in ARCHITECTURE.md, and the P1/P2/P3 feature tiers in FEATURES.md.

### Phase 1: Project Foundation + Firebase Setup

**Rationale:** Everything else depends on a working Firebase-connected Android project. The three Phase 1 pitfalls all block feature work if not resolved here. The existing IntelliJ stub must be discarded; a new Android Gradle project must be created in Android Studio.

**Delivers:** A buildable Android project with Firebase Auth + Firestore initialized, verified on a physical device or emulator with a debug login call. No UI polish required.

**Addresses:** Firebase project setup, google-services.json placement in app/, manifest INTERNET permission, libs.versions.toml version catalog, lazy Firebase initialization pattern in ViewModel skeleton.

**Avoids:** FirebaseApp is not initialized crash, FirebaseNetworkException from missing permission, class-load-time initialization crash.

**Research flag:** Standard, well-documented setup. Follow STACK.md libs.versions.toml template exactly. No deeper research needed.

---

### Phase 2: Auth Core - Login + Registration + Firestore Upsert

**Rationale:** Core user-facing value. All P1 features live here. Architecture must be correct from the start - retrofitting viewModelScope + .await() after callback-based code costs 1-2 hours.

**Delivers:** Working login and registration flows end-to-end: email/password form -> Firebase Auth -> Firestore upsert -> navigate to CalendarFragment placeholder. Includes loading states, specific error messages per FirebaseAuthException error code, and client-side validation.

**Uses:** AuthRepository, FirebaseAuthDataSource, FirestoreUserDataSource, AuthViewModel with StateFlow<AuthUiState>, viewModelScope.launch with task.await(), SetOptions.merge() for upsert.

**Implements:** Full layered data path (architecture layers 1-5 of the component build order).

**Avoids:** Callbacks in Fragment/Activity, lifecycleScope for Firebase Tasks, fire-and-forget Firestore write without error handling.

**Research flag:** Patterns fully documented in ARCHITECTURE.md with complete code examples. No deeper research needed.

---

### Phase 3: Navigation + Auto-Login

**Rationale:** The navigation layer must be wired after the ViewModel is verified to emit correct states. Both navigation pitfalls (backstack and currentUser race condition) must be addressed here.

**Delivers:** Complete navigation graph, popUpTo(loginFragment, inclusive=true) on success, auto-login via authStateChanges() Flow in MainActivity, sign-out from CalendarFragment returning to login. Zero back-stack issues verified manually.

**Addresses:** Auto-login routing (PROJECT.md Active requirement), navigation back-stack clearing, auth state listener pattern replacing synchronous currentUser.

**Avoids:** Login screen visible on Back press from calendar, logged-in users seeing login screen on cold start, currentUser null race condition.

**Research flag:** popUpTo and authStateChanges() patterns are standard Navigation Component and Firebase Auth documentation. No deeper research needed.

---

### Phase 4: Polish + Release Validation

**Rationale:** P2 features are quick wins once P1 is solid. Release build testing and Firestore Security Rules hardening must happen before demo - both pitfalls are catastrophic if discovered at demo time.

**Delivers:** P2 UX improvements (password toggle, display name, IME actions, email verification), Firestore Security Rules deployed in non-test mode, release APK verified on device, google-services.json excluded from public git.

**Avoids:** R8 stripping Firebase classes in release build, user data exposed by open Firestore rules at demo.

**Research flag:** Security Rules syntax and R8 rules follow standard templates in PITFALLS.md. No deeper research needed.

---

### Phase Ordering Rationale

- Phase 1 before everything: Firebase connectivity is a hard gate. Every other feature is blocked without google-services.json correctly placed and verified.
- Phase 2 before Phase 3: The ViewModel must emit correct AuthUiState values before the nav graph can react to them. Building navigation before auth produces untestable wiring.
- Phase 3 before Phase 4: Polish and release prep should not be applied to a navigation structure that still has backstack bugs.
- Phase 4 last: Polish features add no grader value if the core auth flow is broken. R8 testing must happen after the full feature set is stable.

### Research Flags

Phases with standard patterns (skip /gsd-research-phase):

- **Phase 1:** Android project creation + Firebase setup is thoroughly documented in STACK.md.
- **Phase 2:** MVVM + StateFlow + Task.await() patterns are fully detailed in ARCHITECTURE.md with complete code examples.
- **Phase 3:** Navigation Component popUpTo and authStateChanges() Flow are official documentation patterns.
- **Phase 4:** ProGuard rules and Firestore Security Rules follow standard templates in PITFALLS.md.

No phase requires additional research. All patterns are covered by the existing four research files.

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | All AndroidX versions confirmed from official release pages. Firebase BoM 33.7.0 is MEDIUM - verify current version at firebase.google.com/support/release-notes/android before project creation. Coroutines 1.9.0 is MEDIUM - verify at github.com/Kotlin/kotlinx.coroutines/releases. |
| Features | HIGH | Firebase Auth SDK behavior and error codes are stable since 2016. Feature list aligned with PROJECT.md requirements. |
| Architecture | HIGH | Patterns drawn directly from official Android Architecture documentation and Firebase Android guides. |
| Pitfalls | HIGH | Pitfalls are structural/architectural, not version-specific. Stable patterns confirmed through August 2025. |

**Overall confidence:** HIGH

### Gaps to Address

- **Firebase BoM version:** 33.7.0 may be outdated by May 2026. Verify at firebase.google.com/support/release-notes/android at project creation. No breaking changes expected within the 33.x/34.x series.

- **Coroutines version compatibility with Kotlin 2.3.21:** Verify the exact compatible release at github.com/Kotlin/kotlinx.coroutines/releases before pinning.

- **currentUser async behavior on specific device/emulator combinations:** The authStateChanges() Flow approach is the universal fix; prioritize it over the synchronous check regardless of what works in emulator testing.

- **CalendarActivity vs CalendarFragment:** PROJECT.md references CalendarActivity in several places. ARCHITECTURE.md recommends a single-Activity architecture with CalendarFragment as a nav destination. Confirm this decision before Phase 3.

---

## Sources

### Primary (HIGH confidence)

- developer.android.com/jetpack/androidx/releases/lifecycle - Lifecycle 2.10.0, April 22 2026
- developer.android.com/jetpack/androidx/releases/navigation - Navigation 2.9.8, April 22 2026
- developer.android.com/jetpack/androidx/releases/activity - activity-ktx 1.13.0, March 11 2026
- developer.android.com/jetpack/androidx/releases/fragment - fragment-ktx 1.8.9, August 13 2025
- developer.android.com/jetpack/androidx/releases/core - core-ktx 1.18.0, March 11 2026
- developer.android.com/build/releases/gradle-plugin - AGP 9.2.0 + Gradle 9.4.1
- kotlinlang.org/docs/releases.html - Kotlin 2.3.21, April 23 2026
- developer.android.com/topic/architecture - StateFlow + UDF pattern, 2025-2026 recommendation
- firebase.google.com/docs/auth/android/password-auth - Firebase Auth Android email/password guide
- firebase.google.com/docs/firestore/manage-data/add-data - Firestore set with merge (upsert)
- developer.android.com/guide/navigation/navigate - Navigation back stack management, popUpTo
- developer.android.com/guide/navigation/use-graph/conditional - Conditional navigation (login flow)

### Secondary (MEDIUM confidence)

- Firebase BoM 33.7.0 - training data; verify current version at firebase.google.com/support/release-notes/android before project creation
- kotlinx-coroutines 1.9.0 - training data; verify compatibility with Kotlin 2.3.21
- Material Components 1.12.0 - training data; official MDC release page was not accessible during research

---

*Research completed: 2026-05-04*
*Ready for roadmap: yes*
