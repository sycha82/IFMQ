package com.example.shuttlewcs.controller;

import com.example.shuttlewcs.service.InboundStartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// wcs-app(입고 시작 CLI) → shuttle-wcs 위임 호출용 내부 API
// 현재는 placeholder — 수신된(RECEIVED) 입고 지시 조회/로깅만 수행 (출고 시작과 대칭 구조)
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InboundStartController {

    private final InboundStartService inboundStartService;

    @PostMapping("/inbound-start")
    public ResponseEntity<String> startInbound() {
        return ResponseEntity.ok(inboundStartService.startInbound());
    }
}
