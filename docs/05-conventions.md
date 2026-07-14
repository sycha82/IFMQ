# 핵심 규칙 (코드 수정 시 반드시 지킬 것)

1. **DTO는 `common`에만 정의.** 타 모듈에서 복제 금지.
2. **if_msg_log는 WCS 입장만 기록.** wms-mock·rcs-mock·테스트용 Producer는 DB 로그 없음.
3. **MQ INBOUND는 멱등 처리.** `(direction, message_id)` 중복이면 `MsgLogService.insertInbound`가 null 반환 → Consumer skip.
4. **RCS 연계 전문은 `RcsMsgLogService`로 SEND/RECEIVE 적재** (wcs_shuttle_msg_log).
5. **라우팅 키/Exchange/큐/base-url은 application.yml에서 관리.** 하드코딩 금지.
6. **여러 Queue 빈이 있는 Config에서 바인딩은 큐 빈 메서드 직접 호출**로 참조
   (`bind(inboundCmdQueue())`). 파라미터 주입은 `-parameters` 미적용 시 모호성 오류.
7. 스키마 변경 시 `schema.sql` + 엔티티 + Mapper 인터페이스 + XML 동반 수정.
8. 코드 스타일: Lombok `@SuperBuilder`(+`@NoArgsConstructor`), 로그/주석 한국어.
9. wcsTaskId 포맷 = `{eqpPalletId}-{cycleNo}`.

## 제외 범위 (현 단계)
인증/보안 · Dead Letter Queue · 재시도 정책 · 동기 타임아웃 처리.
