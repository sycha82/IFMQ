package com.example.shuttlewcs.controller;

import com.example.common.dto.OutboundDoneAckDto;
import com.example.common.dto.OutboundDoneDto;
import com.example.shuttlewcs.service.OutboundDoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// RCS/설비ECS → shuttle-wcs 수신용 API (OUTBOUND_DONE)
@RestController
@RequestMapping("/rcs")
@RequiredArgsConstructor
public class RcsOutboundController {

    private final OutboundDoneService outboundDoneService;

    // API 05 · OUTBOUND_DONE → 응답이 곧 API 06 OUTBOUND_DONE_ACK
    @PostMapping("/outbound-done")
    public OutboundDoneAckDto outboundDone(@RequestBody OutboundDoneDto dto) {
        return outboundDoneService.receiveOutboundDone(dto);
    }
}
