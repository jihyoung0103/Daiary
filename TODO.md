# TODO

- [ ] RetrospectRepository에 회고 리포트 delete API 부재
  - saveRetrospect만 있고 delete 없음, Firestore 문서 존재 여부로 배너 상태 판단
  - 재생성/재확인 시 기존 데이터 덮어쓰기(월간) 또는 영구 잔존(주간) 위험
  - ACTIVITY/SPENDING 카드 다크모드 실기 검증도 이 문제 때문에 보류됨 (코드 리뷰로만 확인, 패턴은 다른 6개 카드와 동일)
