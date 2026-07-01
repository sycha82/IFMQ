package com.example.shuttlewcs.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// ⑤ wcs_eqp_pallet_map_h — 매핑 이력 (사이클링·상태 변경 보존)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WcsEqpPalletMapH {

    private Long hstSeq;
    private String eqpPalletId;
    private Integer cycleNo;
    private String taskId;
    private String palletId;
    private String mapStatus;
    private String location;
    private LocalDateTime mappedAt;
    private LocalDateTime storedAt;
    private String eventType;          // MAPPED | STATUS_CHANGE | LOCATION_CHANGE | CYCLE_END
    private LocalDateTime eventAt;
    private String eventBy;
    private String note;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
