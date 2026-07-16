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
[03/04] 출고 시작(wcs-app CLI 2번, 작업자 트리거) → /internal/outbound-start
        RECEIVED 지시 전체를 팔렛 라인별 순차 OUTBOUND_TASK 발행 (일괄, 완료 대기 없음)
        destStation = OUTBOUND 스테이션 1건(가용성 판정 없음 — 버퍼 게이팅은 주체 미정으로 보류)
        성공 라인: STORED→IN_PROGRESS, IN_RACK→OUTBOUNDING / 전 라인 성공 시 H→DISPATCHED
        스킵(best-effort): 매핑 미존재·REJECTED 라인은 건너뛰고 계속
[05/06] RCS OUTBOUND_DONE(rcs-mock CLI 5번 수동) → 검증(wcsTaskId·IN_PROGRESS/OUTBOUNDING)
        → OUTBOUND/PICKING_ZONE 전이 + 랙 재고 삭제 + D 완료·task 전체 완료 시 H→COMPLETED
        → OUTBOUND_DONE_ACK 회신
```

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
