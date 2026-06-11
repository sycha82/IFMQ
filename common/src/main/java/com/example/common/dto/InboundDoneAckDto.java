package com.example.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

// API 06 · WCS → RCS · INBOUND_DONE 동기 응답 ACK (DB 갱신 완료 후 반환)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class InboundDoneAckDto extends WcsMessageBase {

    private String wcsTaskId;
    private String result;   // OK | FAILED
    private String message;
}
