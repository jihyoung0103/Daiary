# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-05-05)

**Core value:** 로그인 성공 후 사용자 정보가 Firestore에 기록되고 캘린더 화면으로 전환되는 전체 흐름이 끊김 없이 동작
**Current focus:** Phase 4 — Polish + Release Validation (complete, pending human verify)

## Current Position

Phase: 4 of 4 (Polish + Release Validation)
Plan: 2 of 2 in current phase
Status: Human verification checkpoint — assembleRelease 빌드 + 앱 동작 확인
Last activity: 2026-05-06 — Phase 4 complete (비밀번호 토글, IME 액션, displayName 저장, ProGuard, Firestore 보안 규칙)

Progress: [██████████] 100%

## Performance Metrics

**Velocity:**
- Total plans completed: 11
- Total phases completed: 4/4

**By Phase:**

| Phase | Plans | Status |
|-------|-------|--------|
| 1 | 3/3 | Complete |
| 2 | 3/3 | Complete |
| 3 | 2/2 | Complete |
| 4 | 2/2 | Complete |

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
Stopped at: Phase 4 complete — all 11 plans executed across 4 phases
Resume file: None
