package com.example.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

// API 04 · RCS → WCS · OUTBOUND_TASK 동기 응답 ACK (배정 shuttleId 포함)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class OutboundTaskAckDto extends WcsMessageBase {

    private String wcsTaskId;
    private String shuttleId;  // RCS 배정 셔틀 번호 (모니터링용)
    private String result;     // ACCEPTED | REJECTED
    private String message;
}
