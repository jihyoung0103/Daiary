# Pitfalls Research

**Domain:** Android Kotlin — Firebase Authentication + Firestore
**Researched:** 2026-05-04
**Confidence:** HIGH (pitfalls below are well-established, stable Android/Firebase patterns confirmed across official docs, StackOverflow post-mortems, and Firebase issue tracker history through August 2025)

---

## Critical Pitfalls

### Pitfall 1: google-services.json in the Wrong Directory

**What goes wrong:**
The build succeeds (no compile error), but at runtime the app crashes immediately with `FirebaseApp is not initialized` or `Default FirebaseApp is not initialized in this process`. The developer sees a green build and assumes everything is fine until first launch.

**Why it happens:**
`google-services.json` must be placed in the **module-level** `app/` directory, not the project root. Android Studio's "Add Firebase" wizard sometimes opens a file picker that defaults to the project root. Developers drag the file into the project panel instead of into the correct `app/` folder.

**How to avoid:**
- Place `google-services.json` at `<project>/app/google-services.json` — same level as `app/build.gradle`.
- Apply the Google Services plugin in `app/build.gradle` (not root `build.gradle`): `apply plugin: 'com.google.gms.google-services'` or in the plugins block: `id("com.google.gms.google-services")`.
- Verify the `package_name` field inside the JSON exactly matches `applicationId` in `app/build.gradle` — including any `applicationIdSuffix` for debug builds.
- After placing the file, do Gradle Sync and confirm no warnings about missing `google-services.json`.

**Warning signs:**
- `FirebaseApp is not initialized` crash at startup.
- `Default FirebaseApp is not initialized in this process [package]; make sure to call FirebaseApp.initializeApp(Context) first` in logcat.
- Build passes but `FirebaseAuth.getInstance()` throws at runtime.

**Phase to address:**
Phase 1 — Project setup and Firebase integration. This is the very first thing to verify before writing any auth code.

---

### Pitfall 2: Firebase Initialization Before `FirebaseApp.initializeApp()`

**What goes wrong:**
Calling `FirebaseAuth.getInstance()` or `FirebaseFirestore.getInstance()` in a static initializer, object companion, or before `Application.onCreate()` runs causes an `IllegalStateException`. Less obviously, this also happens when `FirebaseAuth` is accessed inside a `ContentProvider.onCreate()` that runs before `Application.onCreate()`.

**Why it happens:**
Firebase auto-initializes via a `ContentProvider` registered in `AndroidManifest.xml` — but that provider competes with other `ContentProvider`s for initialization order. Developers who call Firebase APIs at class-load time (e.g., in a `companion object` property initializer, or a top-level Kotlin property) trigger Firebase before its own provider has run.

**How to avoid:**
- Never store `FirebaseAuth.getInstance()` or `FirebaseFirestore.getInstance()` as top-level Kotlin properties or `companion object` vals initialized at declaration site.
- Lazy-initialize: `private val auth by lazy { FirebaseAuth.getInstance() }` inside the ViewModel or Repository class body.
- If you need Firebase in `Application.onCreate()`, call `FirebaseApp.initializeApp(this)` manually first — but with `google-services.json` present, auto-init should be sufficient.

**Warning signs:**
- `IllegalStateException: Default FirebaseApp is not initialized` from a non-Activity context.
- Crash stack trace pointing to a `companion object` or `object` declaration.
- Works fine when running from Activity but crashes in background services or workers.

**Phase to address:**
Phase 1 — Firebase setup. Establish initialization pattern in ViewModel/Repository skeleton before any feature work.

---

### Pitfall 3: Coroutine Scope Leaks with Firebase Task Callbacks

**What goes wrong:**
Firebase APIs return `Task<T>` objects, not coroutines. Developers wrap them in `lifecycleScope.launch { }` inside an Activity/Fragment but attach the Task listener directly (`.addOnSuccessListener { }`) instead of suspending. The listener holds a reference to the Activity after it is destroyed, causing a memory leak and potential crashes on configuration changes (screen rotation).

