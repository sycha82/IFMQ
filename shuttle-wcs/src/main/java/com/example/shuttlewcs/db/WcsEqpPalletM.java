package com.example.shuttlewcs.db;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// ③ wcs_eqp_pallet_m — EqpPallet 물리 풀 마스터
@Getter
@Setter
@Builder
public class WcsEqpPalletM {

    private String eqpPalletId;
    private Integer eqpPalletNo;
    private String palletStatus;       // EMPTY | IN_USE | MAINTENANCE
    private String useYn;              // Y | N
    private String maintReason;

    private LocalDateTime registeredAt;
    private LocalDateTime lastUsedAt;
    private String memo;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
