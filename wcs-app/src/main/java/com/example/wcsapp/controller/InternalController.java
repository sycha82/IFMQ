package com.example.wcsapp.controller;

import com.example.common.dto.InboundCompleteDto;
import com.example.wcsapp.producer.InboundCompleteProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// shuttle-wcs(입고 비즈니스) → wcs-app(MQ 어댑터) 위임 수신용 내부 API
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final InboundCompleteProducer inboundCompleteProducer;

    // INBOUND_COMPLETE 발행 위임 — MQ 발행 + if_msg_log OUTBOUND 적재는 Producer가 처리
    @PostMapping("/inbound-complete")
    public ResponseEntity<Void> publishInboundComplete(@RequestBody InboundCompleteDto dto) {
        inboundCompleteProducer.send(dto);
        return ResponseEntity.ok().build();
    }
}