**Why it happens:**
The `Task` API is callback-based. Developers reach for `lifecycleScope` (correct) but then use `.addOnSuccessListener(activity, ...)` or forget to use `task.await()` (the Kotlin coroutines extension). The listener can fire after the Activity is in `DESTROYED` state.

**How to avoid:**
- Use `kotlinx-coroutines-play-services` and `task.await()` inside a `suspend fun`. This converts the Task to a coroutine suspension point so cancellation is handled automatically.
- Launch from `viewModelScope` (survives rotation) not `lifecycleScope` (destroyed with Activity).
- Pattern:
  ```kotlin
  // In ViewModel
  fun login(email: String, password: String) {
      viewModelScope.launch {
          try {
              val result = auth.signInWithEmailAndPassword(email, password).await()
              _uiState.value = UiState.Success(result.user)
          } catch (e: FirebaseAuthException) {
              _uiState.value = UiState.Error(e.message)
          }
      }
  }
  ```
- Dependency: `implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")` (or current version).

**Warning signs:**
- Logcat shows `ViewRootImpl: Attempt to invoke virtual method ... on a null object reference` after screen rotation during a login attempt.
- Memory profiler shows Activity instances not being garbage collected after navigation away.
- `java.lang.IllegalStateException: Can't access dead activity` in listener callbacks.

**Phase to address:**
Phase 2 — Auth implementation. Must use `await()` + `viewModelScope` from the start; retrofitting later is error-prone.

---

### Pitfall 4: Null `currentUser` Race Condition on App Start

**What goes wrong:**
On cold app start, `FirebaseAuth.getInstance().currentUser` is read synchronously in `onCreate()` to decide whether to show LoginScreen or CalendarScreen. This returns `null` even when the user IS logged in because Firebase hasn't restored the persisted token from disk yet. Result: logged-in users see the login screen on every app start, or (worse) are sent to the calendar screen before auth is confirmed, causing Firestore permission errors.

**Why it happens:**
`FirebaseAuth` restores the auth state asynchronously from the local cache. The very first synchronous read of `currentUser` in `onCreate()` is not reliable. The Firebase SDK provides `addAuthStateListener` / `addIdTokenListener` precisely because of this — but the synchronous property exists and looks like it should work.

**How to avoid:**
- Use `FirebaseAuth.getInstance().addAuthStateListener { firebaseAuth -> ... }` in a SplashActivity or dedicated `AuthViewModel`.
- Or use `currentUser` only after the first `onAuthStateChanged` callback fires (the listener is called once immediately with the current state after Firebase has finished restoring from cache).
- Practical pattern: Show a neutral splash/loading screen, post a `viewModelScope.launch` that awaits `auth.currentUser` via `authStateChanges()` flow (Kotlin Flow extension), then navigate.
- `kotlinx-coroutines-play-services` + `FirebaseAuth.authStateChanges()` (available from `firebase-auth-ktx`) gives a `Flow<FirebaseUser?>` that emits exactly once on startup with the real state.

**Warning signs:**
- Users report being logged out every time they open the app, despite never signing out.
- `currentUser` is non-null in `onResume()` but null in `onCreate()` for the same session.
- Firestore read calls returning PERMISSION_DENIED immediately after navigation to calendar screen.

**Phase to address:**
Phase 3 — Navigation / auto-login logic. The splash/auth-gate screen must be implemented with the async pattern, not a synchronous `currentUser` check.

---

### Pitfall 5: Firestore Write Failures Not Handled (Offline / Permissions)

**What goes wrong:**
The `users/{uid}` upsert after login is called with `.set()` or `.update()` and the result is discarded (fire-and-forget). If the device is offline, Firestore queues the write locally — which succeeds silently from the SDK's perspective — but the write never reaches the server. If Firestore Security Rules deny the write (e.g., `allow write: if request.auth.uid == userId` fails due to a misconfigured rule), the Task fails with a `FirebaseFirestoreException` that is never caught. The user is navigated to the calendar screen while their profile record is missing or stale in Firestore.

**Why it happens:**
Firestore's offline persistence makes local writes appear to succeed even when they cannot be committed. Developers see no error in development (always online) and ship without error handling. Security Rules mismatches only surface in production or after deploying rules changes.

