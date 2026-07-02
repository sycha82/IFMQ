package com.example.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// OUTBOUND_CMD 의 개별 출고 팔렛트 항목 (Case A · PalletId 지정)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboundCmdItem {

    private String palletId;    // 출고 대상 팔렛트 (WMS 기준 키)
    private String itemCode;
    private String lotId;
    private int pickQty;        // 출고(피킹) 요청 수량
}
