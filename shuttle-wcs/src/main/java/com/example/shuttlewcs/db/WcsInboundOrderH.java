package com.example.shuttlewcs.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WcsInboundOrderH {

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
