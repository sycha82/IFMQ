package com.example.shuttlewcs.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// 출고 지시 헤더 — task 단위 메시지 추적 + 집계 결과 (WMS OUTBOUND_CMD)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WcsOutboundOrderH {

    private String taskId;
    private String cmdStatus;
    private String recvMessageId;
    private String lastMessageId;
    private Integer recvCount;
    private LocalDateTime receivedAt;
    private LocalDateTime completedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
