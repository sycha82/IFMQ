# IFMQ — 4Way 셔틀 WCS 샘플 (WMS·RCS 연계)

WMS(상위)와 WCS(창고제어) 간 **RabbitMQ Topic Exchange 비동기 메시지**,
WCS와 RCS(설비제어) 간 **REST(Feign) 동기 호출**로 구성된 4Way 셔틀 ASRS 샘플 백엔드.

- WMS↔WCS 송수신 전문은 WCS 입장에서 PostgreSQL `inf.if_msg_log` 에 적재
- WCS↔RCS 송수신 전문은 shuttle-wcs 기준으로 `biz.wcs_shuttle_msg_log` 에 적재
- 설계 근거 문서: `4way_shuttle` 인터페이스 명세 (c1 입고 / c3 출고 외부 피킹존 Case)

---

## 1. 기술 스택 / 환경

| 항목 | 값 |
|------|-----|
| Java | 21 |
| Spring Boot | 3.4.1 (+ Spring Cloud OpenFeign) |
| Build | Maven (멀티 모듈) |
| MQ | RabbitMQ 3.13-management (Docker, 5672/15672 guest/guest) |
| DB | PostgreSQL (외부, localhost:5438 / db=wcs / user=wcs) |
| ORM | MyBatis 3.x (Mapper 인터페이스 + XML) |
| 기타 | Lombok (@SuperBuilder) |

---

## 2. 모듈 구조 (5개)

```
ifmq/                        ← parent pom
├── common/                  ← 공통 DTO만. Spring/MQ/DB 의존 없음
│   └── com.example.common.dto
│       ├── WcsMessageBase        (messageType, messageId, refMessageId, sequenceNo, timestamp)
│       ├── 입고: InboundCmdDto(+InboundCmdDetail), InboundCompleteDto, InboundCancelDto,
│       │        BcrReadDto, InboundTaskDto/AckDto, InboundDoneDto/AckDto, MappingRegisterDto
│       ├── 출고: OutboundCmdDto(+OutboundCmdItem), OutboundCmdAckDto,
│       │        OutboundTaskDto/AckDto, OutboundDoneDto/AckDto
│       └── 공용: StationStatusDto
│
├── wcs-app/                 ← WMS 연계 MQ 어댑터 (port 9001, DB=inf.if_msg_log)
│   ├── consumer/  InboundCmdConsumer · InboundCancelConsumer · OutboundCmdConsumer
│   ├── producer/  InboundCompleteProducer · OutboundCmdAckProducer · InboundCmdProducer(테스트용)
│   ├── controller/ InternalController(/internal/inbound-complete) · TestController(/test/*)
│   ├── client/    ShuttleWcsClient → shuttle-wcs 위임 (inbound-order, outbound-order, outbound-start, inbound-start)
│   ├── cli/       WcsCliRunner — 1. INBOUND_COMPLETE 발행 / 2. 출고 시작 / 3. 입고 시작(placeholder)
│   └── service/db MsgLogService · IfMsgLog(Mapper)  ← if_msg_log 적재/멱등/상태전이
│
├── shuttle-wcs/             ← WCS 비즈니스 모듈 (port 9002, DB=biz 스키마)
│   ├── controller/
│   │   ├── InboundOrderController   POST /internal/inbound-order    (wcs-app 위임 수신)
│   │   ├── OutboundOrderController  POST /internal/outbound-order   (〃, ACK 반환)
│   │   ├── OutboundStartController  POST /internal/outbound-start   (출고 시작 트리거)
│   │   ├── InboundStartController   POST /internal/inbound-start    (입고 시작 — placeholder, 상태조회만)
│   │   ├── MappingController        POST /api/mapping               (PRE03 매핑 등록)
│   │   ├── RcsInboundController     POST /rcs/station-status · /rcs/bcr-read · /rcs/inbound-done
│   │   └── RcsOutboundController    POST /rcs/outbound-done
│   ├── service/  InboundOrderService · MappingService · RcsInboundService · InboundStartService(placeholder)
│   │             OutboundOrderService · OutboundStartService · OutboundDoneService
│   │             RcsMsgLogService(wcs_shuttle_msg_log SEND/RECEIVE)
│   └── client/   RcsClient(→rcs-mock: inbound-task, outbound-task) · WcsAppClient(→wcs-app: inbound-complete)
│
├── rcs-mock/                ← RCS/설비ECS 시뮬레이터 (port 9003, DB 없음)
│   ├── cli/  RcsCliRunner — 1. STATION_STATUS(입고) / 2. BCR_READ / 3. INBOUND_DONE
│   │                        4. STATION_STATUS(출고) / 5. OUTBOUND_DONE
│   ├── controller/ RcsTaskController — POST /rcs/inbound-task · /rcs/outbound-task (ACK 동기 회신)
│   └── controller/ TestController — POST /test/* (CLI 메뉴 1:1 대응 REST, 도커용)
│
└── wms-mock/                ← WMS 시뮬레이터 (port 9004, DB 없음)
    ├── cli/  WmsCliRunner — 1. INBOUND_CMD / 2. INBOUND_CANCEL / 3. OUTBOUND_CMD
    ├── producer/ InboundCmdPublisher · InboundCancelPublisher · OutboundCmdPublisher
    ├── consumer/ InboundCompleteConsumer · OutboundCmdAckConsumer (화면 출력만)
    └── controller/ TestController — POST /test/* (CLI 메뉴 1:1 대응 REST, 도커용)
```