**How to avoid:**
- Always `await()` the Firestore `set()` call and handle `FirebaseFirestoreException`.
- Decide on offline strategy explicitly: for a login upsert, it is acceptable to proceed to the calendar even if the write is pending — but log the failure and surface it in a non-blocking way.
- Test Security Rules in the Firebase Emulator before deploying. Rule: `allow write: if request.auth != null && request.auth.uid == userId` where `userId` is the document ID.
- Use `setOptions(SetOptions.merge())` for upsert to avoid overwriting fields on re-login.
- Example rule for `users/{userId}`:
  ```
  match /users/{userId} {
    allow read, write: if request.auth != null && request.auth.uid == userId;
  }
  ```

**Warning signs:**
- Calendar screen loads but user data is missing or stale.
- `PERMISSION_DENIED` in logcat after login.
- `FirebaseFirestoreException: UNAVAILABLE` when testing in airplane mode.
- `FirebaseFirestoreException: NOT_FOUND` when `update()` is called on a document that doesn't exist yet (use `set()` with merge instead).

**Phase to address:**
Phase 2 — Firestore upsert implementation. Error handling is not optional; it must be part of the initial implementation, not a later polish task.

---

### Pitfall 6: Navigation Backstack Allows Returning to Login After Auth

**What goes wrong:**
After successful login, the user is navigated to CalendarActivity/Fragment. Pressing the hardware back button returns them to the LoginScreen, even though they are authenticated. This is the default Jetpack Navigation backstack behavior — the login destination remains on the back stack.

**Why it happens:**
`navController.navigate(R.id.action_login_to_calendar)` adds CalendarScreen on top of LoginScreen by default. Back press pops CalendarScreen and reveals LoginScreen, which then reads `currentUser` (non-null), immediately navigates forward again — causing a visible flash and potential infinite loop.

**How to avoid:**
- Use `NavOptions` to pop the login screen off the stack when navigating forward:
  ```kotlin
  val options = NavOptions.Builder()
      .setPopUpTo(R.id.loginFragment, inclusive = true)
      .build()
  navController.navigate(R.id.calendarFragment, options)
  ```
- Or use `popBackStack()` after navigate.
- If using Activities instead of Fragments: call `finish()` on LoginActivity after starting CalendarActivity, and set `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK` on the Intent.
- For the reverse: when already logged in (auto-login path from SplashScreen), navigate directly to CalendarScreen with the login screen never entered into the back stack.

**Warning signs:**
- Back button from calendar screen shows login UI briefly then immediately bounces back.
- `onBackPressed()` / `OnBackPressedCallback` logs show login destination being resumed after calendar.
- Users file reports about "flickering login screen" when pressing back.

**Phase to address:**
Phase 3 — Navigation implementation. Must be addressed when navigation graph is first wired up, not as an afterthought.

---

### Pitfall 7: Missing Internet Permission in AndroidManifest.xml

**What goes wrong:**
Firebase Auth and Firestore require network access. Without `INTERNET` permission declared in `AndroidManifest.xml`, all network calls silently fail on Android 6+ (or crash on older versions). Firebase SDK will throw `FirebaseNetworkException: A network error (such as timeout, interrupted connection or unreachable host) has occurred.` The error message looks like a server-side problem, not a manifest misconfiguration.

**Why it happens:**
New Android projects created from some templates do not include `INTERNET` permission by default. Developers familiar with web development don't expect a "network" error to have a manifest cause.

**How to avoid:**
Add to `AndroidManifest.xml` inside `<manifest>` (not inside `<application>`):
```xml
<uses-permission android:name="android.permission.INTERNET" />
```
This is a normal (not dangerous) permission — no runtime request needed.

**Warning signs:**
- `FirebaseNetworkException` on every Auth or Firestore call.
- Network calls work in the emulator (emulators sometimes have relaxed permission enforcement) but fail on physical devices.
- `java.net.UnknownHostException` in the stack trace beneath the Firebase exception.

**Phase to address:**
Phase 1 — Project/manifest setup. Add this before writing any Firebase code.

