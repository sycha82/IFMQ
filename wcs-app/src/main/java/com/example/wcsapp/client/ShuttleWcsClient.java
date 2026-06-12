package com.example.wcsapp.client;

import com.example.common.dto.InboundCmdDto;
import com.example.wcsapp.config.ShuttleWcsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

// shuttle-wcs(입고 비즈니스 모듈) 내부 API 호출 클라이언트
@Slf4j
@Component
@RequiredArgsConstructor
public class ShuttleWcsClient {

    private static final String INBOUND_ORDER_PATH = "/internal/inbound-order";

    private final RestTemplate restTemplate;
    private final ShuttleWcsProperties shuttleWcsProperties;

    public void notifyInboundOrder(InboundCmdDto dto) {
        String url = shuttleWcsProperties.getBaseUrl() + INBOUND_ORDER_PATH;
        log.debug("[SHUTTLE-WCS] INBOUND_CMD 위임 호출 | url={} taskId={} palletId={}",
                url, dto.getTaskId(), dto.getPalletId());
        restTemplate.postForEntity(url, dto, Void.class);
    }
}
