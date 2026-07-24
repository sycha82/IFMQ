package com.example.shuttlewcs.controller;

import com.example.common.dto.OutboundDoneAckDto;
import com.example.common.dto.OutboundDoneDto;
import com.example.common.dto.OutboundStartDto;
import com.example.shuttlewcs.service.RcsOutboundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// RCS/설비ECS → shuttle-wcs 수신용 API (OUTBOUND_START · OUTBOUND_DONE)
@RestController
@RequestMapping("/rcs")
@RequiredArgsConstructor
public class RcsOutboundController {

    private final RcsOutboundService rcsOutboundService;

    // OUTBOUND_START · 출고 착수 통보 → location IN_RACK → OUTBOUNDING 전이
    @PostMapping("/outbound-start")
    public ResponseEntity<String> outboundStart(@RequestBody OutboundStartDto dto) {
        rcsOutboundService.receiveOutboundStart(dto);
        return ResponseEntity.ok("OUTBOUND_START 처리 · OUTBOUNDING 전이 | wcsTaskId=" + dto.getWcsTaskId()
                + " eqpPalletId=" + dto.getEqpPalletId());
    }

    // API 05 · OUTBOUND_DONE → 응답이 곧 API 06 OUTBOUND_DONE_ACK
    @PostMapping("/outbound-done")
    public OutboundDoneAckDto outboundDone(@RequestBody OutboundDoneDto dto) {
        return rcsOutboundService.receiveOutboundDone(dto);
    }
}
