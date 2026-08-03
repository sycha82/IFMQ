# 4Way 셔틀 설계 원문 정제 노트 (c3 출고 외부 피킹존)

> **참조 전용 문서.** CLAUDE.md에서 `@import` 하지 않는다(평소 로드 X). 출고/피킹존/예외 등
> 아직 구현 안 된 부분이나 원문 대조가 필요할 때만 이 파일을 연다.
> 원본: `4way_shuttle_260611_2.html`(이미지 포함, 미커밋). 여기엔 텍스트 계약만 옮김.
> 구현 현황은 `docs/04-flows.md`(구현됨) / 이 문서 하단 "구현 매핑" 참조.

---

## 0. Case 개요

- **c1 입고**: 구현됨 → `docs/04-flows.md` 입고 플로우.
- **c2 출고 전용 GTP**: 현 범위 밖(GTP 포트에서 작업자 피킹 후 잔여 재입고). 미구현.
- **c3 출고 외부 피킹존**: 현재 구현 중인 Case. 아래가 그 설계.

**c3 3단 분리 원칙**: 출고 / 피킹존 작업 / 재입고는 각각 독립 프로세스. 피킹존 작업 완료가
재입고 지시의 선행 조건이 아님. 재입고는 c1 입고 프로세스 재사용(신규 개발 없음).
**재고 책임**: 수량 관리는 WMS 전담. WCS는 피킹 수량을 WMS에 보고.

---

## 1. 전체 플로우 (STEP)

### 출고 프로세스
| STEP | 주체 | 내용 | API |
|------|------|------|-----|
| PRE | RCS | 출고 스테이션 상태 변경 통보 (AVAILABLE/BUSY/ERROR) | STATION_STATUS |
| 01 | WMS | 출고 지시 생성 → WCS. Case A: PalletId 지정 / Case B: SKU+수량 | OUTBOUND_CMD |
| 02 | WCS | 수신 ACK. Case A 매핑 조회 후 즉시 / Case B 팔렛 선정 후. 재고부족·매핑미존재 → REJECTED | OUTBOUND_CMD_ACK |
| 02-A | WCS | **[Case A]** WCS 재고 vs WMS 지시 불일치 시 오류 보고, 해당 지시 중단 | OUTBOUND_MISMATCH |
| 02-B | WCS↔WMS | **[Case B]** WCS 선정 팔렛 확인 요청 → WMS ACK/NACK. NACK 시 대체 팔렛 재선정 | OUTBOUND_CONFIRM_REQ / _ACK |
| 03 | WCS | RCS에 출고 Task (EqpPalletId 기준, destStation) | OUTBOUND_TASK |
| 04 | RCS | 수신 ACK (배정 shuttleId). Shuttle→Lift→출고스테이션 배출 | OUTBOUND_TASK_ACK |
| 05 | RCS | 출고 스테이션 배출 완료 통보 | OUTBOUND_DONE |
| 06 | WCS | EqpPallet location→PICKING_ZONE, 완료 ACK | OUTBOUND_DONE_ACK |
| 07 | WCS | WMS에 팔렛 단위 출고 완료 통보 (PalletId 기준, EqpPalletId 미포함) | OUTBOUND_COMPLETE |
| 07-E | WCS | taskId 내 전체 팔렛 완료 시 지시 단위 완료 통보 | OUTBOUND_ORDER_COMPLETE |

### 피킹존 작업 [TBD — WMS 담당 예상, 설비 외부 재고는 WMS 영역]
| STEP | 주체 | 내용 | API |
|------|------|------|-----|
| — | 작업자 | 외부 피킹존으로 팔렛 이동 후 필요 수량 피킹(박스 단위) | (현장) |
| 08 | WCS 단말 | 팔렛 라벨 스캔 + 피킹 수량 입력 | WCS UI |
| 09 | WCS | WMS에 피킹 보고 (PalletId + 피킹수량 + 잔여수량) | PICKING_REPORT |
| 10 | WMS | 재고 처리(팔렛 차감 + 박스 재고 생성 + 잔여 라벨 갱신), ACK/NACK | PICKING_REPORT_ACK |
| 11 | WCS | ACK 후: 라벨 재발행/수기수정 안내, 출고 팔렛 EMPTY/IDLE 반환 | (내부) |
| 12~ | — | 잔여 팔렛 재입고: **c1 입고 프로세스 그대로 재사용** | c1 참조 |

