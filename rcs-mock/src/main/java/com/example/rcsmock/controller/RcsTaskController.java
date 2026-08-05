package com.example.rcsmock.controller;

import com.example.common.dto.InboundTaskAckDto;
import com.example.common.dto.InboundTaskDto;
import com.example.common.dto.OutboundTaskAckDto;
import com.example.common.dto.OutboundTaskDto;
import com.example.rcsmock.sim.EquipmentSimulator;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class RcsTaskController {

    // 데모용 셔틀 배정 로직 — 3대를 순환 배정
    private static final String[] SHUTTLES = {"SHUTTLE-01", "SHUTTLE-02", "SHUTTLE-03"};
    private final AtomicInteger shuttleCursor = new AtomicInteger(0);

    private final EquipmentSimulator equipmentSimulator;

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

        // 설비 동작 자동 시뮬레이션 예약 (예약만 하고 즉시 ACK 반환 — 블로킹 금지)
        equipmentSimulator.scheduleInbound(dto, shuttleId);

        return ack;
    }

    // API 03 · OUTBOUND_TASK 수신 → API 04 OUTBOUND_TASK_ACK 동기 응답
    @PostMapping("/rcs/outbound-task")
    public OutboundTaskAckDto receiveOutboundTask(@RequestBody OutboundTaskDto dto) {
        System.out.println("\n[RCS 수신] OUTBOUND_TASK");
        System.out.println("  wcsTaskId  : " + dto.getWcsTaskId());
        System.out.println("  taskType   : " + dto.getTaskType());
        System.out.println("  eqpPalletId: " + dto.getEqpPalletId());
        System.out.println("  destStation: " + dto.getDestStation());

        String shuttleId = SHUTTLES[shuttleCursor.getAndIncrement() % SHUTTLES.length];

        OutboundTaskAckDto ack = OutboundTaskAckDto.builder()
                .messageType("OUTBOUND_TASK_ACK")
                .messageId(UUID.randomUUID().toString())
                .refMessageId(dto.getMessageId())
                .sequenceNo(1)
                .timestamp(LocalDateTime.now())
                .wcsTaskId(dto.getWcsTaskId())
                .shuttleId(shuttleId)
                .result("ACCEPTED")
                .message("")
                .build();

        log.info("[RCS] OUTBOUND_TASK_ACK 회신 | wcsTaskId={} shuttleId={}", ack.getWcsTaskId(), shuttleId);
        System.out.println("  → ACK 회신: shuttleId=" + shuttleId);
        System.out.print("선택 > ");

        // 설비 동작 자동 시뮬레이션 예약 (예약만 하고 즉시 ACK 반환 — 블로킹 금지)
        equipmentSimulator.scheduleOutbound(dto, shuttleId);

        return ack;
    }
}
