package com.example.shuttlewcs.service;

import com.example.common.dto.InboundCmdDto;
import com.example.shuttlewcs.db.WcsInboundOrderH;
import com.example.shuttlewcs.db.WcsInboundOrderHMapper;
import com.example.shuttlewcs.db.WcsPalletLineH;
import com.example.shuttlewcs.db.WcsPalletLineHMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class InboundOrderService {

    private static final String LOT_NOT_MANAGED = "N/A";

    private final WcsInboundOrderHMapper inboundOrderHMapper;
    private final WcsPalletLineHMapper palletLineHMapper;

    // INBOUND_CMD 수신 적재 — wcs_inbound_order_h(RECEIVED) + wcs_pallet_line_h(is_latest=Y)
    @Transactional
    public void receiveInboundOrder(InboundCmdDto dto) {
        String lotId = resolveLotId(dto.getLotId());
        LocalDateTime now = LocalDateTime.now();

        WcsInboundOrderH existing = inboundOrderHMapper.findByPk(dto.getTaskId(), dto.getPalletId());

        if (existing == null) {
            inboundOrderHMapper.insert(WcsInboundOrderH.builder()
                    .taskId(dto.getTaskId())
                    .palletId(dto.getPalletId())
                    .cmdStatus("RECEIVED")
                    .recvMessageId(dto.getMessageId())
                    .lastMessageId(dto.getMessageId())
                    .recvCount(1)
                    .receivedAt(now)
                    .build());
            log.info("[INBOUND_ORDER] 신규 적재 | taskId={} palletId={}", dto.getTaskId(), dto.getPalletId());
        } else {
            inboundOrderHMapper.updateOnResubmit(dto.getTaskId(), dto.getPalletId(), dto.getMessageId(), now);
            palletLineHMapper.supersedeLatest(dto.getPalletId(), dto.getItemCode(), lotId, now, now);
            log.info("[INBOUND_ORDER] 재수신 적재 | taskId={} palletId={} recvCount={}",
                    dto.getTaskId(), dto.getPalletId(), existing.getRecvCount() + 1);
        }

        palletLineHMapper.insert(WcsPalletLineH.builder()
                .palletId(dto.getPalletId())
                .effectiveFrom(now)
                .skuCode(dto.getItemCode())
                .lotId(lotId)
                .qty(dto.getQty())
                .expireDate(dto.getExpireDate())
                .isLatest("Y")
                .build());
    }

    private String resolveLotId(String lotId) {
        return (lotId == null || lotId.isBlank()) ? LOT_NOT_MANAGED : lotId;
    }
}
