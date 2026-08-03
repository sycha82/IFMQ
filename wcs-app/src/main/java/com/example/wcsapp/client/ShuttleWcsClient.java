package com.example.wcsapp.client;

import com.example.common.dto.InboundCmdDto;
import com.example.common.dto.OutboundCmdAckDto;
import com.example.common.dto.OutboundCmdDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// shuttle-wcs(입고 비즈니스 모듈) 내부 API 호출용 선언형 클라이언트
@FeignClient(name = "shuttle-wcs", url = "${wcs.shuttle.base-url}")
public interface ShuttleWcsClient {

    // INBOUND_CMD 수신분을 shuttle-wcs 입고 처리로 위임
    @PostMapping("/internal/inbound-order")
    void notifyInboundOrder(@RequestBody InboundCmdDto dto);

    // OUTBOUND_CMD 수신분을 shuttle-wcs 매핑 조회로 위임 → OUTBOUND_CMD_ACK 반환
    @PostMapping("/internal/outbound-order")
    OutboundCmdAckDto notifyOutboundOrder(@RequestBody OutboundCmdDto dto);

    // 사용자 출고 요청(작업자 트리거) — 수신된(RECEIVED) 출고 지시를 순차 OUTBOUND_TASK 발행 위임
    // (RCS→WCS 이벤트 OUTBOUND_START 와 구분해 user-outbound-request 로 명명)
    @PostMapping("/internal/user-outbound-request")
    String requestOutbound();
}
