package com.example.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

// OUTBOUND_CMD_ACK (WCS → WMS) · API 02 — 출고 지시 수신 응답
// Case A : 매핑 조회 결과에 따라 ACCEPTED / REJECTED. refMessageId = OUTBOUND_CMD messageId
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class OutboundCmdAckDto extends WcsMessageBase {

    private String taskId;
    private String result;    // ACCEPTED | REJECTED
    private String message;
}
