# Feature Research

**Domain:** Android Kotlin app — Firebase Auth (email/password) + Firestore user storage + Navigation to calendar
**Researched:** 2026-05-04
**Confidence:** HIGH (Firebase Auth Android is a stable, well-documented SDK; patterns are consistent across the ecosystem)

## Feature Landscape

### Table Stakes (Users Expect These / Graders Require These)

Features that must exist. Missing any of these = app feels broken, or capstone grader marks you down.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Email + password login form | Entry point to the entire app | LOW | EditText for email, EditText for password (inputType="textPassword") |
| Client-side email format validation | Users expect instant feedback before hitting the server | LOW | `android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()` — no network needed |
| Client-side password length check | Prevents obvious errors before Firebase rejects | LOW | Firebase requires minimum 6 chars; check this locally first |
| Login button disabled while form is invalid | Standard Android form UX; prevents empty submissions | LOW | TextWatcher or Flow on both fields, enable button only when both pass |
| Loading/progress indicator during auth | Auth call is async; blank frozen UI = users tap again | LOW | ProgressBar or button disable + spinner; show on call start, hide on result |
| Specific error messages per Firebase error code | "Something went wrong" is unacceptable; users need to know if email is wrong vs. password | MEDIUM | Map `FirebaseAuthException.errorCode` to human-readable strings (see Dependency Notes) |
| Session persistence across app restarts | Users must not re-login every launch | LOW | Firebase Auth persists token by default via `FirebaseAuth.getInstance().currentUser` |
| Auto-login check at app start (splash/entry routing) | If already signed in, skip login screen entirely | LOW | Check `currentUser != null` in `MainActivity` or a dedicated `SplashActivity` before navigating |
| Navigate to main screen after successful login | The entire point of the module — login must land somewhere | LOW | `startActivity(Intent(this, CalendarActivity::class.java)); finish()` |
| Firestore upsert on login success | Grader requirement per PROJECT.md — user data must be persisted | MEDIUM | `set(..., SetOptions.merge())` on `users/{uid}` doc; handles first login and re-login |
| Sign-out capability | Every auth flow needs an exit; graders will test this | LOW | `FirebaseAuth.getInstance().signOut()` + navigate back to login |
| Registration / Sign-up screen | Users need to create accounts; login-only app is a dead end | MEDIUM | Same email/password fields + confirm password field; `createUserWithEmailAndPassword` |

### Differentiators (Competitive Advantage / Bonus Points)

These are not expected but earn extra credit for a capstone and improve UX.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Password visibility toggle | Reduces password entry frustration; standard in modern Android apps | LOW | `PasswordTransformationMethod` toggle on TextInputLayout endIconMode; one line with Material Components |
| "Remember me" checkbox / biometric unlock | Reduces friction for returning users | MEDIUM | Firebase persists by default; biometric requires `BiometricPrompt` API — meaningful bonus work |
| Email verification enforcement | Prevents spam accounts; shows auth maturity | MEDIUM | `currentUser.sendEmailVerification()` after registration; check `isEmailVerified` before routing to calendar |
| Display name stored in Firestore on register | Personalizes the calendar screen ("Welcome, [name]") | LOW | Capture name field during registration, write to Firestore `users/{uid}.displayName` |
| Auth state listener via `addAuthStateListener` | Reactive approach; handles token expiry and multi-device sign-out gracefully | LOW | Better than polling `currentUser`; attach in `onStart`, detach in `onStop` |
| Input field shake animation on invalid submission | Polished error UX that signals validation failure visually | LOW | `ObjectAnimator` or `TranslateAnimation` on the parent layout; ~10 lines |
| Keyboard "Next" / "Done" IME action | Email field Next moves focus to password; password Done triggers login | LOW | `imeOptions="actionNext"` / `actionDone` + `OnEditorActionListener` |

### Anti-Features (Things to Deliberately NOT Build for a Capstone)

