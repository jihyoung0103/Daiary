# Phase 2: Auth Core — Research

**Researched:** 2026-05-06
**Domain:** Firebase Auth (이메일/비밀번호) + Firestore upsert + Jetpack Navigation + ViewBinding + StateFlow
**Confidence:** HIGH

---

## Summary

Phase 2는 Phase 1에서 만들어진 스텁 파일 10개를 실제 Firebase 호출로 교체하는 단계다.
모든 레이어 경계, 클래스 구조, StateFlow 계약이 Phase 1에서 이미 확립되어 있으므로, Phase 2는 "설계"가 아닌 "채우기" 작업이다.

핵심 기술적 작업은 세 가지다:
1. `FirebaseAuthDataSource` / `FirestoreUserDataSource` — `throw NotImplementedError` 를 실제 `.await()` 호출로 교체
2. `AuthViewModel` — Firebase 에러 코드를 한국어 메시지로 매핑하는 로직 추가 (현재는 `e.message` 를 그대로 노출)
3. `LoginFragment` — 버튼 클릭 핸들러, ProgressBar 토글, errorTextView 표시, NavController 이동 주석 해제 및 완성

주의해야 할 것: `fragment_login.xml`에 회원가입 버튼이 없다. 02-03 플랜에서 레이아웃 수정이 필요하다.
또한 Firebase의 이메일 열거 보호(Email Enumeration Protection)가 활성화되면 `ERROR_WRONG_PASSWORD`, `ERROR_USER_NOT_FOUND`가 `INVALID_LOGIN_CREDENTIALS` 하나로 통합된다 — 에러 매핑 로직 설계에 반영해야 한다.

**Primary recommendation:** DataSource 스텁만 교체하면 Repository·ViewModel 로직은 이미 정상 동작한다. Fragment UI 연결과 에러 코드 한국어 매핑이 이 Phase의 실질적인 작업이다.

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AUTH-01 | 사용자가 이메일과 비밀번호로 로그인할 수 있다 | `FirebaseAuthDataSource.signIn()` 구현 — `auth.signInWithEmailAndPassword(email, pw).await()` |
| AUTH-02 | 사용자가 이메일과 비밀번호로 회원가입할 수 있고, 성공 시 Firestore `users/{uid}` 문서가 생성(upsert)된다 | `FirebaseAuthDataSource.signUp()` + `FirestoreUserDataSource.upsertUser()` 구현 |
| AUTH-03 | 사용자가 로그아웃할 수 있다 (Firebase Auth signOut 호출) | `auth.signOut()` — 이미 DataSource에 구현됨; CalendarFragment에 로그아웃 버튼 추가 필요 |
| UI-01 | Firebase 비동기 호출 중 로딩 인디케이터(ProgressBar)가 표시된다 | `AuthUiState.Loading` → `binding.progressBar.isVisible = true` + 버튼 비활성화 |
| UI-02 | FirebaseAuthException 에러 코드에 따라 구체적인 한국어 에러 메시지가 표시된다 | ViewModel의 에러 매핑 함수 + `binding.errorTextView` 표시 |
| UI-04 | 이메일 형식과 비밀번호 최소 길이(6자 이상)를 Firebase 호출 전 클라이언트에서 검사한다 | `android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()` + `password.length < 6` 검사 |
</phase_requirements>

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Firebase Auth 호출 (signIn/signUp/signOut) | Data Layer (DataSource) | — | 레이어 경계 원칙: Firebase SDK 호출은 data/source/만 담당 |
| Firestore upsert (users/{uid}) | Data Layer (DataSource) | — | 레이어 경계 원칙 동일 |
| 인증 흐름 조율 (Auth → upsert → Result 반환) | Data Layer (Repository) | — | DataSource를 조합하는 역할; 예외를 Result.Error로 래핑 |
| UI 상태 관리 (StateFlow<AuthUiState>) | ViewModel | — | viewModelScope에서 비동기 실행; UI와 비즈니스 로직 분리 |
| 에러 코드 → 한국어 메시지 매핑 | ViewModel | — | UI 표시 로직이지만 Firebase 예외 타입 접근 필요 — ViewModel이 적합 |
| 클라이언트 유효성 검사 (이메일 형식, 비밀번호 길이) | Fragment (UI Layer) | ViewModel에서도 가능 | Firebase 호출 전 즉각적 UI 피드백; Fragment에서 처리가 UX상 빠름 |
| ProgressBar 토글, 에러 메시지 표시 | Fragment (UI Layer) | — | View 조작은 Fragment 담당; ViewModel은 상태만 노출 |
| NavController.navigate() | Fragment (UI Layer) | — | Navigation Component 사용; Fragment에서 NavController 획득 |
| 로그아웃 버튼 (CalendarFragment) | Fragment (UI Layer) | ViewModel | 버튼 클릭 → ViewModel.signOut() 호출 |

