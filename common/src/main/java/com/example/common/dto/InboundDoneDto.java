package com.example.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

// API 05 · RCS → WCS · 셀 입고 완료 통보 (wcsTaskId로 INBOUND_TASK 역추적)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class InboundDoneDto extends WcsMessageBase {

    private String wcsTaskId;
    private String eqpPalletId;
    private String shuttleId;
    private String status;      // COMPLETED | FAILED
    private String failReason;
}
