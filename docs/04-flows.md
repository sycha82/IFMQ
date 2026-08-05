# 구현된 플로우

## 입고 (c1)
```
WMS INBOUND_CMD(MQ) → wcs-app if_msg_log 멱등 적재 → shuttle-wcs 위임(H/D 저장, 중복 409)
→ [PRE03] POST /api/mapping (EqpPalletId↔PalletId 매핑, pallet_line_h 생성)
→ RCS BCR_READ → 스테이션 점유(BUSY) → INBOUND_TASK/ACK 자동 발행 (MAPPED→IN_PROGRESS)
→ RCS INBOUND_START → 스테이션 해제(BUSY→AVAILABLE, 팔렛이 셔틀에 실려 스테이션 이탈)
→ RCS INBOUND_DONE → STORED/IN_RACK 전이 + 재고 적재
→ INBOUND_COMPLETE 자동 통보 (shuttle-wcs → wcs-app → MQ → WMS)
```
- 스테이션 해제는 **INBOUND_START(착수) 시점**. INBOUND_DONE은 재고 적재만 담당.
  INBOUND_START를 건너뛰면 스테이션이 BUSY로 남아 다음 팔렛 BCR_READ가 거부됨.

## 출고 (c3 · Case A: WMS가 PalletId 지정, 외부 피킹존)
```
[PRE] RCS STATION_STATUS(OUTBOUND) → wcs_station upsert
[01/02] WMS OUTBOUND_CMD(MQ) → wcs-app 멱등 적재 → shuttle-wcs 위임
        검증: taskId 중복 / 매핑 미존재 / STORED 아님 / 이미 예약된 팔렛 → REJECTED
        통과: H/D 저장(RECEIVED) + 팔렛 전체 예약(reserved_qty=quantity, 가용재고 제외)
        → OUTBOUND_CMD_ACK(MQ) 회신
[03/04] 출고 요청(wcs-app CLI 2번, 작업자 트리거) → /internal/user-outbound-request
        RECEIVED 지시 전체를 팔렛 라인별 순차 OUTBOUND_TASK 발행 (한 번의 요청 안 for-loop, 주기·타이머 없음, 완료 대기 없음)
        destStation = OUTBOUND 스테이션 1건(가용성 판정 없음 — 버퍼 게이팅은 주체 미정으로 보류)
        성공 라인: STORED→IN_PROGRESS (location IN_RACK 유지) / 전 라인 성공 시 H→DISPATCHED
        스킵(best-effort): 매핑 미존재·REJECTED 라인은 건너뛰고 계속
[START] RCS OUTBOUND_START(설비 착수, rcs-mock CLI 7번 수동) → 검증(wcsTaskId·IN_PROGRESS/IN_RACK)
        → location IN_RACK→OUTBOUNDING (팔렛이 실제 랙을 떠남). map_status는 IN_PROGRESS 유지
[05/06] RCS OUTBOUND_DONE(rcs-mock CLI 5번 수동) → 검증(wcsTaskId·IN_PROGRESS/OUTBOUNDING)
        → OUTBOUND/PICKING_ZONE 전이 + 랙 재고 삭제 + D 완료·task 전체 완료 시 H→COMPLETED
        → OUTBOUND_DONE_ACK 회신
```
- OUTBOUNDING 전이는 **OUTBOUND_START(착수) 시점**(입고 INBOUND_START 대칭). OUTBOUND_TASK는 IN_PROGRESS만.
  OUTBOUND_START를 건너뛰면 location=IN_RACK로 남아 OUTBOUND_DONE 검증(OUTBOUNDING 요구)에서 거부됨.

