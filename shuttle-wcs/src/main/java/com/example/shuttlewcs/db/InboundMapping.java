package com.example.shuttlewcs.db;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

// EqpPalletId ↔ PalletId 매핑. EqpPalletId 기준 사이클링 재사용 (덮어쓰기)
@Getter
@Setter
@Builder
public class InboundMapping {

    private String eqpPalletId;
    private String palletId;
    private String skuCode;
    private String lotId;
    private Integer qty;
    private LocalDate expireDate;
    private String status;          // PENDING | MAPPED | IN_PROGRESS | STORED
    private String wcsTaskId;
    private LocalDateTime mappedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
}
