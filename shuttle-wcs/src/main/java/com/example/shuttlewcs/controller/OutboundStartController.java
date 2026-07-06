package com.example.shuttlewcs.controller;

import com.example.shuttlewcs.service.OutboundStartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// wcs-app(출고 시작 CLI) → shuttle-wcs 위임 호출용 내부 API
// 수신된(RECEIVED) 출고 지시를 순차적으로 OUTBOUND_TASK 발행한다.
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class OutboundStartController {

    private final OutboundStartService outboundStartService;

    @PostMapping("/outbound-start")
    public ResponseEntity<String> startOutbound() {
        return ResponseEntity.ok(outboundStartService.startOutbound());
    }
}
