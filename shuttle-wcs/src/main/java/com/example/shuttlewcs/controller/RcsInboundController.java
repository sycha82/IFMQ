package com.example.shuttlewcs.controller;

import com.example.common.dto.BcrReadDto;
import com.example.common.dto.InboundDoneAckDto;
import com.example.common.dto.InboundDoneDto;
import com.example.common.dto.InboundStartDto;
import com.example.common.dto.StationStatusDto;
import com.example.shuttlewcs.service.RcsInboundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// RCS/설비ECS → shuttle-wcs 수신용 API (STATION_STATUS · BCR_READ)
@RestController
@RequestMapping("/rcs")
@RequiredArgsConstructor
public class RcsInboundController {

    private final RcsInboundService rcsInboundService;

    // API 01 · STATION_STATUS
    @PostMapping("/station-status")
    public ResponseEntity<String> stationStatus(@RequestBody StationStatusDto dto) {
        rcsInboundService.receiveStationStatus(dto);
        return ResponseEntity.ok("STATION_STATUS 반영 | stationId=" + dto.getStationId()
                + " status=" + dto.getStatus());
    }

    // API 02 · BCR_READ
    @PostMapping("/bcr-read")
    public ResponseEntity<String> bcrRead(@RequestBody BcrReadDto dto) {
        rcsInboundService.receiveBcrRead(dto);
        return ResponseEntity.ok("BCR_READ 처리 | stationId=" + dto.getStationId()
                + " eqpPalletId=" + dto.getEqpPalletId());
    }

    // INBOUND_START · 입고 착수 통보 → 스테이션 해제
    @PostMapping("/inbound-start")
    public ResponseEntity<String> inboundStart(@RequestBody InboundStartDto dto) {
        rcsInboundService.receiveInboundStart(dto);
        return ResponseEntity.ok("INBOUND_START 처리 · 스테이션 해제 | stationId=" + dto.getStationId()
                + " eqpPalletId=" + dto.getEqpPalletId());
    }

    // API 05 · INBOUND_DONE → 응답이 곧 API 06 INBOUND_DONE_ACK
    @PostMapping("/inbound-done")
    public InboundDoneAckDto inboundDone(@RequestBody InboundDoneDto dto) {
        return rcsInboundService.receiveInboundDone(dto);
    }
}
