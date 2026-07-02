package com.example.shuttlewcs.controller;

import com.example.common.dto.OutboundCmdAckDto;
import com.example.common.dto.OutboundCmdDto;
import com.example.shuttlewcs.service.OutboundOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// wcs-app(OUTBOUND_CMD 수신) → shuttle-wcs 위임 호출용 내부 API
// 매핑 조회 후 OUTBOUND_CMD_ACK(ACCEPTED/REJECTED)를 반환한다.
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class OutboundOrderController {

    private final OutboundOrderService outboundOrderService;

    @PostMapping("/outbound-order")
    public OutboundCmdAckDto receiveOutboundOrder(@RequestBody OutboundCmdDto dto) {
        return outboundOrderService.receiveOutboundOrder(dto);
    }
}
