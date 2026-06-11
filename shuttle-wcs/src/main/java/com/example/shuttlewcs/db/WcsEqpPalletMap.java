package com.example.shuttlewcs.db;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// ④ wcs_eqp_pallet_map — EqpPallet ↔ PalletId 매핑 현재 상태 (사이클링 덮어쓰기)
@Getter
@Setter
@Builder
public class WcsEqpPalletMap {

    private String eqpPalletId;        // PK
    private String taskId;
    private String palletId;
    private String mapStatus;          // EMPTY | MAPPED | IN_PROGRESS | STORED | PICKING
    private String location;           // IDLE | STATION | IN_RACK | PICKING_ZONE
    private Integer cycleNo;

    private LocalDateTime mappedAt;
    private LocalDateTime storedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
