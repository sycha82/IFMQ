package com.example.shuttlewcs.service;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class InboundOrderService {

    private static final String LOT_NOT_MANAGED = "N/A";

    private final WcsInboundOrderHMapper inboundOrderHMapper;
    private final WcsPalletLineHMapper palletLineHMapper;

    // INBOUND_CMD 수신 적재 — wcs_inbound_order_h(RECEIVED) + wcs_pallet_line_h(is_latest=Y)
    // 동일 taskId+palletId 재수신은 WMS 재시도로만 해석 → 중복 거부(409).
    // 입고 정보 수정은 취소(추후 구현) 후 신규 taskId로 재발행하는 것이 올바른 흐름이다.
    @Transactional
    public void receiveInboundOrder(InboundCmdDto dto) {
        WcsInboundOrderH existing = inboundOrderHMapper.findByPk(dto.getTaskId(), dto.getPalletId());
        if (existing != null) {
            throw new InboundOrderConflictException(
                    "이미 존재하는 입고 주문 재수신 거부 | taskId=" + dto.getTaskId()
                            + " palletId=" + dto.getPalletId()
                            + " cmdStatus=" + existing.getCmdStatus());
        }

        String lotId = resolveLotId(dto.getLotId());
        LocalDateTime now = LocalDateTime.now();

        inboundOrderHMapper.insert(WcsInboundOrderH.builder()
                .taskId(dto.getTaskId())
                .palletId(dto.getPalletId())
                .cmdStatus("RECEIVED")
                .recvMessageId(dto.getMessageId())
                .lastMessageId(dto.getMessageId())
                .recvCount(1)
                .receivedAt(now)
                .build());

        palletLineHMapper.insert(WcsPalletLineH.builder()
                .palletId(dto.getPalletId())
                .effectiveFrom(now)
                .skuCode(dto.getItemCode())
                .lotId(lotId)
                .qty(dto.getQty())
                .expireDate(dto.getExpireDate())
                .isLatest("Y")
                .build());

        log.info("[INBOUND_ORDER] 신규 적재 | taskId={} palletId={}", dto.getTaskId(), dto.getPalletId());
    }

    private String resolveLotId(String lotId) {
        return (lotId == null || lotId.isBlank()) ? LOT_NOT_MANAGED : lotId;
    }
}
