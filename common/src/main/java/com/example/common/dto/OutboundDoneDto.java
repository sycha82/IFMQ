package com.example.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

// API 05 · RCS → WCS · 출고 스테이션 배출 완료 통보 (wcsTaskId로 OUTBOUND_TASK 역추적)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class OutboundDoneDto extends WcsMessageBase {

    private String wcsTaskId;
    private String eqpPalletId;
    private String shuttleId;
    private String destStation;
    private String status;      // COMPLETED | FAILED
    private String failReason;
}
