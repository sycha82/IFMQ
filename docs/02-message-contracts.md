# 메시지 계약 (Contract)

## WMS↔WCS — RabbitMQ (Exchange `wcs.topic`)

| 흐름 | 라우팅 키 | 큐 | 메시지 |
|------|-----------|-----|--------|
| WMS→WCS | `wms.inbound.cmd` | `wcs.queue.inbound.cmd` | INBOUND_CMD (taskId + inboundDetail[]) |
| WMS→WCS | `wms.inbound.cancel` | `wcs.queue.inbound.cancel` | INBOUND_CANCEL |
| WCS→WMS | `wcs.inbound.complete` | `wms.queue.inbound.complete` | INBOUND_COMPLETE |
| WMS→WCS | `wms.outbound.cmd` | `wcs.queue.outbound.cmd` | OUTBOUND_CMD (taskId + items[]: palletId·itemCode·lotId·pickQty) |
| WCS→WMS | `wcs.outbound.cmd.ack` | `wms.queue.outbound.cmd.ack` | OUTBOUND_CMD_ACK (result=ACCEPTED\|REJECTED) |

- 라우팅 키/Exchange/큐 이름은 각 모듈 `application.yml`에서 관리(하드코딩 금지).
- Postman 등으로 큐에 직접 발행하려면 RabbitMQ Management API(`:15672`) 사용 — 자세한 건 `06-run-and-test.md`.

## WCS↔RCS — REST 동기 (요청의 응답이 곧 ACK)

| API | 방향 | 엔드포인트(shuttle-wcs 기준) | 비고 |
|-----|------|------------------------------|------|
| STATION_STATUS | RCS→WCS | POST /rcs/station-status | 입고/출고 공용, stationType으로 구분, upsert |
| BCR_READ | RCS→WCS | POST /rcs/bcr-read | 입고: 스캔 → 스테이션 점유(BUSY) + INBOUND_TASK 자동 발행 |
| INBOUND_TASK / ACK | WCS→RCS | POST {rcs}/rcs/inbound-task | wcsTaskId = eqpPalletId-cycleNo |
| INBOUND_START | RCS→WCS | POST /rcs/inbound-start | 설비 입고 착수(팔렛 스테이션 이탈) → 스테이션 해제(AVAILABLE) |
| INBOUND_DONE / ACK | RCS→WCS | POST /rcs/inbound-done | 완료 → 재고 적재 + INBOUND_COMPLETE 자동 통보 |
| OUTBOUND_TASK / ACK | WCS→RCS | POST {rcs}/rcs/outbound-task | eqpPalletId + destStation |
| OUTBOUND_START | RCS→WCS | POST /rcs/outbound-start | 설비 출고 착수(팔렛 랙 이탈) → location IN_RACK→OUTBOUNDING |
| OUTBOUND_DONE / ACK | RCS→WCS | POST /rcs/outbound-done | 배출 완료 → PICKING_ZONE + 재고 소멸 |

- 모든 DTO는 `common` 모듈에만 정의(타 모듈 복제 금지).
- `wcsTaskId` 포맷 = `{eqpPalletId}-{cycleNo}`.
