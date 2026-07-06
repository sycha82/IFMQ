package com.example.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

// API 03 · WCS → RCS · 출고 Task 전달 (EqpPalletId 기준, destStation = 출고 스테이션)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class OutboundTaskDto extends WcsMessageBase {

    private String wcsTaskId;
    private String taskType;      // OUTBOUND
    private String eqpPalletId;
    private String destStation;   // 출고 스테이션 ID
}