---

## Standard Stack

### 실제 사용 버전 (Phase 1에서 다운그레이드됨)

| Component | Actual Version | Notes |
|-----------|---------------|-------|
| Kotlin | 1.9.0 | Phase 1에서 로컬 환경 호환을 위해 다운그레이드됨 |
| AGP | 8.1.0 | |
| Gradle | 8.1 | |
| compileSdk / targetSdk | 34 | |
| JVM toolchain | 20 | `jvmToolchain(20)` 설정됨 |
| Firebase BoM | 33.7.0 | |
| kotlinx-coroutines | 1.9.0 | `kotlinx-coroutines-android` + `kotlinx-coroutines-play-services` |
| lifecycle | 2.10.0 | `lifecycle-viewmodel-ktx`, `lifecycle-runtime-ktx` |
| navigation | 2.9.8 | `navigation-fragment-ktx` |

> [VERIFIED: 프로젝트 `gradle/libs.versions.toml` 직접 확인]

### 핵심 의존성 — Phase 2에 필요한 것들

| Library | Purpose | Import |
|---------|---------|--------|
| `kotlinx-coroutines-play-services` | `Task.await()` 확장함수 제공 | `import kotlinx.coroutines.tasks.await` |
| `firebase-auth-ktx` | `FirebaseAuth`, `FirebaseAuthException` 서브클래스들 | BoM 관리 |
| `firebase-firestore-ktx` | `FirebaseFirestore`, `SetOptions`, `FieldValue` | BoM 관리 |
| `lifecycle-runtime-ktx` | `repeatOnLifecycle(STARTED)` | 이미 의존성 선언됨 |
| `navigation-fragment-ktx` | `findNavController()` | 이미 의존성 선언됨 |

모든 의존성이 이미 `app/build.gradle.kts`에 선언되어 있다. Phase 2에서 새로운 의존성 추가 불필요.
[VERIFIED: `app/build.gradle.kts` 직접 확인]

---

## Architecture Patterns

### 기존 레이어 구조 (변경 없음)

```
app/src/main/kotlin/com/example/capstone_login/
├── model/
│   └── User.kt                     # 도메인 모델 (Firebase 의존성 없음)
├── util/
│   └── Result.kt                   # sealed Result<T>: Success, Error, Loading
├── data/
│   └── source/
│       ├── FirebaseAuthDataSource.kt   # ← Phase 2 핵심: signIn/signUp 구현
│       └── FirestoreUserDataSource.kt  # ← Phase 2 핵심: upsertUser 구현
│   └── repository/
│       └── AuthRepository.kt           # 이미 완성 (스텁 교체 불필요)
└── ui/
    ├── auth/
    │   ├── AuthUiState.kt              # 이미 완성
    │   ├── AuthViewModel.kt            # ← Phase 2: 에러 매핑 함수 추가
    │   └── LoginFragment.kt            # ← Phase 2 핵심: UI 연결 전체
    ├── calendar/
    │   └── CalendarFragment.kt         # ← Phase 2: 로그아웃 버튼 추가
    └── MainActivity.kt                 # 변경 없음 (Phase 3 담당)
```

### 데이터 흐름 다이어그램

```
[사용자 입력]
    │
    ▼
[LoginFragment]
  클라이언트 검증 (이메일 형식, 비밀번호 길이)
    │ 검증 통과
    ▼
[AuthViewModel.login() / .register()]
  viewModelScope.launch { }
  _uiState.value = AuthUiState.Loading
    │
    ▼
[AuthRepository.signIn() / .signUp()]
  try/catch → Result.Success / Result.Error
    │
    ├──▶ [FirebaseAuthDataSource.signIn() / .signUp()]
    │         auth.signInWithEmailAndPassword(email, pw).await()
    │         auth.createUserWithEmailAndPassword(email, pw).await()
    │         ──▶ Firebase Auth 서버 (네트워크)
    │
    └──▶ [FirestoreUserDataSource.upsertUser()]  (auth 성공 후)
              db.collection("users").document(uid).set(data, SetOptions.merge()).await()
              ──▶ Firestore 서버 (네트워크)
              runCatching { } → non-fatal (실패해도 Result.Success 반환)
    │
    ▼
[AuthViewModel]
  Result.Success → AuthUiState.Success(user)
  Result.Error   → AuthUiState.Error(mapErrorMessage(e))
    │
    ▼ StateFlow 방출
[LoginFragment — repeatOnLifecycle(STARTED) collect]
  Loading  → progressBar VISIBLE, loginButton disabled
  Success  → NavController.navigate(action_loginFragment_to_calendarFragment)
  Error    → errorTextView VISIBLE + 한국어 메시지
  Idle     → 초기 상태
```

### Pattern 1: FirebaseAuthDataSource.signIn() / signUp() 구현

