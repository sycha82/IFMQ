package com.example.shuttlewcs.db;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// ⑥ wcs_station — 입고/출고 스테이션 상태 (RCS STATION_STATUS 반영)
@Getter
@Setter
@Builder
public class WcsStation {

    private String stationId;          // PK
    private String stationType;        // INBOUND | OUTBOUND
    private String status;             // AVAILABLE | BUSY | DOWN
    private String curEqpPalletId;
    private LocalDateTime statusChangedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
