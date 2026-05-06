# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-05-05)

**Core value:** 로그인 성공 후 사용자 정보가 Firestore에 기록되고 캘린더 화면으로 전환되는 전체 흐름이 끊김 없이 동작
**Current focus:** Phase 2 — Auth Core

## Current Position

Phase: 2 of 4 (Auth Core)
Plan: 0 of ? in current phase
Status: Ready to plan
Last activity: 2026-05-06 — Phase 1 complete (3/3 plans done, build verified, google-services.json placed)

Progress: [██░░░░░░░░] 25%

## Performance Metrics

**Velocity:**
- Total plans completed: 3
- Average duration: ~7 min/plan
- Total execution time: ~20 min

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 1 | 3/3 | ~20 min | ~7 min |
| 2 | 0/? | — | — |

## Accumulated Context

### Decisions

- Firebase Auth (이메일/비밀번호) + Firestore — 서버 없이 Firebase SDK 직접 사용
- Single-Activity + Navigation Component — Google 권장 패턴, back stack 자동 관리
- MVVM + StateFlow — 2025-2026 공식 아키텍처 권장
- Firebase BoM 사용 — 개별 버전 핀 금지
- No Hilt — by viewModels() 충분 (capstone 2-3 화면)
- viewModelScope + Task.await() — lifecycleScope 대신 사용
- Firestore upsert non-fatal — 인증 성공이 핵심
- **빌드 툴체인 다운그레이드:** AGP 8.1.0 / Gradle 8.1 / Kotlin 1.9.0 / compileSdk 34 (로컬 환경 호환)

### Pending Todos

None.

### Blockers/Concerns

None. (google-services.json 배치 완료, 빌드 성공 확인)

## Session Continuity

Last session: 2026-05-06
Stopped at: Phase 1 complete — all 3 plans executed, build verified
Resume file: None
