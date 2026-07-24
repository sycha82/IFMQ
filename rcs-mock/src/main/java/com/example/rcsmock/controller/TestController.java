package com.example.rcsmock.controller;

import com.example.common.dto.BcrReadDto;
import com.example.common.dto.InboundDoneAckDto;
import com.example.common.dto.InboundDoneDto;
import com.example.common.dto.InboundStartDto;
import com.example.common.dto.OutboundDoneAckDto;
import com.example.common.dto.OutboundDoneDto;
import com.example.common.dto.OutboundStartDto;
import com.example.common.dto.StationStatusDto;
import com.example.rcsmock.client.ShuttleWcsRcsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

// 도커 컨테이너 등 대화형 CLI 사용이 불편한 환경을 위한 REST 발행 엔드포인트.
// RcsCliRunner 메뉴와 1:1 대응 (기본값도 동일).
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final ShuttleWcsRcsClient shuttleWcsRcsClient;

    @PostMapping("/station-status-in")
    public ResponseEntity<String> sendStationStatusIn(@RequestBody(required = false) StationStatusDto body) {
        StationStatusDto dto = (body != null) ? body : defaultStationStatusIn();
        String resp = shuttleWcsRcsClient.sendStationStatus(dto);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/station-status-out")
    public ResponseEntity<String> sendStationStatusOut(@RequestBody(required = false) StationStatusDto body) {
        StationStatusDto dto = (body != null) ? body : defaultStationStatusOut();
        String resp = shuttleWcsRcsClient.sendStationStatus(dto);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/bcr-read")
    public ResponseEntity<String> sendBcrRead(@RequestBody(required = false) BcrReadDto body) {
        BcrReadDto dto = (body != null) ? body : defaultBcrRead();
        String resp = shuttleWcsRcsClient.sendBcrRead(dto);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/inbound-start")
    public ResponseEntity<String> sendInboundStart(@RequestBody(required = false) InboundStartDto body) {
        InboundStartDto dto = (body != null) ? body : defaultInboundStart();
        String resp = shuttleWcsRcsClient.sendInboundStart(dto);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/inbound-done")
    public InboundDoneAckDto sendInboundDone(@RequestBody(required = false) InboundDoneDto body) {
        InboundDoneDto dto = (body != null) ? body : defaultInboundDone();
        return shuttleWcsRcsClient.sendInboundDone(dto);
    }

    @PostMapping("/outbound-start")
    public ResponseEntity<String> sendOutboundStart(@RequestBody(required = false) OutboundStartDto body) {
        OutboundStartDto dto = (body != null) ? body : defaultOutboundStart();
        String resp = shuttleWcsRcsClient.sendOutboundStart(dto);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/outbound-done")
    public OutboundDoneAckDto sendOutboundDone(@RequestBody(required = false) OutboundDoneDto body) {
        OutboundDoneDto dto = (body != null) ? body : defaultOutboundDone();
        return shuttleWcsRcsClient.sendOutboundDone(dto);
    }

    private StationStatusDto defaultStationStatusIn() {
        return StationStatusDto.builder()
                .messageType("STATION_STATUS")
                .messageId("RCS-STS-0001")
                .refMessageId(null)
                .sequenceNo(1)
                .timestamp(LocalDateTime.of(2026, 4, 22, 9, 20, 0))
                .stationId("STATION-IN-01")
                .stationType("INBOUND")
                .status("AVAILABLE")
                .build();
    }

    private StationStatusDto defaultStationStatusOut() {
        return StationStatusDto.builder()
                .messageType("STATION_STATUS")
                .messageId("RCS-STS-OUT-0001")
                .refMessageId(null)
                .sequenceNo(1)
                .timestamp(LocalDateTime.of(2026, 4, 22, 9, 20, 0))
                .stationId("STATION-OUT-01")
                .stationType("OUTBOUND")
                .status("AVAILABLE")
                .build();
    }

    private InboundStartDto defaultInboundStart() {
        return InboundStartDto.builder()
                .messageType("INBOUND_START")
                .messageId("RCS-IN-START-0001")
                .refMessageId(null)
                .sequenceNo(1)
                .timestamp(LocalDateTime.of(2026, 4, 22, 9, 25, 0))
                .stationId("STATION-IN-01")
                .eqpPalletId("EP0001")
                .wcsTaskId("EP0001-1")
                .shuttleId("SHUTTLE-01")
                .build();
    }

    private BcrReadDto defaultBcrRead() {
        return BcrReadDto.builder()
                .messageType("BCR_READ")
                .messageId("RCS-BCR-0001")
                .refMessageId(null)
                .sequenceNo(1)
                .timestamp(LocalDateTime.of(2026, 4, 22, 9, 21, 0))
                .stationId("STATION-IN-01")
                .eqpPalletId("EP0001")
                .build();
    }

    private InboundDoneDto defaultInboundDone() {
        return InboundDoneDto.builder()
                .messageType("INBOUND_DONE")
                .messageId("RCS-DONE-0001")
                .refMessageId(null)
                .sequenceNo(1)
                .timestamp(LocalDateTime.of(2026, 4, 22, 9, 30, 0))
                .wcsTaskId("EP0001-1")
                .eqpPalletId("EP0001")
                .shuttleId("SHUTTLE-01")
                .status("COMPLETED")
                .failReason(null)
                .build();
    }

    private OutboundStartDto defaultOutboundStart() {
        return OutboundStartDto.builder()
                .messageType("OUTBOUND_START")
                .messageId("RCS-OUT-START-0001")
                .refMessageId(null)
                .sequenceNo(1)
                .timestamp(LocalDateTime.of(2026, 4, 22, 10, 5, 0))
                .wcsTaskId("EQPPLT-OUT-01-1")
                .eqpPalletId("EQPPLT-OUT-01")
                .shuttleId("SHUTTLE-01")
                .destStation("STATION-OUT-01")
                .build();
    }

    private OutboundDoneDto defaultOutboundDone() {
        return OutboundDoneDto.builder()
                .messageType("OUTBOUND_DONE")
                .messageId("RCS-OUT-DONE-0001")
                .refMessageId(null)
                .sequenceNo(1)
                .timestamp(LocalDateTime.of(2026, 4, 22, 10, 8, 15))
                .wcsTaskId("EQPPLT-OUT-01-1")
                .eqpPalletId("EQPPLT-OUT-01")
                .shuttleId("SHUTTLE-01")
                .destStation("STATION-OUT-01")
                .status("COMPLETED")
                .failReason(null)
                .build();
    }
}
