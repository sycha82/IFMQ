package com.example.shuttlewcs.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// ⑦ wcs_rcs_msg_log — RCS/설비ECS 연계 REST API 송수신 이력 (WCS 기준)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WcsRcsMsgLog {

    private Long logId;
    private String direction;          // SEND | RECEIVE
    private String apiName;
    private String messageId;
    private String refMessageId;
    private String stationId;
    private String eqpPalletId;
    private String wcsTaskId;
    private String payload;            // JSON 직렬화된 문자열
    private String result;
    private LocalDateTime createdAt;
}
