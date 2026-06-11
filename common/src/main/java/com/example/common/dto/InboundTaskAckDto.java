package com.example.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

// API 04 · RCS → WCS · INBOUND_TASK 동기 응답 ACK
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class InboundTaskAckDto extends WcsMessageBase {

    private String wcsTaskId;
    private String shuttleId;  // RCS 배정 셔틀 번호 (모니터링용)
    private String result;     // ACCEPTED | REJECTED
    private String message;
}
