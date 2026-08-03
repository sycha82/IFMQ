package com.example.shuttlewcs.controller;

import com.example.shuttlewcs.service.UserOutboundRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// wcs-app(사용자 출고 요청 CLI) → shuttle-wcs 위임 호출용 내부 API
// 수신된(RECEIVED) 출고 지시를 순차적으로 OUTBOUND_TASK 발행한다.
// (RCS→WCS 이벤트 OUTBOUND_START(/rcs/outbound-start)와 이름 충돌을 피해 user-outbound-request 로 명명)
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class UserOutboundRequestController {

    private final UserOutboundRequestService userOutboundRequestService;

    @PostMapping("/user-outbound-request")
    public ResponseEntity<String> requestOutbound() {
        return ResponseEntity.ok(userOutboundRequestService.requestOutbound());
    }
}
