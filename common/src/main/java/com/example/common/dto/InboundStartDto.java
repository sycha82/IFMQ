package com.example.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

// RCS → WCS · 입고 착수 통보 — 셔틀이 스테이션에서 팔렛을 집어 입고 작업을 시작한 시점.
// 이 시점에 팔렛이 스테이션을 떠나므로 WCS는 해당 스테이션을 AVAILABLE로 해제한다.
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class InboundStartDto extends WcsMessageBase {

    private String stationId;
    private String eqpPalletId;
    private String wcsTaskId;
    private String shuttleId;
}