**What:** `throw NotImplementedError` 제거, `.await()` 호출로 교체
**Import 주의:** `kotlinx.coroutines.tasks.await` — `kotlinx-coroutines-play-services` 라이브러리 제공

```kotlin
// Source: 기존 스텁 주석 + kotlinx-coroutines-play-services 공식 문서
// import kotlinx.coroutines.tasks.await  ← 이미 파일 상단에 선언되어 있음

suspend fun signIn(email: String, password: String): FirebaseUser {
    return auth.signInWithEmailAndPassword(email, password).await().user
        ?: throw IllegalStateException("Auth succeeded but user is null")
}

suspend fun signUp(email: String, password: String): FirebaseUser {
    return auth.createUserWithEmailAndPassword(email, password).await().user
        ?: throw IllegalStateException("Auth succeeded but user is null")
}
```

[VERIFIED: 기존 스텁 파일에 이미 `.await()` 패턴이 주석으로 존재함 — `FirebaseAuthDataSource.kt` 직접 확인]
[CITED: kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-play-services/]

### Pattern 2: FirestoreUserDataSource.upsertUser() 구현

**What:** `throw NotImplementedError` 제거, Firestore `set(merge=true)` 호출로 교체
**핵심:** `SetOptions.merge()` 사용 — 기존 필드 덮어쓰지 않음 (재로그인 안전)

```kotlin
// Source: 기존 스텁 주석 — FirestoreUserDataSource.kt
// import com.google.firebase.firestore.FieldValue  ← 추가 필요
// import com.google.firebase.firestore.SetOptions   ← 이미 임포트됨

suspend fun upsertUser(uid: String, email: String) {
    val data = mapOf(
        "email" to email,
        "lastLoginAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )
    db.collection("users").document(uid).set(data, SetOptions.merge()).await()
}
```

