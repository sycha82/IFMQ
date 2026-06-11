package com.example.shuttlewcs.db;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

// ② wcs_pallet_line_h — Pallet 적재 라인 (시계열, is_latest 플래그)
@Getter
@Setter
@Builder
public class WcsPalletLineH {

    // PK
    private String palletId;
    private LocalDateTime effectiveFrom;
    private String skuCode;
    private String lotId;              // lot 미관리 품목은 'N/A' sentinel

    private Integer qty;
    private LocalDate expireDate;
    private String isLatest;           // Y | N
    private LocalDateTime effectiveTo;
    private LocalDateTime supersededBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
