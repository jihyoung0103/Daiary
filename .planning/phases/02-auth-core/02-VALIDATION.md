---
phase: 2
phase-slug: auth-core
date: 2026-05-06
---

# Phase 2: Auth Core — Validation Strategy

## Test Framework

| Property | Value |
|----------|-------|
| Framework | 없음 — 단위 테스트 인프라 미설정 (Wave 0 Gap) |
| Build verification | `./gradlew assembleDebug` |
| Full E2E | 에뮬레이터 수동 검증 |

테스트 프레임워크가 없으므로 이 Phase의 검증은 빌드 성공 + 에뮬레이터 수동 시나리오로 구성된다.

---

## Validation Matrix

| Req ID | Behavior | Validation Type | Pass Condition |
|--------|----------|-----------------|----------------|
| AUTH-01 | 이메일/비밀번호 로그인 성공 → CalendarFragment 이동 | E2E (수동) | 로그인 버튼 탭 후 CalendarFragment 화면 표시됨 |
| AUTH-02 | 회원가입 성공 → Firestore `users/{uid}` 문서 생성 | E2E (수동) | Firebase Console → Firestore → users 컬렉션에 uid 문서 존재 확인 |
| AUTH-03 | CalendarFragment 로그아웃 버튼 → LoginFragment 이동 | 수동 | 로그아웃 버튼 탭 후 LoginFragment 화면 표시됨 |
| UI-01 | Firebase 비동기 호출 중 ProgressBar 표시 | 수동 | 로그인/회원가입 버튼 탭 후 ProgressBar 표시 + 버튼 비활성화 관찰 |
| UI-02 | FirebaseAuthException → 한국어 에러 메시지 | 수동 | 잘못된 비밀번호 입력 → 한국어 에러 문구 표시됨 |
| UI-04 | 이메일 형식/비밀번호 길이 클라이언트 검증 | 수동 | 형식 오류 이메일 또는 5자 비밀번호 → Firebase 호출 없이 에러 표시됨 |

---

## Build Verification (자동)

```bash
./gradlew assembleDebug
```

**Pass condition:** `BUILD SUCCESSFUL` — 컴파일 에러, 미해결 참조 없음

**Fail signals:**
- `Unresolved reference: FragmentLoginBinding` → fragment_login.xml ViewBinding ID 누락
- `Unresolved reference: await` → `kotlinx.coroutines.tasks.await` import 누락
- `Unresolved reference: action_loginFragment_to_calendarFragment` → nav_graph.xml 액션 ID 누락

---

## Manual Verification Scenarios

### Scenario 1: 로그인 성공 흐름 (AUTH-01, UI-01)
1. 에뮬레이터 앱 실행 → LoginFragment 표시 확인
2. 유효한 이메일/비밀번호 입력 → 로그인 버튼 탭
3. **Pass:** ProgressBar 표시 → CalendarFragment로 이동 → back 버튼 누르면 로그인 화면으로 돌아옴 (Phase 3에서 popUpTo 적용 전)

### Scenario 2: 회원가입 + Firestore upsert (AUTH-02)
1. 새 이메일/비밀번호 입력 → 회원가입 버튼 탭
2. **Pass:** CalendarFragment로 이동
3. Firebase Console → Firestore Database → `users/{uid}` 문서에 `email`, `lastLoginAt` 필드 존재 확인

### Scenario 3: 로그아웃 (AUTH-03)
1. 로그인 완료 후 CalendarFragment에서 로그아웃 버튼 탭
2. **Pass:** LoginFragment로 이동

### Scenario 4: 에러 메시지 — 잘못된 비밀번호 (UI-02)
1. 등록된 이메일 + 틀린 비밀번호 입력 → 로그인 버튼 탭
2. **Pass:** errorTextView에 "비밀번호가 틀렸습니다." 또는 "이메일 또는 비밀번호가 올바르지 않습니다." 표시 (이메일 열거 보호 활성화 여부에 따라 다름)

### Scenario 5: 클라이언트 검증 (UI-04)
1a. 형식이 잘못된 이메일 (예: "notanemail") + 유효한 비밀번호 입력 → 로그인 버튼 탭
   - **Pass:** "이메일 형식이 올바르지 않습니다." 표시, Firebase 네트워크 호출 없음
1b. 유효한 이메일 + 5자 비밀번호 (예: "abc12") → 로그인 버튼 탭
   - **Pass:** "비밀번호는 6자 이상이어야 합니다." 표시, Firebase 네트워크 호출 없음

---

## Wave 0 Gaps

| Gap | Impact | Mitigation |
|-----|--------|------------|
| 단위 테스트 프레임워크 없음 | `validateInput()`, `mapAuthError()` 함수가 자동화 검증 불가 | 수동 Scenario 4, 5로 대체; 함수를 별도로 분리하면 향후 JUnit 테스트 추가 가능 |
| Firebase Emulator 미설정 | 자동화 E2E 테스트 불가 | 실제 Firebase 프로젝트 (test mode) 에서 수동 검증 |

---

## Phase 2 Acceptance Gate

Phase 2 완료 선언 전 아래 항목이 모두 충족되어야 한다:

- [ ] `./gradlew assembleDebug` 성공
- [ ] 에뮬레이터에서 로그인 성공 → CalendarFragment 이동 확인
- [ ] 에뮬레이터에서 회원가입 성공 → Firestore Console에서 users/{uid} 문서 확인
- [ ] 에뮬레이터에서 잘못된 비밀번호 → 한국어 에러 메시지 표시 확인
- [ ] 에뮬레이터에서 형식 오류 이메일 → 클라이언트 검증 차단 확인
- [ ] 에뮬레이터에서 로그아웃 → LoginFragment 복귀 확인

---

*Phase: 02-auth-core*
*Validation strategy created: 2026-05-06*