## 설비 자동 시뮬레이션 (rcs-mock)
`rcs.auto-simulation.enabled=true`(기본) 이면 rcs-mock 이 **TASK 수신 시점**에 START/DONE 을
자동 예약해 보고한다. BCR_READ(입고) 또는 user-outbound-request(출고) 한 번이면 사이클이 완주한다.
```
INBOUND_TASK  수신 → (5초) INBOUND_START  → (10초) INBOUND_DONE
OUTBOUND_TASK 수신 → (5초) OUTBOUND_START → (10초) OUTBOUND_DONE
```
- **스테이션 단위 직렬 처리** : 한 스테이션은 동시에 여러 팔렛을 처리할 수 없으므로
  `앞 팔렛 DONE + gap(2초)` 이후에 다음 팔렛이 START 한다(`reserveSlot` 커서).
  `user-outbound-request` 는 TASK 를 for-loop 로 연달아 발행하므로 이 직렬화가 없으면
  전 팔렛의 START/DONE 이 거의 동시에 발생해 **같은 출고 스테이션에 동시 도착**하는
  물리적으로 불가능한 상황이 된다. 입고/출고 스테이션은 키가 달라 서로 간섭하지 않는다.
  ```
  3팔렛 출고 예: TSK-1 START 00:00 DONE 00:10
                TSK-2 START 00:12 DONE 00:22
                TSK-3 START 00:24 DONE 00:34
  ```
- **트리거가 TASK 수신인 이유** : START/DONE 에 필수인 `wcsTaskId` 는 WCS 가 채번해 TASK 전문으로
  내려주는 값이라, BCR_READ 시점에는 알 수 없다.
- **ACK 를 블로킹하면 안 된다** : WCS 의 BCR_READ 트랜잭션 안에서 TASK 전송이 일어나므로,
  예약만 걸고 즉시 ACK 를 반환한다. 지연시간도 WCS 트랜잭션 커밋 여유를 포함해야 한다
  (커밋 전 START 도착 시 작업 이력 조회 실패로 거부됨).
- 수동 시나리오 테스트가 필요하면 `AUTO_SIM_ENABLED=false` 로 끄고 기존 CLI/`/test/*` 사용.
- 시뮬레이션 전송 실패는 무시(경고 로그만) — 이미 수동 진행된 작업이면 WCS 가 상태 검증으로 거부하는 게 정상.

## 재고 예약 모델 (설계 결정)
- 팔렛트 단위 전량 출고 운영(부분 피킹은 외부 피킹존에서 후처리 → 잔량 재입고)이므로
  **예약은 팔렛 전체**(reserved_qty=quantity). pickQty 부분 예약 금지.
- 동일 팔렛이 입출고를 반복하므로 주문 테이블 조인 파생이 아닌 **재고 컬럼(reserved_qty)** 방식 채택.
- 예약 시점 = OUTBOUND_CMD 수신(할당). 물리 소멸 = OUTBOUND_DONE(배출).
- 이미 예약된 팔렛에 대한 중복 OUTBOUND_CMD는 REJECTED.
- available = on-hand(quantity) − reserved_qty. `biz.wcs_vw_available_inventory` 뷰로 조회.

## 미구현 (후속 백로그)
OUTBOUND_COMPLETE(07, WMS 팔렛 단위 완료 통보) · OUTBOUND_ORDER_COMPLETE(07-E) ·
PICKING_REPORT/ACK(08/09) · INVENTORY_ADJUSTMENT(10) · OUTBOUND_MISMATCH(02-A 재고 대사) ·
Case B(SKU+수량, 02-B CONFIRM) · 출고 취소/실패 처리(예약 해제 reserved_qty=0) ·
rcs-mock OUTBOUND_DONE 자동 주기 발행 · OUTBOUND_TASK 동시 유지 개수 제한(예: 10건)

> **미구현 항목의 상세 설계 원문**(payload 필드·예외 EX-01~08·Case B 확인 절차·피킹존 08~11 등)은
> `docs/reference/4way-design-notes.md` 참조. `@import` 안 하는 참조 전용 문서라 필요할 때만 열어 본다.
> 위 백로그(07/07-E/08/09/10/02-A/02-B) 작업 시 먼저 이 파일에서 해당 계약을 확인할 것.
