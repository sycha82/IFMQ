package com.example.shuttlewcs.service;

import com.example.common.dto.InboundCmdDetail;
import com.example.common.dto.InboundCmdDto;
import com.example.shuttlewcs.db.WcsInboundOrderH;
import com.example.shuttlewcs.db.WcsInboundOrderHMapper;
import com.example.shuttlewcs.db.WcsPalletLineH;
import com.example.shuttlewcs.db.WcsPalletLineHMapper;
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

    private final WcsInboundOrderHMapper inboundOrderHMapper;
    private final WcsPalletLineHMapper palletLineHMapper;

    // INBOUND_CMD 수신 적재 — 1 task : N pallet 일괄 처리
    // 동일 taskId+palletId 재수신은 WMS 재시도로만 해석 → 중복 거부(409).
    @Transactional
    public void receiveInboundOrder(InboundCmdDto dto) {
        List<InboundCmdDetail> details = dto.getInboundDetail();
        if (details == null || details.isEmpty()) {
            throw new IllegalArgumentException("inboundDetail이 비어 있음 | messageId=" + dto.getMessageId());
        }

        LocalDateTime now = LocalDateTime.now();

        for (InboundCmdDetail detail : details) {
            WcsInboundOrderH existing = inboundOrderHMapper.findByPk(dto.getTaskId(), detail.getPalletId());
            if (existing != null) {
                throw new InboundOrderConflictException(
                        "이미 존재하는 입고 주문 재수신 거부 | taskId=" + dto.getTaskId()
                                + " palletId=" + detail.getPalletId()
                                + " cmdStatus=" + existing.getCmdStatus());
            }

            String lotId = resolveLotId(detail.getLotId());

            inboundOrderHMapper.insert(WcsInboundOrderH.builder()
                    .taskId(dto.getTaskId())
                    .palletId(detail.getPalletId())
                    .cmdStatus("RECEIVED")
                    .recvMessageId(dto.getMessageId())
                    .lastMessageId(dto.getMessageId())
                    .recvCount(1)
                    .receivedAt(now)
                    .build());

            palletLineHMapper.insert(WcsPalletLineH.builder()
                    .palletId(detail.getPalletId())
                    .effectiveFrom(now)
                    .skuCode(detail.getItemCode())
                    .lotId(lotId)
                    .qty(detail.getQty())
                    .expireDate(detail.getExpireDate())
                    .isLatest("Y")
                    .build());

            log.info("[INBOUND_ORDER] 신규 적재 | taskId={} palletId={} seq={}",
                    dto.getTaskId(), detail.getPalletId(), detail.getSequenceNo());
        }
    }

    private String resolveLotId(String lotId) {
        return (lotId == null || lotId.isBlank()) ? LOT_NOT_MANAGED : lotId;
    }
}
