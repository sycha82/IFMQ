package com.example.shuttlewcs.db;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// ① wcs_inbound_order_h — WMS INBOUND_CMD 수신 헤더 (1 task × N pallet)
@Getter
@Setter
@Builder
public class WcsInboundOrderH {

    // PK
    private String taskId;
    private String palletId;

    private String cmdStatus;          // RECEIVED | MAPPED | COMPLETED | CANCELLED
    private String recvMessageId;
    private String lastMessageId;
    private Integer recvCount;

    private LocalDateTime receivedAt;
    private LocalDateTime qtyUpdatedAt;
    private LocalDateTime mappedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;

    // 감사 컬럼
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