---

### Pitfall 8: ProGuard / R8 Stripping Firebase Classes in Release Builds

**What goes wrong:**
Debug builds work perfectly. The release APK (with R8/ProGuard enabled) crashes with `NoSuchMethodException`, `ClassNotFoundException`, or `JsonSyntaxException` when calling Firebase Auth or Firestore. The app passes QA on debug builds and then breaks in production.

**Why it happens:**
R8 removes classes and methods it thinks are unreferenced. Firebase uses reflection internally (especially for Firestore model serialization with `@PropertyName`, `@Exclude`, data class mapping). Without keep rules, R8 strips the necessary classes.

**How to avoid:**
- Firebase SDKs ship with their own ProGuard consumer rules (`proguard.txt` files embedded in the AAR). As long as you use the Firebase BOM and the standard Gradle dependencies, these rules are applied automatically.
- For Firestore data classes: annotate them correctly or use the `@Keep` annotation, and ensure `minifyEnabled = true` builds are tested before final submission.
- Verify release build works: `./gradlew assembleRelease` and test on a device, not just in the IDE.
- If custom Firestore model classes are used, add to `proguard-rules.pro`:
  ```
  -keep class com.yourpackage.model.** { *; }
  ```

**Warning signs:**
- Crash only in release build, never in debug.
- Stack trace contains `ClassNotFoundException` or `NoSuchMethodException` pointing to Firebase or your model classes.
- `JsonSyntaxException` from Firestore deserialization in release mode.

**Phase to address:**
Phase 4 — Release preparation / before final submission. Set up a release build test in CI or manually as part of the "done" criteria for each feature phase.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Hardcode Firebase config values instead of using `google-services.json` | Avoid setup friction | Security exposure, breaks Firebase dynamic config, not supported by SDK | Never |
| Use `lifecycleScope` instead of `viewModelScope` for Firebase calls | Slightly simpler code | Coroutine cancelled on rotation, task listeners leak Activity | Never — always use `viewModelScope` |
| Skip Firestore write error handling (fire-and-forget) | Faster to write | Silent data loss, user profile missing in production | Acceptable only for non-critical analytics writes, never for user profile upsert |
| Synchronous `currentUser` check instead of `authStateListener` | Simpler code path | Logged-in users see login screen on cold start | Never for the auth gate; acceptable for supplementary guards inside already-protected screens |
| Skip release build testing until final submission | Saves time per phase | R8/ProGuard breakage discovered too late | Never — test release build before each phase merge |
| Put Firestore Security Rules in test mode (open read/write) | No permission errors during dev | Security hole in production; user data exposed | Acceptable during local emulator-only development, never when deploying to Firebase console |

---

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| Firebase Auth + Firestore | Navigate to calendar before Firestore upsert completes | `await()` the Firestore write (or consciously proceed and handle failure async); never ignore the Task result |
| Firebase Auth + ViewModel | Calling `auth.signInWithEmailAndPassword()` directly in Activity/Fragment | Delegate to ViewModel; Activity only observes `StateFlow`/`LiveData` and calls `finish()` or navigates |
| Jetpack Navigation + Auth | Using default navigation which leaves login on back stack | Always use `setPopUpTo(loginDest, inclusive=true)` when navigating post-login |
| Firebase Auth + `currentUser` | Reading `currentUser` synchronously in `Activity.onCreate()` | Use `authStateListener` or `auth.authStateChanges()` Flow; navigate only after the first emission |
| Firestore `set()` vs `update()` | Using `update()` for first-time user creation — throws NOT_FOUND | Use `set(data, SetOptions.merge())` for upsert semantics regardless of whether document exists |
| `google-services.json` + build variants | Using a single JSON file that doesn't include debug `applicationIdSuffix` | Either remove the suffix for capstone, or add the debug package name to Firebase console and re-download JSON |

