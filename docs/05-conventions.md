# 핵심 규칙 (코드 수정 시 반드시 지킬 것)

1. **DTO는 `common`에만 정의.** 타 모듈에서 복제 금지.
2. **if_msg_log는 WCS 입장만 기록.** wms-mock·rcs-mock·테스트용 Producer는 DB 로그 없음.
   적재 주체는 **wcs-app 단독**이다. shuttle-wcs 의 `IfMsgLogView(+Mapper)`는 모니터링 화면
   표시용 **조회 전용**이며(두 모듈이 같은 DB `wcs` 사용), 여기서 INSERT/UPDATE 하지 않는다.
3. **MQ INBOUND는 멱등 처리.** `(direction, message_id)` 중복이면 `MsgLogService.insertInbound`가 null 반환 → Consumer skip.
4. **RCS 연계 전문은 `RcsMsgLogService`로 SEND/RECEIVE 적재** (wcs_shuttle_msg_log).
5. **라우팅 키/Exchange/큐/base-url은 application.yml에서 관리.** 하드코딩 금지.
6. **이 프로젝트는 `-parameters` 없이 컴파일된다** — 파라미터 이름을 런타임에 못 읽는다.
   - 여러 Queue 빈이 있는 Config에서 바인딩은 큐 빈 메서드 직접 호출로 참조(`bind(inboundCmdQueue())`).
   - `@RequestParam`·`@PathVariable`은 **반드시 이름을 명시**한다
     (`@RequestParam(name = "limit", ...)`, `@PathVariable("wcsTaskId")`).
     생략하면 컴파일은 되지만 호출 시 `IllegalArgumentException`으로 500이 난다.
7. 스키마 변경 시 `schema.sql` + 엔티티 + Mapper 인터페이스 + XML 동반 수정.
8. 코드 스타일: Lombok `@SuperBuilder`(+`@NoArgsConstructor`), 로그/주석 한국어.
9. wcsTaskId 포맷 = `TSK-{8자리}` (시퀀스 `biz.wcs_task_seq` 채번). TASK 발행 시마다 신규 발급 —
   입고 TASK와 출고 TASK가 각각 고유 ID를 갖는다. 팔렛 상태에서 유도하지 말고
   `TaskHistoryService.nextWcsTaskId()`로 채번하며, 수신 검증은 `requireTask(...)`(wcs_task_h 조회)로 한다.
10. **외부로 나가는 식별자는 발송 전에 커밋한다(선기록).** TASK 발행은
    `record*Pending()`(REQUIRES_NEW로 즉시 커밋) → 전송 → `markDispatched()`/`markRejected()`
    /`markSendFailed()` 순서. 발송 후 기록하면 전송~커밋 사이 장애 시 물리-논리 괴리가 생기고,
    거부 시 롤백으로 이력이 통째로 사라진다.

## 제외 범위 (현 단계)
인증/보안 · Dead Letter Queue · 재시도 정책 · 동기 타임아웃 처리.