[VERIFIED: 기존 스텁 파일에 이미 패턴이 주석으로 존재함 — `FirestoreUserDataSource.kt` 직접 확인]
[CITED: firebase.google.com/docs/firestore/manage-data/add-data#update-data]

### Pattern 3: FirebaseAuthException 에러 코드 매핑 (중요 — 최신 SDK 변경사항 포함)

**Firebase Email Enumeration Protection 영향:**

Firebase는 2023년 9월부터 신규 프로젝트에 이메일 열거 보호를 기본 활성화했다.
보호가 활성화되면 `ERROR_WRONG_PASSWORD` / `ERROR_USER_NOT_FOUND`가 더 이상 별도로 반환되지 않고
`FirebaseAuthInvalidCredentialsException`의 `INVALID_LOGIN_CREDENTIALS` 코드로 통합된다.

[CITED: docs.cloud.google.com/identity-platform/docs/admin/email-enumeration-protection]

**에러 코드 분류:**

| 예외 타입 | `getErrorCode()` 반환값 | 상황 | 한국어 메시지 |
|----------|------------------------|------|--------------|
| `FirebaseAuthInvalidCredentialsException` | `ERROR_INVALID_EMAIL` | 이메일 형식 불량 | "이메일 형식이 올바르지 않습니다." |
| `FirebaseAuthInvalidCredentialsException` | `ERROR_WRONG_PASSWORD` | 잘못된 비밀번호 (보호 비활성화 시) | "비밀번호가 틀렸습니다." |
| `FirebaseAuthInvalidCredentialsException` | `INVALID_LOGIN_CREDENTIALS` | 잘못된 자격증명 (보호 활성화 시, 통합 코드) | "이메일 또는 비밀번호가 올바르지 않습니다." |
| `FirebaseAuthInvalidUserException` | `ERROR_USER_NOT_FOUND` | 존재하지 않는 계정 (보호 비활성화 시) | "등록되지 않은 이메일입니다." |
| `FirebaseAuthUserCollisionException` | `ERROR_EMAIL_ALREADY_IN_USE` | 이미 사용 중인 이메일 (회원가입 시) | "이미 사용 중인 이메일입니다." |
| `FirebaseAuthWeakPasswordException` | `ERROR_WEAK_PASSWORD` | 6자 미만 비밀번호 | "비밀번호는 6자 이상이어야 합니다." |
| `FirebaseNetworkException` | `ERROR_NETWORK_REQUEST_FAILED` | 네트워크 오류 | "네트워크 오류가 발생했습니다. 연결을 확인하세요." |
| 기타 `FirebaseAuthException` | `ERROR_TOO_MANY_REQUESTS` | 과도한 요청 | "요청이 너무 많습니다. 잠시 후 다시 시도하세요." |

[ASSUMED: `ERROR_WRONG_PASSWORD`, `ERROR_USER_NOT_FOUND` 등 구체적 에러 코드 문자열은 Firebase 공식 문서에서 직접 확인 불가 (참조 페이지 접근 실패). 이 코드들은 Firebase Android SDK에서 수년간 안정적으로 사용된 값이지만, 최신 SDK에서 email enumeration protection에 의해 `INVALID_LOGIN_CREDENTIALS`로 통합될 수 있음을 감안해야 함]

**권장 구현 패턴 (이메일 열거 보호 대응):**

```kotlin
// ViewModel에서 에러 매핑 함수
private fun mapAuthError(e: Exception): String {
    return when {
        e is FirebaseAuthInvalidCredentialsException -> when (e.errorCode) {
            "ERROR_INVALID_EMAIL" -> "이메일 형식이 올바르지 않습니다."
            "ERROR_WRONG_PASSWORD" -> "비밀번호가 틀렸습니다."
            else -> "이메일 또는 비밀번호가 올바르지 않습니다." // INVALID_LOGIN_CREDENTIALS 포함
        }
        e is FirebaseAuthInvalidUserException -> "등록되지 않은 이메일입니다."
        e is FirebaseAuthUserCollisionException -> "이미 사용 중인 이메일입니다."
        e is FirebaseAuthWeakPasswordException -> "비밀번호는 6자 이상이어야 합니다."
        e.message?.contains("NETWORK_REQUEST_FAILED", ignoreCase = true) == true ->
            "네트워크 오류가 발생했습니다. 연결을 확인하세요."
        e is FirebaseAuthException && e.errorCode == "ERROR_TOO_MANY_REQUESTS" ->
            "요청이 너무 많습니다. 잠시 후 다시 시도하세요."
        else -> "로그인에 실패했습니다. 다시 시도하세요."
    }
}
```

> **주의:** 현재 `AuthViewModel`은 `e.message ?: "로그인 실패"` 로 날 것의 에러 메시지를 노출한다.
> Phase 2에서 반드시 위와 같은 매핑 함수로 교체해야 UI-02 요구사항이 충족된다.

### Pattern 4: LoginFragment 버튼 클릭 + 클라이언트 유효성 검사

**ViewBinding 필드 접근:**
- `binding.emailEditText.text?.toString()?.trim().orEmpty()`
- `binding.passwordEditText.text?.toString().orEmpty()`

```kotlin
// 클라이언트 검증 (UI-04)
private fun validateInput(email: String, password: String): String? {
    if (email.isBlank()) return "이메일을 입력하세요."
    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
        return "이메일 형식이 올바르지 않습니다."
    }
    if (password.length < 6) return "비밀번호는 6자 이상이어야 합니다."
    return null // 검증 통과
}

// 버튼 클릭 (loginButton)
binding.loginButton.setOnClickListener {
    val email = binding.emailEditText.text?.toString()?.trim().orEmpty()
    val password = binding.passwordEditText.text?.toString().orEmpty()
    val error = validateInput(email, password)
    if (error != null) {
        binding.errorTextView.text = error
        binding.errorTextView.isVisible = true
        return@setOnClickListener
    }
    viewModel.login(email, password)
}

// 회원가입 버튼 클릭 (registerButton — fragment_login.xml에 추가 필요)
binding.registerButton.setOnClickListener {
    val email = binding.emailEditText.text?.toString()?.trim().orEmpty()
    val password = binding.passwordEditText.text?.toString().orEmpty()
    val error = validateInput(email, password)
    if (error != null) {
        binding.errorTextView.text = error
        binding.errorTextView.isVisible = true
        return@setOnClickListener
    }
    viewModel.register(email, password)
}
```

[VERIFIED: `fragment_login.xml` 직접 확인 — `emailEditText`, `passwordEditText`, `loginButton`, `progressBar`, `errorTextView` ID 존재]

### Pattern 5: StateFlow collect + ProgressBar 토글 + Navigation

```kotlin
// Fragment의 onViewCreated
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
            when (state) {
                is AuthUiState.Loading -> {
                    binding.progressBar.isVisible = true
                    binding.loginButton.isEnabled = false
                    binding.registerButton.isEnabled = false  // 회원가입 버튼도 비활성화
                    binding.errorTextView.isVisible = false
                }
                is AuthUiState.Success -> {
                    binding.progressBar.isVisible = false
                    // Phase 2에서는 단순 navigate (popUpTo는 Phase 3에서)
                    findNavController().navigate(
                        R.id.action_loginFragment_to_calendarFragment
                    )
                }
                is AuthUiState.Error -> {
                    binding.progressBar.isVisible = false
                    binding.loginButton.isEnabled = true
                    binding.registerButton.isEnabled = true
                    binding.errorTextView.text = state.message
                    binding.errorTextView.isVisible = true
                }
                is AuthUiState.Idle -> {
                    binding.progressBar.isVisible = false
                    binding.loginButton.isEnabled = true
                    binding.registerButton.isEnabled = true
                    binding.errorTextView.isVisible = false
                }
            }
        }
    }
}
```

[VERIFIED: 기존 `LoginFragment.kt` 스텁에 이미 `repeatOnLifecycle(STARTED)` 패턴 설정되어 있음]
[VERIFIED: `nav_graph.xml`에 `action_loginFragment_to_calendarFragment` 액션 ID 존재 확인]

> **Phase 3 분리 경계:** `popUpTo(loginFragment, inclusive=true)` 는 Phase 3 담당이다.
> Phase 2에서는 단순 `navigate(actionId)` 만 구현하면 된다.

### Pattern 6: CalendarFragment 로그아웃 버튼 추가

CalendarFragment에는 현재 로그아웃 버튼이 없다. `fragment_calendar.xml`도 플레이스홀더만 있다.
AUTH-03 충족을 위해:
1. `fragment_calendar.xml`에 `logoutButton` 추가
2. `CalendarFragment.kt`에 ViewModel 연결 + 버튼 클릭 핸들러 추가

```kotlin
// CalendarFragment에서
private val viewModel: AuthViewModel by viewModels()

// onViewCreated에서
binding.logoutButton.setOnClickListener {
    viewModel.signOut()
    findNavController().navigate(R.id.loginFragment)
    // Phase 3에서 popUpTo 적용
}
```

### Anti-Patterns to Avoid

- **ViewModel에서 직접 FirebaseAuth 호출:** DataSource 레이어 경계 위반 — Repository를 통해야 함
- **Fragment에서 FirebaseAuthException 직접 catch:** ViewModel/Repository가 담당
- **`lifecycleScope` 대신 `viewModelScope` 없이 Firebase Task 실행:** 화면 회전 시 누수 발생
- **`auth.currentUser`를 동기적으로 확인:** Phase 3(자동 로그인)에서 처리할 문제 — Phase 2에서 건드리지 말 것
- **Success 상태에서 중복 navigate 호출:** StateFlow는 재방출 방지 안 함 — isNavigated 플래그 또는 `resetState()` 호출로 방지

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Firebase Task → suspend 변환 | 커스텀 suspendCoroutine 래퍼 | `kotlinx-coroutines-play-services` `.await()` | 취소 처리, 예외 전파가 정확히 구현됨 |
| Firestore upsert (document 없을 때도 안전) | `get()` 후 `set()`/`update()` 분기 | `set(data, SetOptions.merge())` | 원자적 upsert, 경쟁 조건 없음 |
| 이메일 형식 검증 정규식 | 직접 작성한 정규식 | `android.util.Patterns.EMAIL_ADDRESS` | Android 플랫폼 내장, RFC 표준 준수 |
| StateFlow 수명주기 안전 수집 | 수동 Job 관리 | `repeatOnLifecycle(STARTED)` | Fragment 생명주기에 정확히 동기화됨 |
| NavController 획득 | 직접 Activity cast | `findNavController()` (fragment-ktx) | Fragment 내에서 안전한 NavController 접근 |

---

## 레이아웃 갭 (Layout Gap)

**발견된 문제: `fragment_login.xml`에 회원가입 버튼 없음**

현재 `fragment_login.xml`에는 `loginButton`만 있고 `registerButton`이 없다.
AUTH-02(회원가입) 구현을 위해 02-03 플랜에서 레이아웃 수정이 필요하다.

추가해야 할 View:
- `android:id="@+id/registerButton"` — `loginButton` 아래에 배치

`strings.xml`에는 이미 `<string name="button_register">회원가입</string>` 이 존재한다.

[VERIFIED: `fragment_login.xml` 직접 확인 — 회원가입 버튼 ID 없음]
[VERIFIED: `strings.xml` 직접 확인 — `button_register` 문자열 존재]

---

## Common Pitfalls

### Pitfall 1: FirebaseAuthException 에러 코드 — 이메일 열거 보호 영향
**What goes wrong:** `ERROR_WRONG_PASSWORD` / `ERROR_USER_NOT_FOUND` when/else 분기를 작성했는데 실제로는 `INVALID_LOGIN_CREDENTIALS` 코드가 와서 "로그인에 실패했습니다. 다시 시도하세요." 같은 기본 메시지만 표시됨.
**Why it happens:** 2023년 9월 이후 신규 Firebase 프로젝트는 이메일 열거 보호가 기본 활성화됨.
**How to avoid:** `when` 분기의 `else` 절에 "이메일 또는 비밀번호가 올바르지 않습니다." 처리를 포함하고, `FirebaseAuthInvalidCredentialsException` 타입 체크를 `getErrorCode()` 문자열 비교보다 우선시한다.
**Warning signs:** 로그인 실패 시 항상 "알 수 없는 오류" 표시됨.

### Pitfall 2: Success 상태에서 중복 navigate 호출
**What goes wrong:** 화면 회전 시 `AuthUiState.Success` 가 재방출되어 navigate가 두 번 호출되고 "Navigation destination already on back stack" 크래시 발생.
**Why it happens:** `StateFlow`는 마지막 값을 보유하므로 `repeatOnLifecycle(STARTED)` 가 재시작될 때 마지막 Success 상태를 즉시 방출함.
**How to avoid:** navigate 직후 `viewModel.resetState()` 호출하여 `AuthUiState.Idle`로 복귀. 또는 `is AuthUiState.Success` 처리에서 `resetState()`를 navigate 전에 호출.
**Warning signs:** 화면 회전 후 "IllegalArgumentException: navigation destination ... is unknown" 크래시.

### Pitfall 3: `binding.loginButton.isEnabled = false` 누락
**What goes wrong:** 로딩 중 버튼이 활성화되어 있어 중복 클릭 시 Firebase Auth 요청이 여러 번 발생. 특히 회원가입의 경우 중복 계정 생성 시도 오류로 이어짐.
**How to avoid:** `AuthUiState.Loading` 에서 모든 액션 버튼을 비활성화, `Idle`/`Error` 상태에서 다시 활성화.

### Pitfall 4: `FieldValue.serverTimestamp()` import 누락
**What goes wrong:** `FirestoreUserDataSource.upsertUser()`에서 `FieldValue.serverTimestamp()` 사용 시 import 문이 없으면 컴파일 에러.
**How to avoid:** `import com.google.firebase.firestore.FieldValue` 추가. 또는 `com.google.firebase.firestore.FieldValue.serverTimestamp()` 완전 경로 사용.

### Pitfall 5: fragment_login.xml 미수정으로 회원가입 불가
**What goes wrong:** `AuthViewModel.register()`, `AuthRepository.signUp()`, `FirebaseAuthDataSource.signUp()`을 모두 구현했지만 LoginFragment에서 호출할 버튼이 없어 AUTH-02 미충족.
**How to avoid:** 02-03 플랜에서 레이아웃 수정을 첫 번째 태스크로 배치.

---

## Code Examples

### 1. .await() 임포트 확인

```kotlin
// FirebaseAuthDataSource.kt 상단에 이미 존재 (확인됨)
import kotlinx.coroutines.tasks.await
```

[VERIFIED: `FirebaseAuthDataSource.kt` 직접 확인]

### 2. Firestore FieldValue.serverTimestamp() 사용

```kotlin
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions

val data = mapOf(
    "email" to email,
    "lastLoginAt" to FieldValue.serverTimestamp()
)
db.collection("users").document(uid).set(data, SetOptions.merge()).await()
```

### 3. ProgressBar isVisible (Core KTX 확장)

```kotlin
import androidx.core.view.isVisible

binding.progressBar.isVisible = true   // VISIBLE
binding.progressBar.isVisible = false  // GONE
```

`android:visibility="gone"` 로 XML에 설정되어 있으므로 `isVisible = true/false` 만으로 충분하다.

### 4. NavController navigate (Phase 2 — popUpTo 없이)

```kotlin
findNavController().navigate(R.id.action_loginFragment_to_calendarFragment)
```

`nav_graph.xml`에 해당 액션 ID가 정의되어 있음.
[VERIFIED: `nav_graph.xml` 직접 확인]

### 5. android.util.Patterns.EMAIL_ADDRESS

```kotlin
import android.util.Patterns

if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
    // 이메일 형식 오류
}
```

### 6. viewModel.resetState() 후 navigate

```kotlin
is AuthUiState.Success -> {
    viewModel.resetState()   // Idle로 리셋 → 재방출 방지
    findNavController().navigate(R.id.action_loginFragment_to_calendarFragment)
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `addOnSuccessListener` / `addOnFailureListener` 콜백 | `task.await()` + try/catch in suspend function | 2019 (coroutines-play-services 출시) | 콜백 지옥 제거, viewModelScope 취소 자동 적용 |
| `LiveData<UiState>` | `StateFlow<UiState>` + `repeatOnLifecycle` | 2021 (Google 공식 권장 변경) | 코루틴 네이티브, Observer 보일러플레이트 제거 |
| `findViewByID()` | `ViewBinding` | 2019 | Null-safe, 컴파일 타임 ID 검증 |
| `ERROR_WRONG_PASSWORD` 별도 처리 | `INVALID_LOGIN_CREDENTIALS` 통합 처리 | 2023년 9월 (이메일 열거 보호 기본 활성화) | 보안 강화, 에러 메시지 구분 불가 |

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `ERROR_WRONG_PASSWORD`, `ERROR_USER_NOT_FOUND`, `ERROR_EMAIL_ALREADY_IN_USE`, `ERROR_WEAK_PASSWORD`, `ERROR_TOO_MANY_REQUESTS` 문자열 상수가 Firebase Android SDK에서 `getErrorCode()` 반환값으로 사용됨 | Pattern 3 에러 코드 매핑 표 | 해당 코드가 일치하지 않으면 모든 에러가 fallback 메시지로 표시됨 (기능은 작동, UX만 저하) |
| A2 | 현재 Firebase 프로젝트에 이메일 열거 보호가 활성화되어 있음 | Pattern 3, Pitfall 1 | 비활성화 상태라면 `INVALID_LOGIN_CREDENTIALS` 대신 `ERROR_WRONG_PASSWORD` 등이 반환되어 에러 메시지가 더 구체적으로 표시됨 (더 좋은 UX) |
| A3 | `androidx.core.view.isVisible` 확장 (`core-ktx` 제공)이 `isVisible = true/false`로 View.VISIBLE / View.GONE 전환을 처리함 | Code Examples #3 | `core-ktx` 의존성이 없으면 `binding.progressBar.visibility = View.VISIBLE` 사용 필요 |

---

## Open Questions (RESOLVED)

1. **이메일 열거 보호 활성화 여부 확인 필요**
   - What we know: 2023년 9월 이후 신규 프로젝트 기본 활성화
   - What's unclear: 현재 프로젝트가 이 시기 이전/이후 생성인지, Firebase Console에서 변경했는지
   - Recommendation: 플랜에서 에러 매핑을 `FirebaseAuthInvalidCredentialsException` 타입 체크 우선으로 구현하여 양쪽 모두 대응
   - **RESOLVED (02-02-PLAN.md):** `mapAuthError()`를 `FirebaseAuthInvalidCredentialsException` 타입 체크 우선으로 구현하고 `else` 절에 "이메일 또는 비밀번호가 올바르지 않습니다." 처리를 포함 → 보호 활성화 여부 무관하게 대응

2. **회원가입 버튼 UI 배치 방식**
   - What we know: `fragment_login.xml`에 현재 `loginButton`만 있음
   - What's unclear: 로그인/회원가입을 같은 화면에서 할지, 탭 또는 별도 버튼으로 전환할지
   - Recommendation: 단순성을 위해 `loginButton` 아래에 `registerButton` 추가 — 같은 이메일/비밀번호 입력 필드 공유
   - **RESOLVED (02-03-PLAN.md Task 1):** `loginButton` 아래에 `registerButton` (MaterialButton) 추가, 같은 emailEditText/passwordEditText 공유

3. **CalendarFragment 로그아웃 후 navigate 대상**
   - What we know: `nav_graph.xml`에 `loginFragment` (startDestination)과 `calendarFragment`가 정의됨
   - What's unclear: 로그아웃 후 `R.id.loginFragment`로 직접 navigate할 수 있는지, 또는 back stack pop이 필요한지
   - Recommendation: Phase 2에서는 `findNavController().navigate(R.id.loginFragment)` 사용; 전체 back stack 정리는 Phase 3 담당
   - **RESOLVED (02-03-PLAN.md Task 3):** `action_calendarFragment_to_loginFragment` 액션을 nav_graph.xml에 추가하고 `findNavController().navigate(R.id.action_calendarFragment_to_loginFragment)` 사용; back stack 정리(popUpTo inclusive)는 Phase 3 담당

---

## Environment Availability

이 Phase는 코드 변경만 포함하며 새로운 외부 도구 의존성 없음. 모든 필요한 SDK는 Phase 1에서 이미 설치 및 Gradle 의존성 선언됨.

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Firebase Auth SDK | AUTH-01, AUTH-02, AUTH-03 | ✓ | BoM 33.7.0 | — |
| Firebase Firestore SDK | AUTH-02 | ✓ | BoM 33.7.0 | — |
| kotlinx-coroutines-play-services | `.await()` | ✓ | 1.9.0 | — |
| google-services.json | Firebase 초기화 | ✓ | (Phase 1에서 배치 완료) | — |

[VERIFIED: `app/build.gradle.kts` 및 `STATE.md` 직접 확인]

---

## Validation Architecture

`config.json`에서 `nyquist_validation: true` 확인됨.

### Test Framework

| Property | Value |
|----------|-------|
| Framework | 없음 — Phase 1/2에서 테스트 인프라 설정되지 않음 |
| Config file | 없음 |
| Quick run command | 해당 없음 |
| Full suite command | `./gradlew assembleDebug` (빌드 검증) |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Notes |
|--------|----------|-----------|-------|
| AUTH-01 | 이메일/비밀번호 로그인 성공 → CalendarFragment 이동 | E2E (수동) | Firebase Emulator 없이 자동화 어려움 |
| AUTH-02 | 회원가입 성공 → Firestore users/{uid} 문서 생성 | E2E (수동) | Firebase Console에서 확인 |
| AUTH-03 | CalendarFragment 로그아웃 버튼 동작 | 수동 | 버튼 탭 → LoginFragment 이동 확인 |
| UI-01 | Firebase 호출 중 ProgressBar 표시 | 수동 | 로그인 시도 중 로딩 UI 관찰 |
| UI-02 | FirebaseAuthException → 한국어 에러 메시지 | 수동 | 잘못된 비밀번호 입력 후 에러 문구 확인 |
| UI-04 | 이메일 형식/비밀번호 길이 클라이언트 검증 | 단위 테스트 가능 | 검증 로직을 별도 함수로 분리하면 JVM 단위 테스트 가능 |

### Wave 0 Gaps

단위 테스트 프레임워크가 설정되어 있지 않다. 이 Phase의 검증은 에뮬레이터/기기에서의 수동 테스트로 대체한다.

- 빌드 검증: `./gradlew assembleDebug` 성공 확인
- 수동 검증 항목:
  - [ ] 이메일/비밀번호 로그인 성공 → CalendarFragment 이동
  - [ ] 잘못된 비밀번호 → 한국어 에러 메시지 표시
  - [ ] 6자 미만 비밀번호 → Firebase 호출 전 클라이언트에서 차단
  - [ ] 로그인 버튼 중복 클릭 시 ProgressBar 표시 + 버튼 비활성화
  - [ ] 회원가입 → Firestore Console에서 users/{uid} 문서 확인

---

## Security Domain

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | Firebase Auth — SDK가 토큰 관리 전담; 클라이언트는 자격증명을 저장하지 않음 |
| V3 Session Management | yes | Firebase Auth 세션 자동 관리; `auth.signOut()` 으로 세션 종료 |
| V4 Access Control | yes | Firestore Security Rules: `allow write: if request.auth.uid == userId` |
| V5 Input Validation | yes | 클라이언트: `Patterns.EMAIL_ADDRESS`, `password.length >= 6` |
| V6 Cryptography | no | Firebase SDK 내부 처리 — 직접 구현 없음 |

### Known Threat Patterns

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| 비밀번호 평문 로그 | Information Disclosure | `FirebaseUser`, 자격증명을 절대 Logcat에 출력하지 않음 |
| 중복 로그인 요청 (버튼 중복 클릭) | Denial of Service | `AuthUiState.Loading` 시 버튼 비활성화 |
| Firestore 무단 쓰기 | Tampering | Security Rules: `request.auth.uid == userId` |
| 이메일 열거 공격 | Information Disclosure | Firebase Email Enumeration Protection (기본 활성화) |

---

## Sources

### Primary (HIGH confidence)
- 프로젝트 실제 파일 직접 확인 (`FirebaseAuthDataSource.kt`, `FirestoreUserDataSource.kt`, `AuthRepository.kt`, `AuthViewModel.kt`, `LoginFragment.kt`, `CalendarFragment.kt`, `fragment_login.xml`, `nav_graph.xml`, `strings.xml`, `libs.versions.toml`, `app/build.gradle.kts`)
- `.planning/phases/01-*/SUMMARY.md` — Phase 1 아키텍처 결정 기록

### Secondary (MEDIUM confidence)
- [docs.cloud.google.com — Email Enumeration Protection](https://docs.cloud.google.com/identity-platform/docs/admin/email-enumeration-protection) — `INVALID_LOGIN_CREDENTIALS` 통합 동작 확인
- [firebase.google.com/docs/auth/android/password-auth](https://firebase.google.com/docs/auth/android/password-auth) — 공식 Android Auth 문서 (콜백 패턴만 제공, await 패턴 없음)
- [kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-play-services](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-play-services/) — `.await()` 확장함수 공식 문서

### Tertiary (LOW confidence)
- WebSearch 결과: `FirebaseAuthInvalidCredentialsException`, `FirebaseAuthInvalidUserException`, `FirebaseAuthUserCollisionException`, `FirebaseAuthWeakPasswordException` 서브클래스 분류 — 공식 참조 페이지 직접 확인 실패; 안정적인 패턴으로 수년간 문서화되어 있으나 `[ASSUMED]` 태그 적용

---

## Metadata

**Confidence breakdown:**
- Standard Stack: HIGH — 프로젝트 파일에서 직접 확인된 버전들
- Architecture: HIGH — Phase 1 스텁 파일 구조 직접 확인
- Firebase .await() 패턴: HIGH — 스텁 파일에 이미 주석으로 존재, kotlinx.coroutines 공식 문서
- 에러 코드 매핑: MEDIUM — 에러 코드 문자열 `[ASSUMED]`, 이메일 열거 보호 동작은 공식 문서로 확인
- 레이아웃 갭 (registerButton 없음): HIGH — fragment_login.xml 직접 확인

**Research date:** 2026-05-06
**Valid until:** 2026-06-06 (Firebase SDK 변경 없는 한 안정적)
