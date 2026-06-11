package com.example.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

// API 03 · WCS → RCS · 입고 Task 전달 (EqpPalletId 기준, 재고속성 포함)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class InboundTaskDto extends WcsMessageBase {

    private String wcsTaskId;
    private String stationId;
    private String eqpPalletId;
    private String result;   // ACCEPTED | REJECTED
    private String message;  // REJECTED 사유

    // ACCEPTED 시만 포함 — 매핑 테이블 조회값
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String itemCode;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String lotId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer qty;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDate expireDate;
}
