# IFMQ — 4Way 셔틀 WCS 샘플 (WMS·RCS 연계)

WMS(상위)와 WCS(창고제어) 간 **RabbitMQ Topic Exchange 비동기 메시지**,
WCS와 RCS(설비제어) 간 **REST(Feign) 동기 호출**로 구성된 4Way 셔틀 ASRS 샘플 백엔드.
설계 근거: `4way_shuttle` 인터페이스 명세 (c1 입고 / c3 출고 외부 피킹존 Case).

## 개발 지침 문서 (docs/)

상세 지침은 `docs/`로 분리해 관리하며, 아래 `@import`로 세션 시작 시 함께 로드된다.
문서를 고칠 땐 `docs/`의 해당 파일을 수정한다(이 파일에 본문을 다시 넣지 말 것).

- @docs/01-architecture.md — 기술 스택 · 5개 모듈 구조 · 호출 방향
- @docs/02-message-contracts.md — WMS↔WCS(MQ) / WCS↔RCS(REST) 메시지 계약
- @docs/03-db-schema.md — inf/biz 스키마 · 상태 모델
- @docs/04-flows.md — 입고(c1)/출고(c3) 플로우 · 재고 예약 모델 · 미구현 백로그
- @docs/05-conventions.md — 코드 수정 시 핵심 규칙 · 제외 범위
- @docs/06-run-and-test.md — 로컬/도커 실행 · Postman/RabbitMQ 발행 · 검증 명령

## 절대 규칙 (요약 — 상세는 docs/05)

1. DTO는 `common`에만 정의(복제 금지).
2. `inf.if_msg_log`는 WCS 입장만 기록. MQ INBOUND는 `(direction, message_id)` 멱등 처리.
3. RCS 연계 전문은 `RcsMsgLogService`로 SEND/RECEIVE 적재(wcs_shuttle_msg_log).
4. 라우팅 키·Exchange·큐·base-url은 application.yml에서 관리(하드코딩 금지).
5. 스키마 변경 시 `schema.sql` + 엔티티 + Mapper + XML 동반 수정.
6. 코드 스타일: Lombok `@SuperBuilder`(+`@NoArgsConstructor`), 로그/주석 한국어.
7. wcsTaskId 포맷 = `{eqpPalletId}-{cycleNo}`.
