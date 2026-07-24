package com.example.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

// RCS → WCS · 출고 착수 통보 — 셔틀이 랙에서 팔렛을 집어 출고 작업을 시작한 시점.
// 이 시점에 팔렛이 랙을 떠나므로 WCS는 location을 IN_RACK → OUTBOUNDING 으로 전이한다.
// (입고 INBOUND_START 대칭. 출고는 출발지가 스테이션이 아니라 랙이라 stationId 대신 destStation)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class OutboundStartDto extends WcsMessageBase {

    private String wcsTaskId;
    private String eqpPalletId;
    private String shuttleId;
    private String destStation;
}
