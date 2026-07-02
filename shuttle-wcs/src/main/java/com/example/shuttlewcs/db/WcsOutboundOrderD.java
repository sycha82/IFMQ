package com.example.shuttlewcs.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 출고 지시 상세 — OUTBOUND_CMD items 원본 라인 + PalletId 라이프사이클 (팔렛트 전체 출고)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WcsOutboundOrderD {

    private String taskId;
    private String palletId;
    private String skuCode;
    private String lotId;
    private Integer lineNo;
    private Integer qty;          // 출고 요청 수량 (pickQty)
    private LocalDate expireDate;

    private LocalDateTime mappedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
