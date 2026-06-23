package com.example.shuttlewcs.service;

import com.example.common.dto.InboundCmdDetail;
import com.example.common.dto.InboundCmdDto;
import com.example.shuttlewcs.db.WcsInboundOrderD;
import com.example.shuttlewcs.db.WcsInboundOrderDMapper;
import com.example.shuttlewcs.db.WcsInboundOrderH;
import com.example.shuttlewcs.db.WcsInboundOrderHMapper;
import com.example.shuttlewcs.exception.InboundOrderConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InboundOrderService {

    private static final String LOT_NOT_MANAGED = "N/A";

    private final WcsInboundOrderHMapper orderHMapper;
    private final WcsInboundOrderDMapper orderDMapper;

    @Transactional
    public void receiveInboundOrder(InboundCmdDto dto) {
        List<InboundCmdDetail> details = dto.getInboundDetail();
        if (details == null || details.isEmpty()) {
            throw new IllegalArgumentException("inboundDetail이 비어 있음 | messageId=" + dto.getMessageId());
        }

        WcsInboundOrderH existing = orderHMapper.findByTaskId(dto.getTaskId());
        if (existing != null) {
            throw new InboundOrderConflictException(
                    "이미 존재하는 입고 주문 재수신 거부 | taskId=" + dto.getTaskId()
                            + " cmdStatus=" + existing.getCmdStatus());
        }

        LocalDateTime now = LocalDateTime.now();

        orderHMapper.insert(WcsInboundOrderH.builder()
                .taskId(dto.getTaskId())
                .cmdStatus("RECEIVED")
                .recvMessageId(dto.getMessageId())
                .lastMessageId(dto.getMessageId())
                .recvCount(1)
                .receivedAt(now)
                .build());

        for (InboundCmdDetail detail : details) {
            orderDMapper.insert(WcsInboundOrderD.builder()
                    .taskId(dto.getTaskId())
                    .palletId(detail.getPalletId())
                    .skuCode(detail.getItemCode())
                    .lotId(resolveLotId(detail.getLotId()))
                    .lineNo(detail.getLineNo())
                    .qty(detail.getQty())
                    .expireDate(detail.getExpireDate())
                    .build());
        }

        log.info("[INBOUND_ORDER] 신규 적재 | taskId={} detailCount={}", dto.getTaskId(), details.size());
    }

    private String resolveLotId(String lotId) {
        return (lotId == null || lotId.isBlank()) ? LOT_NOT_MANAGED : lotId;
    }
}