`wcs-app`의 CLI 메뉴(출고 시작 등)도 `POST /test/outbound-start`로 동일하게 REST 호출 가능
(TestController에 통합, 기존 inbound-cmd·inbound-complete 엔드포인트와 함께).

호출 방향: `wms-mock ⇄(MQ)⇄ wcs-app ⇄(Feign)⇄ shuttle-wcs ⇄(Feign/REST)⇄ rcs-mock`

---

## 3. 빌드 & 실행

### 3-1. 로컬 mvn 실행 (CLI 메뉴 조작)

```bash
docker compose up -d rabbitmq           # RabbitMQ만 (최초 1회). Postgres는 별도 로컬 Docker(5438)
psql -h localhost -p 5438 -U wcs -d wcs -f shuttle-wcs/src/main/resources/sql/schema.sql

mvn install -DskipTests                 # 반드시 루트에서 (common 선행 빌드 필요)

# 터미널 4개
cd wcs-app     && mvn spring-boot:run   # 9001 + CLI
cd shuttle-wcs && mvn spring-boot:run   # 9002
cd rcs-mock    && mvn spring-boot:run   # 9003 + CLI
cd wms-mock    && mvn spring-boot:run   # 9004 + CLI
```

base-url은 각 application.yml에서 환경변수로 override 가능
(`SHUTTLE_WCS_BASE_URL`, `RCS_MOCK_BASE_URL`, `WCS_APP_BASE_URL`).

### 3-2. 전체 도커 실행 (앱 4종 컨테이너화, CLI 대신 REST)

앱 4개(`wcs-app`·`shuttle-wcs`·`rcs-mock`·`wms-mock`)는 도커로 띄우고,
Postgres는 기존 로컬 Docker(5438)를 그대로 사용한다(`docker-compose.yml`에 미포함).
컨테이너 CLI는 표준입력이 없어 조작 불가 — **모든 CLI 메뉴는 `TestController`의
`POST /test/*` 엔드포인트로 1:1 대응**되어 있으니 이걸로 조작한다.

```bash
docker compose up -d --build            # rabbitmq + wcs-app + shuttle-wcs + rcs-mock + wms-mock

# 예시: 입고 지시 발행 (기본값 사용, body 생략 가능)
curl -X POST http://localhost:9004/test/inbound-cmd
curl -X POST http://localhost:9003/test/station-status-in
curl -X POST http://localhost:9003/test/bcr-read
curl -X POST http://localhost:9003/test/inbound-done
curl -X POST http://localhost:9001/test/inbound-start   # placeholder — 상태 조회/로깅만
curl -X POST http://localhost:9004/test/outbound-cmd
curl -X POST http://localhost:9001/test/outbound-start
curl -X POST http://localhost:9003/test/station-status-out
curl -X POST http://localhost:9003/test/outbound-done

docker compose logs -f wcs-app          # 개별 서비스 로그 확인
docker compose down                     # 종료 (RabbitMQ 볼륨은 유지)
```

- Linux Docker Engine은 `host.docker.internal`이 기본 미지원이라
  `extra_hosts: host-gateway` 매핑을 `wcs-app`·`shuttle-wcs`에 넣어뒀다(맥/윈도우 Docker Desktop은 자동 지원).
- 코드 변경 후에는 `docker compose up -d --build`로 재빌드해야 반영된다.
- `Dockerfile`은 루트 1개, 멀티스테이지 + `--target`으로 4개 이미지를 만든다
  (공유 `build` 스테이지를 4개 서비스가 캐시 공유).

---

## 4. 메시지 계약

### 4-1. WMS↔WCS — RabbitMQ (Exchange `wcs.topic`)

