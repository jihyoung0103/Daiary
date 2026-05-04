# Requirements: Capstone Login — Android Firebase Auth

**Defined:** 2026-05-04
**Core Value:** Firebase Auth로 로그인한 사용자가 Firestore에 기록되고 캘린더 화면으로 전환되는 전체 흐름이 끊김 없이 동작

## v1 Requirements

### Project Setup

- [ ] **SETUP-01**: Android Gradle 프로젝트가 AGP 9.2.0 + Kotlin 2.3.21 + Kotlin DSL로 생성된다
- [ ] **SETUP-02**: Firebase BoM(Auth-ktx, Firestore-ktx)이 의존성에 추가되고 `google-services.json`이 올바른 위치에 배치된다
- [ ] **SETUP-03**: data / domain / ui 레이어로 구성된 MVVM 아키텍처 골격과 `Result<T>` 래퍼 클래스가 구성된다

### Authentication

- [ ] **AUTH-01**: 사용자가 이메일과 비밀번호로 로그인할 수 있다
- [ ] **AUTH-02**: 사용자가 이메일과 비밀번호로 회원가입할 수 있고, 성공 시 Firestore `users/{uid}` 문서가 생성(upsert)된다
- [ ] **AUTH-03**: 사용자가 로그아웃할 수 있다 (Firebase Auth signOut 호출)
- [ ] **AUTH-04**: 앱 시작 시 기존 로그인 세션이 감지되면 로그인 화면 없이 캘린더 화면으로 자동 이동한다

### UI/UX

- [ ] **UI-01**: Firebase 비동기 호출 중 로딩 인디케이터(ProgressBar 또는 동등한 요소)가 표시된다
- [ ] **UI-02**: FirebaseAuthException 에러 코드에 따라 구체적인 한국어 에러 메시지가 표시된다 (예: 잘못된 비밀번호, 이미 가입된 이메일 등)
- [ ] **UI-03**: 로그인/회원가입 성공 시 캘린더 화면으로 이동하고, back 버튼으로 로그인 화면에 되돌아올 수 없다 (`popUpTo` inclusive)
- [ ] **UI-04**: 이메일 형식과 비밀번호 최소 길이(6자 이상)를 Firebase 호출 전에 클라이언트에서 검사한다

## v2 Requirements

### Auth Enhancements

- **AUTH-V2-01**: 이메일로 비밀번호 재설정 링크를 발송할 수 있다
- **AUTH-V2-02**: 구글 계정으로 OAuth2 소셜 로그인을 할 수 있다
- **AUTH-V2-03**: 회원가입 후 이메일 인증 확인 단계가 있다

### UI Enhancements

- **UI-V2-01**: 다크 모드를 지원한다

## Out of Scope

| Feature | Reason |
|---------|--------|
| Spring Boot 백엔드 서버 | Firebase SDK가 모든 인증·저장 담당 — 서버 불필요 |
| JWT 직접 생성/검증 | Firebase Auth가 토큰 관리 전담 |
| 캘린더 기능 구현 | 이 모듈 범위 밖 — 다른 팀원 담당 |
| 실시간 Firestore 리스너 (로그인 화면) | 불필요한 복잡도 |
| 프로필 편집 화면 | v1 범위 초과 |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| SETUP-01 | Phase 1 | Pending |
| SETUP-02 | Phase 1 | Pending |
| SETUP-03 | Phase 1 | Pending |
| AUTH-01 | Phase 2 | Pending |
| AUTH-02 | Phase 2 | Pending |
| AUTH-03 | Phase 2 | Pending |
| UI-01 | Phase 2 | Pending |
| UI-02 | Phase 2 | Pending |
| UI-04 | Phase 2 | Pending |
| AUTH-04 | Phase 3 | Pending |
| UI-03 | Phase 3 | Pending |

**Coverage:**
- v1 requirements: 11 total
- Mapped to phases: 11
- Unmapped: 0 ✓

---
*Requirements defined: 2026-05-04*
*Last updated: 2026-05-04 after initial definition*
