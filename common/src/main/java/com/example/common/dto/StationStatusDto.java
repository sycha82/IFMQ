package com.example.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

// API 01 · RCS → WCS · 입고 스테이션 상태 통보
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class StationStatusDto extends WcsMessageBase {

    private String stationId;
    private String stationType;  // INBOUND | OUTBOUND
    private String status;       // AVAILABLE | BUSY | ERROR
}
