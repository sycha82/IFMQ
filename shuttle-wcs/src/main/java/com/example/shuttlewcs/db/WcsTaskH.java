package com.example.shuttlewcs.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// ⑩ wcs_task_h — 셔틀 이동 작업(TASK) 이력 (INBOUND_TASK/OUTBOUND_TASK ~ START ~ DONE 요약)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WcsTaskH {

    private Long taskSeq;
    private String wcsTaskId;          // {eqpPalletId}-{cycleNo}
    private String taskType;           // INBOUND | OUTBOUND
    private String taskStatus;         // DISPATCHED | STARTED | COMPLETED | FAILED
    private String eqpPalletId;
    private Integer cycleNo;
    private String palletId;
    private String orderTaskId;        // WMS 지시 task_id
    private String stationId;          // 입고=출발 스테이션 / 출고=destStation
    private String shuttleId;
    private String skuCode;
    private String lotId;
    private Integer qty;
    private String taskMessageId;
    private String ackMessageId;
    private String ackResult;
    private String failReason;
    private LocalDateTime dispatchedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