| 흐름 | 라우팅 키 | 큐 | 메시지 |
|------|-----------|-----|--------|
| WMS→WCS | `wms.inbound.cmd` | `wcs.queue.inbound.cmd` | INBOUND_CMD (taskId + inboundDetail[]) |
| WMS→WCS | `wms.inbound.cancel` | `wcs.queue.inbound.cancel` | INBOUND_CANCEL |
| WCS→WMS | `wcs.inbound.complete` | `wms.queue.inbound.complete` | INBOUND_COMPLETE |
| WMS→WCS | `wms.outbound.cmd` | `wcs.queue.outbound.cmd` | OUTBOUND_CMD (taskId + items[]: palletId·itemCode·lotId·pickQty) |
| WCS→WMS | `wcs.outbound.cmd.ack` | `wms.queue.outbound.cmd.ack` | OUTBOUND_CMD_ACK (result=ACCEPTED\|REJECTED) |

### 4-2. WCS↔RCS — REST 동기 (요청의 응답이 곧 ACK)

| API | 방향 | 엔드포인트(shuttle-wcs 기준) | 비고 |
|-----|------|------------------------------|------|
| STATION_STATUS | RCS→WCS | POST /rcs/station-status | 입고/출고 공용, stationType으로 구분, upsert |
| BCR_READ | RCS→WCS | POST /rcs/bcr-read | 입고: 스캔 → INBOUND_TASK 자동 발행 |
| INBOUND_TASK / ACK | WCS→RCS | POST {rcs}/rcs/inbound-task | wcsTaskId = eqpPalletId-cycleNo |
| INBOUND_DONE / ACK | RCS→WCS | POST /rcs/inbound-done | 완료 → 재고 적재 + INBOUND_COMPLETE 자동 통보 |
| OUTBOUND_TASK / ACK | WCS→RCS | POST {rcs}/rcs/outbound-task | eqpPalletId + destStation |
| OUTBOUND_DONE / ACK | RCS→WCS | POST /rcs/outbound-done | 배출 완료 → PICKING_ZONE + 재고 소멸 |

---

## 5. DB 스키마

### 5-1. `inf.if_msg_log` (wcs-app) — WMS 전문 이력
direction(INBOUND|OUTBOUND, WCS 기준) · message_type · message_id · payload(JSONB) ·
status(RECEIVED→PROCESSING→COMPLETED / FAILED). 멱등키 `(direction, message_id)` UNIQUE.
- INBOUND(Consumer): RECEIVED → PROCESSING → COMPLETED (예외 FAILED)
- OUTBOUND(Producer): 발행 성공 COMPLETED insert / 실패 FAILED insert

### 5-2. `biz` 스키마 (shuttle-wcs) — 전체 DDL은 `shuttle-wcs/src/main/resources/sql/schema.sql`

| 테이블 | 용도 |
|--------|------|
| wcs_inbound_order_h / _d | 입고 지시 헤더/상세 (task→pallet 라인, 중복 taskId 409) |
| wcs_outbound_order_h / _d | 출고 지시 헤더/상세 (입고와 동일 구조, 중복 taskId → ACK REJECTED) |
| wcs_pallet_line_h (+vw) | Pallet 적재 라인 시계열 (is_latest) |
| wcs_eqp_pallet_m | 설비파레트 마스터 |
| wcs_eqp_pallet_map (+_h) | EqpPalletId↔PalletId 매핑 현재상태 + 이력 (cycle_no) |
| wcs_station | 스테이션 상태 (STATION_STATUS upsert, INBOUND/OUTBOUND) |
| wcs_shuttle_msg_log | RCS REST 송수신 이력 (SEND/RECEIVE) |
| wcs_inventory (+vw_available) | 랙 재고. **available = quantity − reserved_qty** |

### 5-3. 상태 모델

```
eqp_pallet_map.map_status : EMPTY → MAPPED → IN_PROGRESS → STORED → (출고) IN_PROGRESS → OUTBOUND
eqp_pallet_map.location   : IDLE → STATION → IN_RACK → OUTBOUNDING → PICKING_ZONE
outbound_order_h.cmd_status : RECEIVED → DISPATCHED → COMPLETED
재고 : INBOUND_DONE 적재(+) / OUTBOUND_CMD 예약(reserved_qty=quantity)
       / OUTBOUND_DONE 소멸(행 삭제). 출고 진행중 팔렛은 location=OUTBOUNDING으로 입고와 구분
```

---

## 6. 구현된 플로우