### 출고 우선순위 (Case B · WCS 팔렛 선택 기준)
1순위 expireDate ASC (FEFO) → 2순위 qty ASC (잔여 적은 것) → 3순위 createdAt ASC (FIFO)

### 상태 전이 (원문)
- 출고 완료 후: 매핑 status=OUTBOUND · EqpPalletId location=PICKING_ZONE
- 피킹 보고 완료 후: EqpPalletId status=EMPTY · location=IDLE

---

## 2. 메시지 계약 (payload 요지)

> 공통 봉투: messageType, messageId, refMessageId, sequenceNo, timestamp (WcsMessageBase)

### PRE · STATION_STATUS (RCS→WCS)
`stationId`, `stationType`(OUTBOUND), `status`(AVAILABLE|BUSY|ERROR)

### 01 · OUTBOUND_CMD (WMS→WCS)
`taskId`, `items[]`: { `PalletId`, `itemCode`, `lotId`, `pickQty` }
- WMS는 EqpPalletId를 모름. PalletId 기준으로만 요청. WCS가 매핑에서 PalletId→EqpPalletId 조회.

### 02 · OUTBOUND_CMD_ACK (WCS→WMS)
`taskId`, `result`(ACCEPTED|REJECTED), `message`
- Case A: 매핑 조회만. Case B: 우선순위로 팔렛 선택. 재고부족/매핑미존재 → REJECTED.

### 02-A · OUTBOUND_MISMATCH (WCS→WMS, 비동기, Case A 전용, 오류 시만)
`refMessageId`(=OUTBOUND_CMD), `taskId`, `palletId`, `wcsSkuCode`, `wcsQty`, `message`
- WCS 보유 SKU·수량만 전달. WMS가 자기 데이터와 비교 판단. 해당 지시 WCS에서 중단.

### 02-B · OUTBOUND_CONFIRM_REQ (WCS→WMS, 동기, Case B 전용)
`refMessageId`(=OUTBOUND_CMD), `taskId`, `palletId`, `skuCode`, `lotId`, `qty`, `expireDate`
- 팔렛 단위 확인. EqpPalletId 미포함. WMS가 일치 확인 후 ACK/NACK.

### 02-B · OUTBOUND_CONFIRM_ACK (WMS→WCS, 동기)
`refMessageId`(=CONFIRM_REQ), `taskId`, `result`(ACK|NACK), `message`
- ACK → OUTBOUND_TASK 진행. NACK → 대체 팔렛 재선정 후 재요청. 반복 후에도 없으면 CMD_ACK REJECTED.

### 03 · OUTBOUND_TASK (WCS→RCS)
`wcsTaskId`(=eqpPalletId-cycleNo), `taskType`(OUTBOUND), `eqpPalletId`, `destStation`

### 04 · OUTBOUND_TASK_ACK (RCS→WCS)
`wcsTaskId`, `shuttleId`, `result`, `message`

### 05 · OUTBOUND_DONE (RCS→WCS)
`wcsTaskId`, `shuttleId`, `eqpPalletId`, `destStation`, `status`(COMPLETED|FAILED), `failReason`
- 이 시점에 EqpPallet location→PICKING_ZONE. 즉시 DONE_ACK 후 WMS에 OUTBOUND_COMPLETE 발송.

### 06 · OUTBOUND_DONE_ACK (WCS→RCS)
`wcsTaskId`, `result`(OK), `message`

### 07 · OUTBOUND_COMPLETE (WCS→WMS, 비동기) — 미구현
`taskId`, `palletId`(WMS 기준, EqpPalletId 미포함), `itemCode`, `lotId`, `outQty`(전량 출고수량), `destStation`, `status`, `message`

### 07-E · OUTBOUND_ORDER_COMPLETE (WCS→WMS, 비동기) — 미구현
`taskId`, `totalPalletCount`, `completedPallets[]`, `status`(COMPLETED|PARTIAL)
- WCS가 taskId별 완료 팔렛 수 카운트 → 전부 완료 시 자동 발송. OUTBOUND_COMPLETE 마지막 건 직후 연속.

### 08 · PICKING_REPORT (WCS→WMS, 비동기) — 미구현
`palletId`, `itemCode`, `lotId`, `pickedQty`(박스 단위), `remainQty`, `operatorId`
- EqpPalletId 미포함. remainQty=0 → 재입고 없음, WCS가 EqpPallet EMPTY 처리.

### 09 · PICKING_REPORT_ACK (WMS→WCS, 비동기) — 미구현
`refMessageId`(=PICKING_REPORT), `result`(ACK|NACK), `palletId`, `needLabelReissue`(bool), `message`

