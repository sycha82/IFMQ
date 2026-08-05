# DB 스키마

## `inf.if_msg_log` (wcs-app) — WMS 전문 이력
direction(INBOUND|OUTBOUND, WCS 기준) · message_type · message_id · payload(JSONB) ·
status(RECEIVED→PROCESSING→COMPLETED / FAILED). 멱등키 `(direction, message_id)` UNIQUE.
- INBOUND(Consumer): RECEIVED → PROCESSING → COMPLETED (예외 FAILED)
- OUTBOUND(Producer): 발행 성공 COMPLETED insert / 실패 FAILED insert

## `biz` 스키마 (shuttle-wcs)
전체 DDL은 `shuttle-wcs/src/main/resources/sql/schema.sql`.

| 테이블 | 용도 |
|--------|------|
| wcs_inbound_order_h / _d | 입고 지시 헤더/상세 (task→pallet 라인, 중복 taskId 409) |
| wcs_outbound_order_h / _d | 출고 지시 헤더/상세 (입고와 동일 구조, 중복 taskId → ACK REJECTED) |
| wcs_pallet_line_h (+vw) | Pallet 적재 라인 시계열 (is_latest). **PRE03 매핑 시점에 생성** |
| wcs_eqp_pallet_m | 설비파레트 마스터 |
| wcs_eqp_pallet_map (+_h) | EqpPalletId↔PalletId 매핑 현재상태 + 이력 (cycle_no) |
| wcs_station | 스테이션 상태 (STATION_STATUS upsert, INBOUND/OUTBOUND) |
| wcs_shuttle_msg_log | RCS REST 송수신 이력 (SEND/RECEIVE) — 전문 payload 원본 |
| wcs_task_h | **셔틀 작업(TASK) 이력** — TASK 발행~START~DONE 요약. 사용자 조회용 |
| wcs_inventory (+vw_available) | 랙 재고. **available = quantity − reserved_qty** |

### `wcs_task_h` (작업 이력)
`wcs_shuttle_msg_log`가 전문 payload 원본이라면, 이 테이블은 **작업 단위 요약**이다
(사용자가 JSON을 뒤지지 않고 작업 진행 상태를 확인하는 용도).
- `wcs_task_id` = `TSK-{8자리}` — 시퀀스 `biz.wcs_task_seq` 로 **TASK 발행 시마다 채번**(유니크키도 이 컬럼 단독).
  입고 TASK와 출고 TASK가 같은 팔렛·같은 사이클이어도 서로 다른 ID를 갖는다.
- 팔렛/사이클 역추적은 조인 없이 이 테이블의 `eqp_pallet_id` · `cycle_no` · `pallet_id` · `order_task_id` 로 가능.
- `task_status` : `DISPATCHING`(발송 전 선기록) → `DISPATCHED`(ACK 수락) → `STARTED`(설비 착수)
  → `COMPLETED` / `FAILED`. ACK 거부는 `REJECTED`, 전송 실패(수신 여부 불명)는 `FAILED`.
- **적재 시점 : 전문 발송 "이전"에 선기록(`DISPATCHING`), ACK 수신 후 결과 반영.**
  선기록·결과반영은 `REQUIRES_NEW`(별도 트랜잭션)로 커밋한다 — 이유는 아래.
  - 외부(RCS)에 공표할 `wcsTaskId`는 공표 전에 내구화되어야 한다. 발송 후 기록하면
    전송~커밋 사이 장애 시 설비는 작업을 수행했는데 WCS엔 기록이 없는 물리-논리 괴리가 생긴다.
  - ACK 거부로 본 처리(스테이션 점유 등)가 롤백돼도 "시도했고 거부당했다"는 이력은 남아야 한다.
- `DISPATCHING` 상태로 오래 남은 행 = 발송했으나 응답을 못 받은 작업(조사 대상 신호)
- RCS가 보고한 wcsTaskId 검증은 팔렛 상태 유도가 아니라 이 테이블 조회(`requireTask`)로 수행
- 조회 API : `GET /api/tasks` (taskType·taskStatus·eqpPalletId·limit 필터) · `GET /api/tasks/{wcsTaskId}`

## 상태 모델

```
eqp_pallet_map.map_status : EMPTY → MAPPED → IN_PROGRESS → STORED → (출고) IN_PROGRESS → OUTBOUND
eqp_pallet_map.location   : IDLE → STATION → IN_RACK → OUTBOUNDING → PICKING_ZONE
  · 출고: OUTBOUND_TASK 는 map_status만 IN_PROGRESS(location IN_RACK 유지),
    OUTBOUND_START(설비 착수) 시 location IN_RACK→OUTBOUNDING, OUTBOUND_DONE 시 →PICKING_ZONE
wcs_station.status        : AVAILABLE ↔ BUSY (BCR_READ 점유 / INBOUND_START 해제)
outbound_order_h.cmd_status : RECEIVED → DISPATCHED → COMPLETED
재고 : INBOUND_DONE 적재(+) / OUTBOUND_CMD 예약(reserved_qty=quantity)
       / OUTBOUND_DONE 소멸(행 삭제). 출고 진행중 팔렛은 location=OUTBOUNDING으로 입고와 구분
```

> 스키마 변경 시 반드시 `schema.sql` + 엔티티 + Mapper 인터페이스 + XML 을 함께 수정한다.
