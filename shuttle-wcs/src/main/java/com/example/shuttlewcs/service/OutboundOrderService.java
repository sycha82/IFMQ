package com.example.shuttlewcs.service;

import com.example.common.dto.OutboundCmdAckDto;
import com.example.common.dto.OutboundCmdDto;
import com.example.common.dto.OutboundCmdItem;
import com.example.shuttlewcs.db.WcsEqpPalletMap;
import com.example.shuttlewcs.db.WcsEqpPalletMapMapper;
import com.example.shuttlewcs.db.WcsOutboundOrderD;
import com.example.shuttlewcs.db.WcsOutboundOrderDMapper;
import com.example.shuttlewcs.db.WcsOutboundOrderH;
import com.example.shuttlewcs.db.WcsOutboundOrderHMapper;
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

    private static final String LOT_NOT_MANAGED = "N/A";
    // 출고 가능(랙 적재 완료) 상태
    private static final String SHIPPABLE_MAP_STATUS = "STORED";

    private final WcsEqpPalletMapMapper eqpPalletMapMapper;
    private final WcsOutboundOrderHMapper orderHMapper;
    private final WcsOutboundOrderDMapper orderDMapper;

    /**
     * OUTBOUND_CMD (API 01) 수신 — Case A · PalletId 지정.
     * PalletId → EqpPalletId 매핑을 조회해 출고 가능 여부를 판정하고, ACCEPTED 시
     * 출고 지시(H/D)를 적재한 뒤 즉시 OUTBOUND_CMD_ACK (API 02) 를 생성해 반환한다.
     * items 누락 / taskId 중복 재수신 / 매핑 미존재 / 출고 불가 상태이면 REJECTED (적재 없음).
     */
    @Transactional
    public OutboundCmdAckDto receiveOutboundOrder(OutboundCmdDto dto) {
        List<OutboundCmdItem> items = dto.getItems();
        if (items == null || items.isEmpty()) {
            return buildAck(dto, "REJECTED", "items가 비어 있음");
        }

        // 1. 중복 출고 지시 재수신 거부
        if (orderHMapper.findByTaskId(dto.getTaskId()) != null) {
            log.warn("[OUTBOUND_ORDER] REJECTED · 중복 재수신 | taskId={}", dto.getTaskId());
            return buildAck(dto, "REJECTED", "이미 존재하는 출고 지시 재수신 | taskId=" + dto.getTaskId());
        }

        // 2. Case A 매핑 조회 검증 (STORED 상태만 출고 가능)
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

        // 3. ACCEPTED — 출고 지시 헤더/상세 적재
        LocalDateTime now = LocalDateTime.now();
        orderHMapper.insert(WcsOutboundOrderH.builder()
                .taskId(dto.getTaskId())
                .cmdStatus("RECEIVED")
                .recvMessageId(dto.getMessageId())
                .lastMessageId(dto.getMessageId())
                .recvCount(1)
                .receivedAt(now)
                .build());

        int lineNo = 1;
        for (OutboundCmdItem item : items) {
            orderDMapper.insert(WcsOutboundOrderD.builder()
                    .taskId(dto.getTaskId())
                    .palletId(item.getPalletId())
                    .skuCode(item.getItemCode())
                    .lotId(resolveLotId(item.getLotId()))
                    .lineNo(lineNo++)
                    .qty(item.getPickQty())
                    .build());
        }

        log.info("[OUTBOUND_ORDER] ACCEPTED · 신규 적재 | taskId={} itemCount={}",
                dto.getTaskId(), items.size());
        return buildAck(dto, "ACCEPTED", "");
    }

    private String resolveLotId(String lotId) {
        return (lotId == null || lotId.isBlank()) ? LOT_NOT_MANAGED : lotId;
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
