# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-05-05)

**Core value:** 로그인 성공 후 사용자 정보가 Firestore에 기록되고 캘린더 화면으로 전환되는 전체 흐름이 끊김 없이 동작
**Current focus:** Phase 1 — Project Foundation + Firebase Setup

## Current Position

Phase: 1 of 4 (Project Foundation + Firebase Setup)
Plan: 0 of 3 in current phase
Status: Ready to execute
Last activity: 2026-05-05 — Phase 1 planned (3 plans in 3 waves)

Progress: [░░░░░░░░░░] 0%

## Performance Metrics

**Velocity:**
- Total plans completed: 0
- Average duration: —
- Total execution time: 0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 1 | 0/3 | — | — |

## Accumulated Context

### Decisions

- Firebase Auth (이메일/비밀번호) + Firestore — 서버 없이 Firebase SDK 직접 사용
- Single-Activity + Navigation Component — Google 권장 패턴, back stack 자동 관리
- MVVM + StateFlow — 2025-2026 공식 아키텍처 권장
- Firebase BoM 사용 — 개별 버전 핀 금지
- No Hilt — by viewModels() 충분 (capstone 2-3 화면)
- viewModelScope + Task.await() — lifecycleScope 대신 사용
- Firestore upsert non-fatal — 인증 성공이 핵심

### Pending Todos

None.

### Blockers/Concerns

- google-services.json은 Firebase Console에서 수동 다운로드 필요 (자동화 불가)
- 라이브러리 버전 일부 (Kotlin 2.3.21, Navigation 2.9.8 등) 연구 기준 — 실행 시 Maven Central 검증 권장

## Session Continuity

Last session: 2026-05-05
Stopped at: Phase 1 planning complete — 3 PLAN.md files verified, ready to execute
Resume file: None
