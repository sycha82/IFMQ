package com.example.shuttlewcs.db;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

// INBOUND_CMD 수신 적재. PRE03 매핑 등록 전 PENDING 단계
@Getter
@Setter
@Builder
public class InboundCmd {

    private String palletId;
    private String taskId;
    private String skuCode;
    private String lotId;
    private Integer qty;
    private LocalDate expireDate;
    private String status;       // PENDING | MAPPED
    private LocalDateTime receivedAt;
    private LocalDateTime updatedAt;
}
