package com.example.shuttlewcs.client;

import com.example.common.dto.InboundTaskAckDto;
import com.example.common.dto.InboundTaskDto;
import com.example.common.dto.OutboundTaskAckDto;
import com.example.common.dto.OutboundTaskDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// Shuttle-WCS → RCS/설비ECS(rcs-mock) 호출용 선언형 클라이언트
@FeignClient(name = "rcs-mock", url = "${wcs.rcs.base-url}")
public interface RcsClient {

    // API 03 · INBOUND_TASK — 동기 호출, 응답이 곧 API 04 INBOUND_TASK_ACK
    @PostMapping("/rcs/inbound-task")
    InboundTaskAckDto sendInboundTask(@RequestBody InboundTaskDto dto);

    // API 03 · OUTBOUND_TASK — 동기 호출, 응답이 곧 API 04 OUTBOUND_TASK_ACK
    @PostMapping("/rcs/outbound-task")
    OutboundTaskAckDto sendOutboundTask(@RequestBody OutboundTaskDto dto);
}