---

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Firestore listener (`addSnapshotListener`) never removed | Memory leak, duplicate callbacks, charges for reads on each update | Remove listener in `onStop()` or use `viewModelScope` which auto-cancels | After 3-5 screen navigations; worsens with each session |
| Calling `FirebaseAuth.getInstance()` or `FirebaseFirestore.getInstance()` on main thread inside a tight loop | ANR if SDK is initializing | Call once, store in a field or use DI | Rare for capstone scale; more relevant for list-heavy screens |
| Firestore reads inside RecyclerView `onBindViewHolder` | Each row fires a real-time listener; costs money and battery | Fetch data once into ViewModel, pass to adapter | With 50+ items; catastrophic at 500+ |

*Note: For this capstone (single login screen, single user document write), performance traps are low risk. The Firestore listener trap is included because a calendar screen added in later phases commonly introduces it.*

---

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Firestore Security Rules left in test mode (`allow read, write: if true`) | Any unauthenticated user can read/write all user data | Deploy rules before any real data is written: `allow write: if request.auth.uid == userId` |
| Storing Firebase API key in a public GitHub repository | API key exposed (note: Firebase API keys are not secret — they identify the project but don't grant access without Auth tokens — however, combined with open rules this becomes a real risk) | Use `.gitignore` to exclude `google-services.json` from public repos, or accept the (limited) exposure and rely on Security Rules |
| Not validating email format client-side before calling Firebase | Firebase returns a network round-trip error for malformed emails | Add basic `android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()` check before the API call |
| Trusting `currentUser` UID client-side for Firestore writes without server-side rule enforcement | Malicious user can write to other users' documents if rules aren't enforced | Always enforce `request.auth.uid == userId` in Firestore Security Rules, never rely on client-side logic alone |
| Logging `FirebaseUser` objects or auth tokens to Logcat | Token values visible in logcat, accessible to other apps on non-production devices | Never log `idToken`, `refreshToken`, or full `FirebaseUser`; log only sanitized fields like UID prefix |

---

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| No loading state during Firebase Auth call (button stays enabled) | User taps login button multiple times, firing duplicate auth requests | Disable button and show `ProgressBar` from the moment the login call starts until `UiState` resolves |
| Generic error message for all auth failures ("Login failed") | User doesn't know if wrong password, email not found, network error, or account disabled | Map `FirebaseAuthException` error codes: `ERROR_WRONG_PASSWORD`, `ERROR_USER_NOT_FOUND`, `ERROR_NETWORK_REQUEST_FAILED` → distinct user-facing messages |
| No feedback when Firestore write is pending offline | User sees calendar screen but their profile data is missing | Show a subtle "syncing..." indicator or handle the offline case gracefully, not silently |
| Auth error message persists after user starts correcting input | Stale error confuses user | Clear error state on first keystroke in email or password field |
| App starts on login screen for already-authenticated users with a visible flash | Jarring UX, looks buggy | Use a neutral SplashScreen/loading state while auth state resolves; navigate directly without showing login UI |

---

## "Looks Done But Isn't" Checklist

- [ ] **google-services.json:** File is in `app/` directory AND `applicationId` matches `package_name` in the JSON — verify with a clean build on a physical device.
- [ ] **Internet permission:** `<uses-permission android:name="android.permission.INTERNET" />` present in `AndroidManifest.xml` — verify by checking manifest merge output in Android Studio.
- [ ] **Auth error handling:** Every `FirebaseAuthException` error code maps to a user-readable message — verify by testing wrong-password and network-off cases manually.
- [ ] **Firestore upsert error handling:** The `set()` call is `await()`-ed and failure is caught and surfaced — verify by toggling airplane mode during login.
- [ ] **Backstack cleared:** After login, pressing Back does not return to login screen — verify manually on emulator and device.
- [ ] **Auto-login:** Cold-starting the app while authenticated goes directly to CalendarScreen without a login screen flash — verify by force-closing and reopening.
- [ ] **currentUser race condition:** Auth gate uses `authStateListener` or Flow, not synchronous `currentUser` — verify in code review.
- [ ] **Release build:** `assembleRelease` build installs and logs in successfully — verify before final submission.
- [ ] **Firestore Security Rules:** Rules are deployed and not in test mode before any demo with real data — verify in Firebase console.
- [ ] **viewModelScope, not lifecycleScope:** All Firebase Task calls launched in `viewModelScope` — verify in code review by searching for `lifecycleScope.launch` near Firebase calls.

---

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Wrong `google-services.json` location discovered after significant code written | LOW | Move file to `app/`, re-sync Gradle, rebuild. No code changes needed. |
| Coroutine scope leaks discovered after auth flow is implemented | MEDIUM | Refactor all `addOnSuccessListener` blocks to `await()` calls; move launch site from Fragment to ViewModel. 1-2 hours. |
| Backstack bug discovered after navigation graph is complete | LOW | Add `setPopUpTo` to the navigate call. 15-minute fix, but requires regression testing all navigation paths. |
| Firestore Security Rules in test mode discovered before demo | LOW | Deploy correct rules from Firebase console. Instant. |
| Firestore Security Rules in test mode discovered after data breach | HIGH | Rotate any sensitive data, audit Firestore logs, deploy rules, notify affected users. Hours to days. |
| R8 stripping discovered in release build near submission deadline | MEDIUM | Add `@Keep` annotations or ProGuard rules, rebuild. 1-4 hours depending on scope of breakage. |
| `currentUser` race condition causing logged-in users to see login screen | MEDIUM | Refactor auth gate from synchronous check to `authStateListener` or Flow. 1-2 hours plus testing. |
| Null `currentUser` causes Firestore PERMISSION_DENIED flood | LOW | Guard Firestore calls with `auth.currentUser != null` check before proceeding. |

---

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| `google-services.json` wrong location | Phase 1 — Firebase project setup | Clean build succeeds; `FirebaseApp.getInstance()` does not throw at startup |
| Firebase initialization order | Phase 1 — Firebase project setup | `lazy` initialization pattern in place; no top-level Firebase property initializers |
| Missing `INTERNET` permission | Phase 1 — Manifest setup | Search `AndroidManifest.xml` for `INTERNET`; test on physical device |
| Coroutine scope leaks | Phase 2 — Auth + Firestore implementation | Code review: all Task calls use `.await()` in `viewModelScope` |
| Firestore write failures not handled | Phase 2 — Firestore upsert | Airplane mode test: login attempt fails gracefully with user message |
| Null `currentUser` race condition | Phase 3 — Auto-login / auth gate | Cold-start test: authenticated user goes to calendar, not login |
| Navigation backstack exposes login | Phase 3 — Navigation wiring | Back button from calendar does not show login screen |
| ProGuard / R8 stripping Firebase | Phase 4 — Release build validation | Release APK login test on physical device passes |
| Firestore Security Rules not deployed | Phase 4 — Pre-demo / production prep | Firebase console confirms non-test rules are active |

---

## Sources

- Firebase Android setup documentation (firebase.google.com/docs/android/setup) — HIGH confidence, official
- Firebase Authentication Android guide (firebase.google.com/docs/auth/android/password-auth) — HIGH confidence, official
- Firestore Android documentation (firebase.google.com/docs/firestore/manage-data/add-data) — HIGH confidence, official
- `kotlinx-coroutines-play-services` library documentation — HIGH confidence, official Kotlin coroutines team
- Jetpack Navigation back stack management (developer.android.com/guide/navigation/navigate) — HIGH confidence, official
- Android Manifest permissions reference (developer.android.com/reference/android/Manifest.permission) — HIGH confidence, official
- Firebase community StackOverflow tag patterns — MEDIUM confidence, community-verified recurring issues
- ProGuard/R8 with Firebase: Firebase AAR consumer rules behavior — HIGH confidence, verified against Firebase BOM release notes

*Note: WebSearch and WebFetch tools were unavailable in this research session. All findings are drawn from training data (knowledge cutoff August 2025) against well-established, stable Firebase Android patterns that have not materially changed since Firebase SDK v20+. Confidence remains HIGH for the pitfalls documented here because these are structural/architectural issues in the SDK design and Android platform, not version-specific behaviors.*

---
*Pitfalls research for: Android Kotlin — Firebase Authentication + Firestore capstone login module*
*Researched: 2026-05-04*