| Feature | Why It Seems Good | Why It Is Problematic for This Project | Alternative |
|---------|-------------------|----------------------------------------|-------------|
| Google Sign-In / OAuth social login | "Modern apps have it" | Requires SHA-1 fingerprint config, Google Cloud Console setup, separate SDK; doubles scope for a login module | Commit to email/password only per PROJECT.md Out of Scope decision |
| Password reset / forgot password flow | Users will ask for it | Requires email deep-link handling, additional screen, Firebase Dynamic Links or manual flow; low ROI for capstone grade | Stub the button with a Toast "Coming soon" or omit entirely; PROJECT.md lists it as low priority |
| Custom JWT / token server | "More control" | Entirely negates the point of Firebase Auth; adds Spring Boot complexity you explicitly removed | Trust Firebase token management — it is production-grade |
| Real-time Firestore listener on login screen | "Stay in sync" | Login screen has no data to sync; attaching a snapshot listener here is wasted complexity and a battery drain | Write once on login (`set` with merge), read on demand from calendar screen |
| Multi-factor authentication (MFA) | "More secure" | Firebase MFA requires phone number verification, additional setup, and a different auth flow entirely | Email verification (see Differentiators) achieves security signaling with far less effort |
| Offline-first auth caching | "Works without internet" | Firebase Auth already caches credentials locally; rolling your own cache creates token staleness bugs | Rely on Firebase's built-in persistence; show "No internet" snackbar if auth call fails |
| User profile edit screen | "Complete user management" | Out of scope for this module; calendar screen is the target | Store the data in Firestore now; build profile editing in a later milestone when calendar is done |
| Complex Firestore data model (subcollections, etc.) | "Future-proof" | Single `users/{uid}` flat document is sufficient and testable; premature normalization adds Firestore rule complexity | Flat document: `{ uid, email, displayName, lastLoginAt, createdAt }` |

## Feature Dependencies

```
[Registration screen]
    └──requires──> [Email format validation]
    └──requires──> [Password length validation]
    └──requires──> [Firebase createUserWithEmailAndPassword]
                       └──requires──> [Firebase project setup + google-services.json]

[Login screen]
    └──requires──> [Email format validation]
    └──requires──> [Firebase signInWithEmailAndPassword]
                       └──requires──> [Firebase project setup + google-services.json]
                       └──on success──> [Firestore upsert users/{uid}]
                                           └──requires──> [Firestore initialized]
                       └──on success──> [Navigate to CalendarActivity]

[Auto-login routing]
    └──requires──> [FirebaseAuth.getInstance().currentUser check]
    └──on non-null──> [Navigate to CalendarActivity, skip login]

[Loading state]
    └──enhances──> [Login screen]
    └──enhances──> [Registration screen]

[Error message mapping]
    └──enhances──> [Login screen]
    └──enhances──> [Registration screen]

[Sign-out]
    └──requires──> [Login screen exists to return to]
    └──lives in──> [CalendarActivity or menu]
```

### Dependency Notes

- **Firebase project setup requires google-services.json:** This is a hard blocker. Nothing else works without this file in `app/`. Do this first.
- **Firestore upsert requires login success:** Never write to Firestore before `signInWithEmailAndPassword` Task completes with non-null result.
- **Auto-login check requires FirebaseAuth instance:** Synchronous; safe to call in `onCreate` before `setContentView` to avoid login screen flash.
- **Error code mapping is not optional:** Firebase throws specific codes: `ERROR_WRONG_PASSWORD`, `ERROR_USER_NOT_FOUND`, `ERROR_EMAIL_ALREADY_IN_USE`, `ERROR_NETWORK_REQUEST_FAILED`, `ERROR_TOO_MANY_REQUESTS`. Map all of these to user-facing Korean or English strings.

## MVP Definition

### Launch With (v1 — this capstone module)

- [ ] Firebase project configured, `google-services.json` in place — nothing works without this
- [ ] Login screen (email + password fields, login button) — core requirement
- [ ] Registration screen (email + password + optional name fields) — users need accounts to log into
- [ ] Client-side email format + password length validation — table stakes UX
- [ ] Loading indicator during auth call — prevents double-tap bugs and feels professional
- [ ] Specific error messages per FirebaseAuthException error code — graders will try wrong passwords
- [ ] Firestore upsert on login success (`users/{uid}` document) — explicit grader requirement per PROJECT.md
- [ ] Auto-login routing at app start (check `currentUser`, skip to calendar if signed in) — PROJECT.md Active requirement
- [ ] Navigate to CalendarActivity placeholder on login success — the end-to-end flow must close
- [ ] Sign-out from CalendarActivity — every auth module needs an exit

