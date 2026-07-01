package com.example.shuttlewcs.client;

import com.example.common.dto.InboundCompleteDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// shuttle-wcs → wcs-app(MQ 어댑터) 호출용 — INBOUND_COMPLETE 발행 위임
@FeignClient(name = "wcs-app", url = "${wcs.app.base-url}")
public interface WcsAppClient {

    @PostMapping("/internal/inbound-complete")
    void notifyInboundComplete(@RequestBody InboundCompleteDto dto);
}
