package com.example.shuttlewcs.service;

import com.example.common.dto.OutboundCmdAckDto;
import com.example.common.dto.OutboundCmdDto;
import com.example.common.dto.OutboundCmdItem;
import com.example.shuttlewcs.db.WcsEqpPalletMap;
import com.example.shuttlewcs.db.WcsEqpPalletMapMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboundOrderService {

    // 출고 가능(랙 적재 완료) 상태
    private static final String SHIPPABLE_MAP_STATUS = "STORED";

    private final WcsEqpPalletMapMapper eqpPalletMapMapper;

    /**
     * OUTBOUND_CMD (API 01) 수신 — Case A · PalletId 지정.
     * WCS는 PalletId → EqpPalletId 매핑을 조회하여 출고 가능 여부만 판정하고
     * 즉시 OUTBOUND_CMD_ACK (API 02) 를 생성해 반환한다.
     * 매핑 미존재 / 팔렛트 미일치 / 출고 불가 상태이면 REJECTED.
     */
    @Transactional(readOnly = true)
    public OutboundCmdAckDto receiveOutboundOrder(OutboundCmdDto dto) {
        List<OutboundCmdItem> items = dto.getItems();
        if (items == null || items.isEmpty()) {
            return buildAck(dto, "REJECTED", "items가 비어 있음");
        }

        List<String> reasons = new ArrayList<>();
        for (OutboundCmdItem item : items) {
            WcsEqpPalletMap map = eqpPalletMapMapper.findByPalletId(item.getPalletId());
            if (map == null || map.getPalletId() == null) {
                reasons.add("매핑 미존재 palletId=" + item.getPalletId());
                continue;
            }
            if (!SHIPPABLE_MAP_STATUS.equals(map.getMapStatus())) {
                reasons.add("출고 불가 상태 palletId=" + item.getPalletId()
                        + " mapStatus=" + map.getMapStatus());
            }
        }

        if (!reasons.isEmpty()) {
            log.warn("[OUTBOUND_ORDER] REJECTED | taskId={} 사유={}", dto.getTaskId(), reasons);
            return buildAck(dto, "REJECTED", String.join("; ", reasons));
        }

        log.info("[OUTBOUND_ORDER] ACCEPTED | taskId={} itemCount={}", dto.getTaskId(), items.size());
        return buildAck(dto, "ACCEPTED", "");
    }

    private OutboundCmdAckDto buildAck(OutboundCmdDto dto, String result, String message) {
        return OutboundCmdAckDto.builder()
                .messageType("OUTBOUND_CMD_ACK")
                .messageId(UUID.randomUUID().toString())
                .refMessageId(dto.getMessageId())
                .sequenceNo(1)
                .timestamp(LocalDateTime.now())
                .taskId(dto.getTaskId())
                .result(result)
                .message(message)
                .build();
    }
}
