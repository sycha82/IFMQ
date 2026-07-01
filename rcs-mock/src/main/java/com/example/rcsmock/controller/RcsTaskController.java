package com.example.rcsmock.controller;

import com.example.common.dto.InboundTaskAckDto;
import com.example.common.dto.InboundTaskDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

// Shuttle-WCS → rcs-mock 수신용 API (INBOUND_TASK) — RCS/설비ECS 입장에서 명령 수신
@Slf4j
@RestController
public class RcsTaskController {

    // 데모용 셔틀 배정 로직 — 3대를 순환 배정
    private static final String[] SHUTTLES = {"SHUTTLE-01", "SHUTTLE-02", "SHUTTLE-03"};
    private final AtomicInteger shuttleCursor = new AtomicInteger(0);

    // API 03 · INBOUND_TASK 수신 → API 04 INBOUND_TASK_ACK 동기 응답
    @PostMapping("/rcs/inbound-task")
    public InboundTaskAckDto receiveInboundTask(@RequestBody InboundTaskDto dto) {
        System.out.println("\n[RCS 수신] INBOUND_TASK");
        System.out.println("  wcsTaskId  : " + dto.getWcsTaskId());
        System.out.println("  stationId  : " + dto.getStationId());
        System.out.println("  eqpPalletId: " + dto.getEqpPalletId());
        System.out.printf("  itemCode=%s lotId=%s qty=%d expireDate=%s%n",
                dto.getItemCode(), dto.getLotId(), dto.getQty(), dto.getExpireDate());

        String shuttleId = SHUTTLES[shuttleCursor.getAndIncrement() % SHUTTLES.length];

        InboundTaskAckDto ack = InboundTaskAckDto.builder()
                .messageType("INBOUND_TASK_ACK")
                .messageId(UUID.randomUUID().toString())
                .refMessageId(dto.getMessageId())
                .sequenceNo(1)
                .timestamp(LocalDateTime.now())
                .wcsTaskId(dto.getWcsTaskId())
                .shuttleId(shuttleId)
                .result("ACCEPTED")
                .message("")
                .build();

        log.info("[RCS] INBOUND_TASK_ACK 회신 | wcsTaskId={} shuttleId={}", ack.getWcsTaskId(), shuttleId);
        System.out.println("  → ACK 회신: shuttleId=" + shuttleId);
        System.out.print("선택 > ");

        return ack;
    }
}