### 10 · INVENTORY_ADJUSTMENT (WCS→WMS, 동기) — 미구현
실물 수량 불일치 시 재고 보정 요청. PalletId 기준. 타임아웃 시 예외+수동 재시도.

---

## 3. 예외 처리 (EX)

기본 원칙: 모든 예외는 해당 작업 중단 + 알람. 담당자 확인·조치 후 수동 재개. 초기 운영은 보수적 전면 중단.

| NO | 예외 | 발생 시점 | 조치 / 중단 범위 |
|----|------|-----------|------------------|
| EX-01 | RCS task REJECTED | API 04 result=REJECTED | RCS 상태 확인 → API 03 재발송. 해당 palletId task |
| EX-02 | RCS 완료 FAILED | API 05 status=FAILED | failReason 확인 → 설비 점검 → API 03 재발송/취소. 해당 task |
| EX-03 | WCS 재고 처리 실패 | API 05 수신 후 | DB 장애→전체 중단 / 로직 오류→수동 완료. API 06 미반환 시 RCS 타임아웃 처리 TBD |
| EX-04 | 출고 재고 부족 | STEP 02 팔렛 선택 | API 02 REJECTED → 재고 대사 → 재발송. 해당 skuCode task |
| EX-05 | 완료 통보 미수신(타임아웃) | API 04 ACK 이후 | 팔렛 물리 위치 확인 → 수동 완료/취소. 타임아웃 기준 RCS 협의 TBD |
| EX-06 | 스테이션 ERROR | STATION_STATUS status=ERROR | 해당 stationId 신규 task 중단 → 복구 후 AVAILABLE 수신 시 재개 |
| EX-07 | BCR 미매칭 | API 04 result=REJECTED | barcode 확인 → 미등록 팔렛이면 WMS 지시 등록 후 재처리. 해당 스테이션 |
| EX-08 | PalletId 중복 입고 지시 | PRE03 매핑 등록 시점 | (입고측) 중복 taskId+palletId 재수신 409 |

---

## 4. 구현 매핑 (2026-07 기준)

| 구간 | 상태 | 위치 |
|------|------|------|
| PRE STATION_STATUS(출고) | ✅ | RcsInboundService.receiveStationStatus (범용) |
| 01 OUTBOUND_CMD (Case A) | ✅ | wcs-app OutboundCmdConsumer → shuttle-wcs OutboundOrderService |
| 02 OUTBOUND_CMD_ACK | ✅ | OutboundOrderService(매핑+예약 검증) → OutboundCmdAckProducer |
| 02-A OUTBOUND_MISMATCH | ❌ 미구현 | 재고 대사(wcs_pallet_line_h vs CMD) |
| 02-B CONFIRM (Case B) | ❌ 미구현 | Case B 자체 미구현 |
| 03/04 OUTBOUND_TASK/ACK | ✅ | UserOutboundRequestService (작업자 출고 요청 트리거 /internal/user-outbound-request) |
| OUTBOUND_START (설비 착수) | ✅ (설계 확장) | RcsOutboundService.receiveOutboundStart (IN_RACK→OUTBOUNDING) |
| 05/06 OUTBOUND_DONE/ACK | ✅ | RcsOutboundService.receiveOutboundDone |
| 07 OUTBOUND_COMPLETE | ❌ 미구현 | WMS 팔렛 단위 완료 통보 |
| 07-E ORDER_COMPLETE | ❌ 미구현 | taskId 단위 완료 집계 |
| 08/09 PICKING_REPORT/ACK | ❌ 미구현 | 피킹존 |
| 10 INVENTORY_ADJUSTMENT | ❌ 미구현 | 재고 보정 |
| 재고 예약 모델 | ✅ (설계 확장) | reserved_qty. `docs/04-flows.md` 참조 |

> **원문에 없는 이 프로젝트 확장/조정 설계** (→ 상세 `docs/04-flows.md`):
> - 재고 예약 모델(reserved_qty, available=quantity-reserved_qty, CMD 예약/DONE 소멸)
> - 작업자 "출고 시작" 트리거(RECEIVED 지시 일괄 OUTBOUND_TASK 발행)
> - **INBOUND_START / OUTBOUND_START**(설비 물리 착수 이벤트) — 착수 시점에
>   입고는 스테이션 해제(BUSY→AVAILABLE), 출고는 랙 이탈(IN_RACK→OUTBOUNDING)
