# Capstone Login — Android Firebase Auth

## What This Is

Android Kotlin 앱의 로그인 모듈. Firebase Authentication으로 이메일/비밀번호 로그인을 처리하고, 로그인 성공 시 Firestore `users` 컬렉션에 사용자 정보를 저장/업데이트한 뒤 캘린더 메인 화면으로 이동하는 내비게이션 흐름을 담당한다. 별도의 Spring Boot 서버 없이 모든 인증·저장 로직을 앱 내부에서 직접 처리한다.

## Core Value

로그인 성공 후 사용자 정보가 Firestore에 기록되고 캘린더 화면으로 전환되는 전체 흐름이 끊김 없이 동작해야 한다.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] Android 프로젝트 생성 및 Firebase 연동 설정 (Auth + Firestore)
- [ ] 이메일/비밀번호 로그인 화면(LoginActivity/Fragment) 구현
- [ ] Firebase Auth로 로그인 수행 로직 구현
- [ ] 로그인 성공 시 Firestore `users` 컬렉션에 사용자 저장/업데이트 (upsert)
- [ ] 로그인 여부에 따라 캘린더 메인 화면으로 이동하는 내비게이션 로직
- [ ] 앱 시작 시 기존 로그인 상태 확인 (자동 로그인)

### Out of Scope

- Spring Boot 백엔드 서버 — Firebase SDK가 모든 인증·저장 담당
- OAuth2 / 소셜 로그인 — v1은 이메일/비밀번호만
- 비밀번호 재설정 — 우선순위 낮음, 추후 추가
- 캘린더 기능 구현 — 이 모듈의 범위 밖

## Context

- **플랫폼**: Android (Kotlin)
- **인증 제공자**: Firebase Authentication (이메일/비밀번호)
- **데이터 저장**: Firestore — `users/{uid}` 문서에 이메일, 이름 등 저장
- **내비게이션**: 로그인 화면 → 캘린더 메인 화면 (캘린더 화면은 플레이스홀더로 시작)
- **현재 상태**: 프로젝트 디렉토리에 Android 프로젝트 미생성, 새로 구성 필요
- 코드베이스 분석 결과 `src/main/kotlin/Main.kt` 스텁만 존재, 빌드 도구 없음

## Constraints

- **Tech Stack**: Kotlin + Android SDK + Firebase (Auth, Firestore)
- **No Server**: 백엔드 API 서버 없음 — Firebase SDK 직접 사용
- **Android Studio**: IntelliJ 기반 Android 프로젝트로 전환 필요

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Firebase Auth (이메일/비밀번호) | 서버 없이 인증 처리, capstone 복잡도 최소화 | — Pending |
| Firestore upsert on login | 첫 로그인 시 자동 사용자 생성, 재로그인 시 업데이트 | — Pending |
| 별도 서버 없음 | 앱 내 Firebase SDK로 모든 로직 처리 | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-05-04 after initialization*
