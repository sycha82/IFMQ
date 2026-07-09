package com.example.shuttlewcs.service;

import com.example.shuttlewcs.db.WcsInboundOrderH;
import com.example.shuttlewcs.db.WcsInboundOrderHMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InboundStartService {

    private final WcsInboundOrderHMapper orderHMapper;

    /**
     * 입고 시작 — 현재는 상태 조회/로깅만 수행하는 placeholder.
     * 입고는 팔렛이 스테이션에 실제 도착해 BCR_READ가 팔렛 단위로 자동 INBOUND_TASK를
     * 발행하는 구조라 출고처럼 일괄 발송할 대상이 없다. 추후 입고 지시 단위 상태
     * 업데이트(예: 작업자 착수 확인, 알림 등)가 필요해지면 여기에 로직을 채운다.
     */
    public String startInbound() {
        List<WcsInboundOrderH> orders = orderHMapper.findByStatus("RECEIVED");

        String summary = String.format("입고 시작 | 대상지시=%d건 (TODO: 상태 업데이트 로직 미구현)",
                orders.size());
        log.info("[INBOUND_START] {}", summary);
        return summary;
    }
}
