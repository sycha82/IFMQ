package com.example.shuttlewcs.controller;

import com.example.common.dto.InboundCmdDto;
import com.example.shuttlewcs.service.InboundOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// wcs-app(INBOUND_CMD 수신) → shuttle-wcs 위임 호출용 내부 API
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InboundOrderController {

    private final InboundOrderService inboundOrderService;

    @PostMapping("/inbound-order")
    public ResponseEntity<Void> receiveInboundOrder(@RequestBody InboundCmdDto dto) {
        inboundOrderService.receiveInboundOrder(dto);
        return ResponseEntity.ok().build();
    }
}
