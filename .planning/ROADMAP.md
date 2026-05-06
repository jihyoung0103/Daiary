# Roadmap: Capstone Login — Android Firebase Auth

## Overview

Firebase Authentication(이메일/비밀번호)과 Firestore를 사용하는 Android Kotlin 로그인 모듈을 4단계에 걸쳐 구축한다. Phase 1에서 Android 프로젝트 기반과 Firebase 연동을 확립하고, Phase 2에서 로그인·회원가입 핵심 흐름을 구현하며, Phase 3에서 자동 로그인과 내비게이션 back stack을 완성한 뒤, Phase 4에서 UX 개선과 릴리즈 검증으로 마무리한다.

## Phases

- [ ] **Phase 1: Project Foundation + Firebase Setup** - Android Gradle 프로젝트 생성 및 Firebase 연동 설정
- [ ] **Phase 2: Auth Core** - 이메일/비밀번호 로그인·회원가입 + Firestore upsert 구현
- [ ] **Phase 3: Navigation + Auto-Login** - back stack 정리 및 앱 시작 시 자동 로그인 라우팅
- [ ] **Phase 4: Polish + Release Validation** - UX 개선, Firestore 보안 규칙, 릴리즈 빌드 검증

## Phase Details

### Phase 1: Project Foundation + Firebase Setup
**Goal**: Firebase에 연결된 빌드 가능한 Android Gradle 프로젝트 완성. 에뮬레이터에서 Firebase 초기화가 정상 동작하고 MVVM 아키텍처 골격이 갖춰진 상태.
**Depends on**: Nothing (first phase)
**Requirements**: SETUP-01, SETUP-02, SETUP-03
**Success Criteria** (what must be TRUE):
  1. Android 프로젝트가 에뮬레이터/기기에서 크래시 없이 실행된다
  2. Firebase Auth와 Firestore SDK가 초기화되고 logcat에서 확인 가능하다
  3. google-services.json이 app/ 디렉토리에 위치하고 패키지명이 일치한다
  4. MVVM 골격(model/User.kt, util/Result.kt, DataSource 스텁, Repository 스텁, ViewModel 스텁)이 존재한다
  5. NavHostFragment가 MainActivity에 설정되고 LoginFragment와 CalendarFragment가 내비게이션 그래프에 등록된다
**Plans**: 3 plans

Plans:
- [ ] 01-01-PLAN.md — Gradle build scripts (settings.gradle.kts, root build.gradle.kts, app/build.gradle.kts, libs.versions.toml, gradle-wrapper.properties, gradle.properties)
- [ ] 01-02-PLAN.md — MVVM architecture skeleton (10 Kotlin source files: model, util, data, ui layers)
- [ ] 01-03-PLAN.md — Android resources (AndroidManifest, layouts, nav_graph.xml, themes) + Firebase setup checkpoint

### Phase 2: Auth Core
**Goal**: 이메일/비밀번호 로그인·회원가입 전체 흐름 동작. Firebase Auth → Firestore upsert → CalendarFragment 이동까지 end-to-end.
**Depends on**: Phase 1
**Requirements**: AUTH-01, AUTH-02, AUTH-03, UI-01, UI-02, UI-04
**Success Criteria** (what must be TRUE):
  1. 이메일/비밀번호로 로그인이 성공하면 CalendarFragment로 이동한다
  2. 회원가입 성공 시 Firestore users/{uid} 문서가 생성(upsert)된다
  3. Firebase 비동기 호출 중 로딩 인디케이터가 표시된다
  4. 잘못된 이메일/비밀번호 입력 시 한국어 에러 메시지가 표시된다
  5. 이메일 형식 오류나 6자 미만 비밀번호는 Firebase 호출 전 클라이언트에서 차단된다
  6. 로그아웃이 가능하다
**Plans**: 3 plans

Plans:
- [ ] 02-01-PLAN.md — FirebaseAuthDataSource + FirestoreUserDataSource (signIn/signUp .await(), upsertUser SetOptions.merge())
- [ ] 02-02-PLAN.md — AuthViewModel mapAuthError() — Firebase 에러 코드 → 한국어 메시지 매핑
- [ ] 02-03-PLAN.md — LoginFragment UI 완성 (입력검증, ProgressBar, 에러, navigate) + CalendarFragment 로그아웃

### Phase 3: Navigation + Auto-Login
**Goal**: 완전한 내비게이션 흐름. 로그인 화면 back stack 제거, 앱 재시작 시 자동 로그인 라우팅, 로그아웃 후 복귀.
**Depends on**: Phase 2
**Requirements**: AUTH-04, UI-03
**Success Criteria** (what must be TRUE):
  1. 로그인/회원가입 성공 후 back 버튼을 눌러도 로그인 화면으로 돌아가지 않는다
  2. 이미 로그인된 상태에서 앱을 재시작하면 로그인 화면 없이 CalendarFragment로 이동한다
  3. CalendarFragment에서 로그아웃하면 LoginFragment로 이동하고 CalendarFragment가 back stack에서 제거된다
**Plans**: TBD

Plans:
- [ ] 03-01: authStateChanges() Flow 기반 자동 로그인 + MainActivity 라우팅
- [ ] 03-02: popUpTo(loginFragment, inclusive=true) 적용 + 로그아웃 흐름 완성

### Phase 4: Polish + Release Validation
**Goal**: P2 UX 개선, Firestore 보안 규칙 강화, assembleRelease 빌드 검증.
**Depends on**: Phase 3
**Requirements**: (none mapped — polish/validation phase)
**Success Criteria** (what must be TRUE):
  1. 비밀번호 보이기/숨기기 토글이 작동한다
  2. Firestore Security Rules가 테스트 모드에서 인증된 사용자만 접근 가능한 규칙으로 전환된다
  3. assembleRelease 빌드가 성공하고 R8/ProGuard가 Firebase 클래스를 제거하지 않는다
  4. google-services.json이 .gitignore에 등록된다
**Plans**: TBD

Plans:
- [ ] 04-01: UX 개선 (비밀번호 토글, IME 액션, displayName 저장)
- [ ] 04-02: Firestore Security Rules + 릴리즈 빌드 검증

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Project Foundation + Firebase Setup | 0/3 | Planned | - |
| 2. Auth Core | 0/3 | Planned | - |
| 3. Navigation + Auto-Login | 0/2 | Not started | - |
| 4. Polish + Release Validation | 0/2 | Not started | - |
