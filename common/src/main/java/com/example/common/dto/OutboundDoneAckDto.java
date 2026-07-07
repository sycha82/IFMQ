package com.example.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

// API 06 · WCS → RCS · OUTBOUND_DONE 동기 응답 ACK
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class OutboundDoneAckDto extends WcsMessageBase {

    private String wcsTaskId;
    private String result;     // OK | FAILED
    private String message;
}
