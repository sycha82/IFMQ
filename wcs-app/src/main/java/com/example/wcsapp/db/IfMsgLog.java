package com.example.wcsapp.db;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class IfMsgLog {

    private Long logId;
    private String direction;       // INBOUND | OUTBOUND
    private String messageType;
    private String messageId;
    private String refMessageId;
    private Integer sequenceNo;
    private LocalDateTime msgTimestamp;
    private String routingKey;
    private String queueName;
    private String payload;         // JSON 직렬화된 문자열
    private String status;          // RECEIVED | PROCESSING | COMPLETED | FAILED
    private String errorMsg;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
}
