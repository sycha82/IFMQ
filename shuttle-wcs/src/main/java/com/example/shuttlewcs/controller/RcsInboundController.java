package com.example.shuttlewcs.controller;

import com.example.common.dto.BcrReadDto;
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
}