### Add After Validation (v1.x)

- [ ] Password visibility toggle — quick win, add once v1 is passing
- [ ] Email verification enforcement — meaningful security; add if time permits before demo
- [ ] Display name stored in Firestore — personalizes calendar screen; low effort once registration exists
- [ ] IME action (Next/Done) on input fields — polish pass, 30 minutes of work

### Future Consideration (v2+ / calendar milestone)

- [ ] Biometric unlock — requires `BiometricPrompt` API, separate UX design
- [ ] Password reset flow — Firebase supports it; defer until grader asks or user research demands it
- [ ] User profile edit screen — belongs in a profile/settings milestone, not this login module
- [ ] "Remember me" with explicit toggle — Firebase persists by default; only add if explicit logout-on-close is required

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Firebase setup + google-services.json | HIGH | LOW | P1 |
| Login screen UI | HIGH | LOW | P1 |
| Registration screen UI | HIGH | LOW | P1 |
| Email + password client validation | HIGH | LOW | P1 |
| Loading state during auth | HIGH | LOW | P1 |
| Error message mapping (FirebaseAuthException) | HIGH | MEDIUM | P1 |
| Firestore upsert on login | HIGH | MEDIUM | P1 |
| Auto-login routing (currentUser check) | HIGH | LOW | P1 |
| Navigate to CalendarActivity on success | HIGH | LOW | P1 |
| Sign-out | HIGH | LOW | P1 |
| Password visibility toggle | MEDIUM | LOW | P2 |
| Display name in Firestore | MEDIUM | LOW | P2 |
| IME Next/Done actions | MEDIUM | LOW | P2 |
| Email verification | MEDIUM | MEDIUM | P2 |
| Input shake animation | LOW | LOW | P3 |
| Biometric login | LOW | HIGH | P3 |
| Password reset flow | MEDIUM | MEDIUM | P3 |

**Priority key:**
- P1: Must have for capstone demo / grader requirements
- P2: Should have — add in polish pass before submission
- P3: Nice to have — only if all P1 and P2 are done and time remains

## Specific UX Patterns to Implement

### Email Validation
Use `android.util.Patterns.EMAIL_ADDRESS` for client-side check. Do NOT write a custom regex — the Android standard pattern is well-tested. Show error on the TextInputLayout with `setError()`, not a Toast.

### Error Handling UX
Map `FirebaseAuthException.errorCode` to strings. Key codes to handle:
- `ERROR_WRONG_PASSWORD` → "비밀번호가 올바르지 않습니다"
- `ERROR_USER_NOT_FOUND` → "등록되지 않은 이메일입니다"
- `ERROR_EMAIL_ALREADY_IN_USE` → "이미 사용 중인 이메일입니다" (registration)
- `ERROR_INVALID_EMAIL` → "이메일 형식이 올바르지 않습니다"
- `ERROR_NETWORK_REQUEST_FAILED` → "네트워크 연결을 확인해주세요"
- `ERROR_TOO_MANY_REQUESTS` → "잠시 후 다시 시도해주세요"
- Fallback → `exception.message ?: "알 수 없는 오류가 발생했습니다"`

Show errors on the relevant TextInputLayout field when field-specific, or as a Snackbar for general/network errors.

### Loading States
Disable the login/register button and show a ProgressBar (or swap button text to a spinner) when the auth Task is in flight. Re-enable on success or failure. This prevents duplicate requests from impatient taps.

### Session Persistence
Firebase Auth persists the token to local storage automatically — no extra code needed. The only implementation required is reading `FirebaseAuth.getInstance().currentUser` at app start (in `SplashActivity` or `MainActivity.onCreate` before rendering) and routing accordingly. If `currentUser != null`, go directly to `CalendarActivity`.

## Sources

- Firebase Auth Android SDK documentation (stable since 2016; patterns confirmed through training data up to August 2025) — HIGH confidence
- Android Material Components TextInputLayout + TextInputEditText patterns — HIGH confidence
- FirebaseAuthException error code enumeration — HIGH confidence (codes are stable across SDK versions)
- PROJECT.md requirements and Out of Scope decisions — authoritative for this project

---
*Feature research for: Android Kotlin Firebase Auth login module (capstone)*
*Researched: 2026-05-04*