### 6-1. 입고 (c1)
```
WMS INBOUND_CMD(MQ) → wcs-app if_msg_log 멱등 적재 → shuttle-wcs 위임(H/D 저장, 중복 409)
→ [PRE03] POST /api/mapping (EqpPalletId↔PalletId 매핑, pallet_line_h 생성)
→ RCS BCR_READ → 스테이션 점유(BUSY) → INBOUND_TASK/ACK 자동 발행 (MAPPED→IN_PROGRESS)
→ RCS INBOUND_DONE → STORED/IN_RACK 전이 + 재고 적재 + 스테이션 해제
→ INBOUND_COMPLETE 자동 통보 (shuttle-wcs → wcs-app → MQ → WMS)
```

### 6-2. 출고 (c3 · Case A: WMS가 PalletId 지정, 외부 피킹존)
```
[PRE] RCS STATION_STATUS(OUTBOUND) → wcs_station upsert
[01/02] WMS OUTBOUND_CMD(MQ) → wcs-app 멱등 적재 → shuttle-wcs 위임
        검증: taskId 중복 / 매핑 미존재 / STORED 아님 / 이미 예약된 팔렛 → REJECTED
        통과: H/D 저장(RECEIVED) + 팔렛 전체 예약(reserved_qty=quantity, 가용재고 제외)
        → OUTBOUND_CMD_ACK(MQ) 회신
[03/04] 출고 시작(wcs-app CLI 2번, 작업자 트리거) → /internal/outbound-start
        RECEIVED 지시 전체를 팔렛 라인별 순차 OUTBOUND_TASK 발행 (일괄, 완료 대기 없음)
        destStation = OUTBOUND 스테이션 1건(가용성 판정 없음 — 버퍼 게이팅은 주체 미정으로 보류)
        성공 라인: STORED→IN_PROGRESS, IN_RACK→OUTBOUNDING / 전 라인 성공 시 H→DISPATCHED
        스킵(best-effort): 매핑 미존재·REJECTED 라인은 건너뛰고 계속
[05/06] RCS OUTBOUND_DONE(rcs-mock CLI 5번 수동) → 검증(wcsTaskId·IN_PROGRESS/OUTBOUNDING)
        → OUTBOUND/PICKING_ZONE 전이 + 랙 재고 삭제 + D 완료·task 전체 완료 시 H→COMPLETED
        → OUTBOUND_DONE_ACK 회신
```

### 6-3. 재고 예약 모델 (설계 결정)
- 팔렛트 단위 전량 출고 운영(부분 피킹은 외부 피킹존에서 후처리 → 잔량 재입고)이므로
  **예약은 팔렛 전체**(reserved_qty=quantity). pickQty 부분 예약 금지.
- 동일 팔렛이 입출고를 반복하므로 주문 테이블 조인 파생이 아닌 **재고 컬럼(reserved_qty)** 방식 채택.
- 예약 시점 = OUTBOUND_CMD 수신(할당). 물리 소멸 = OUTBOUND_DONE(배출).
- 이미 예약된 팔렛에 대한 중복 OUTBOUND_CMD는 REJECTED.

### 6-4. 미구현 (후속 백로그)
OUTBOUND_COMPLETE(07, WMS 팔렛 단위 완료 통보) · OUTBOUND_ORDER_COMPLETE(07-E) ·
PICKING_REPORT/ACK(08/09) · INVENTORY_ADJUSTMENT(10) · OUTBOUND_MISMATCH(02-A 재고 대사) ·
Case B(SKU+수량, 02-B CONFIRM) · 출고 취소/실패 처리(예약 해제 reserved_qty=0) ·
rcs-mock OUTBOUND_DONE 자동 주기 발행 · OUTBOUND_TASK 동시 유지 개수 제한(예: 10건)

---

## 7. 핵심 규칙 (코드 수정 시 반드시 지킬 것)

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

---

## 8. 검증 명령

```bash
mvn -q -pl common install              # 계약(DTO) 변경 후 우선 빌드
mvn -q -pl shuttle-wcs -am install -DskipTests
mvn -q install -DskipTests             # 전체 통합 빌드

# 큐 비우기
docker exec rabbitmq rabbitmqctl purge_queue wcs.queue.outbound.cmd
```

주요 확인 쿼리:
```sql
SELECT * FROM inf.if_msg_log ORDER BY log_id DESC LIMIT 10;
SELECT * FROM biz.wcs_shuttle_msg_log ORDER BY log_id DESC LIMIT 10;
SELECT * FROM biz.wcs_vw_available_inventory;
SELECT eqp_pallet_id, pallet_id, map_status, location, cycle_no FROM biz.wcs_eqp_pallet_map;
SELECT task_id, cmd_status FROM biz.wcs_outbound_order_h;
```

---

## 9. 제외 범위 (현 단계)
인증/보안 · Dead Letter Queue · 재시도 정책 · 동기 타임아웃 처리 · 앱 컨테이너화.
