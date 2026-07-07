package com.example.shuttlewcs.service;

import com.example.common.dto.OutboundDoneAckDto;
import com.example.common.dto.OutboundDoneDto;
import com.example.shuttlewcs.db.WcsEqpPalletMap;
import com.example.shuttlewcs.db.WcsEqpPalletMapH;
import com.example.shuttlewcs.db.WcsEqpPalletMapHMapper;
import com.example.shuttlewcs.db.WcsEqpPalletMapMapper;
import com.example.shuttlewcs.db.WcsOutboundOrderD;
import com.example.shuttlewcs.db.WcsOutboundOrderDMapper;
import com.example.shuttlewcs.db.WcsOutboundOrderHMapper;
import com.example.shuttlewcs.exception.RcsProtocolException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboundDoneService {

    private final WcsEqpPalletMapMapper eqpPalletMapMapper;
    private final WcsEqpPalletMapHMapper eqpPalletMapHMapper;
    private final WcsOutboundOrderDMapper orderDMapper;
    private final WcsOutboundOrderHMapper orderHMapper;
    private final RcsMsgLogService rcsMsgLogService;

    /**
     * API 05 · OUTBOUND_DONE — 출고 스테이션 배출 완료 보고 수신 → 상태 전이 → API 06 OUTBOUND_DONE_ACK 동기 회신.
     * (외부 피킹존 Case) 완료 시 EqpPallet은 PICKING_ZONE 으로 이동, 출고 지시 라인 완료 처리.
     * 재고 차감·WMS OUTBOUND_COMPLETE 통보는 후속(피킹 보고 단계).
     */
    @Transactional
    public OutboundDoneAckDto receiveOutboundDone(OutboundDoneDto dto) {
        LocalDateTime now = LocalDateTime.now();

        // 1. eqpPallet 매핑 조회 + 정합성 검증
        WcsEqpPalletMap map = eqpPalletMapMapper.findById(dto.getEqpPalletId());
        if (map == null) {
            throw new RcsProtocolException("미등록 eqpPallet | eqpPalletId=" + dto.getEqpPalletId());
        }
        String expectedTaskId = map.getEqpPalletId() + "-" + map.getCycleNo();
        if (!expectedTaskId.equals(dto.getWcsTaskId())) {
            throw new RcsProtocolException("wcsTaskId 불일치 | 수신=" + dto.getWcsTaskId()
                    + " 현재=" + expectedTaskId);
        }
        // 출고 진행중 상태(OUTBOUND_TASK 발행분)만 완료 처리 — 입고 IN_PROGRESS와 location으로 구분
        if (!"IN_PROGRESS".equals(map.getMapStatus()) || !"OUTBOUNDING".equals(map.getLocation())) {
            throw new RcsProtocolException("출고 진행중(IN_PROGRESS/OUTBOUNDING) 상태 아님 | eqpPalletId="
                    + dto.getEqpPalletId() + " mapStatus=" + map.getMapStatus() + " location=" + map.getLocation());
        }

        rcsMsgLogService.logReceive("OUTBOUND_DONE", dto.getMessageId(), dto.getRefMessageId(),
                dto.getDestStation(), dto.getEqpPalletId(), dto.getWcsTaskId(), dto, dto.getStatus());
        log.info("[RCS] OUTBOUND_DONE 수신 | wcsTaskId={} eqpPalletId={} status={} shuttleId={}",
                dto.getWcsTaskId(), dto.getEqpPalletId(), dto.getStatus(), dto.getShuttleId());

        // 2. 실패 보고 — 상태 전이 없이 실패 ACK
        if (!"COMPLETED".equals(dto.getStatus())) {
            return replyDoneAck(dto, "FAILED", "출고 실패 보고 수신: " + dto.getFailReason());
        }

        // 3. 완료 처리 — 출고 지시 라인 완료 + EqpPallet PICKING_ZONE 이동
        List<WcsOutboundOrderD> lines = orderDMapper.findActiveByPalletId(map.getPalletId());
        orderDMapper.updateCompletedByPalletId(map.getPalletId(), now);

        eqpPalletMapMapper.updateStatus(map.getEqpPalletId(), "OUTBOUND");        // IN_PROGRESS → OUTBOUND
        eqpPalletMapMapper.updateLocation(map.getEqpPalletId(), "PICKING_ZONE");  // OUTBOUNDING → PICKING_ZONE
        // 랙 재고는 출고 시작(OUTBOUND_TASK) 시점에 이미 제외됨 — 여기서는 재고 미조작

        eqpPalletMapHMapper.insert(WcsEqpPalletMapH.builder()
                .eqpPalletId(map.getEqpPalletId())
                .cycleNo(map.getCycleNo())
                .taskId(map.getTaskId())
                .palletId(map.getPalletId())
                .mapStatus("OUTBOUND")
                .location("PICKING_ZONE")
                .mappedAt(map.getMappedAt())
                .eventType("OUTBOUND_DONE")
                .eventAt(now)
                .eventBy("RCS")
                .note("wcsTaskId=" + dto.getWcsTaskId() + " shuttleId=" + dto.getShuttleId())
                .build());

        // 4. 출고 지시(task) 전체 완료 시 헤더 COMPLETED
        for (WcsOutboundOrderD line : lines) {
            if (orderDMapper.countActiveIncompleteByTaskId(line.getTaskId()) == 0) {
                orderHMapper.updateStatus(line.getTaskId(), "COMPLETED");
                log.info("[RCS] 출고 task 완료 | taskId={}", line.getTaskId());
            }
        }

        log.info("[RCS] OUTBOUND_DONE 완료 | eqpPalletId={} palletId={} 완료라인={}건",
                map.getEqpPalletId(), map.getPalletId(), lines.size());

        return replyDoneAck(dto, "OK", "");
    }

    // API 06 · OUTBOUND_DONE_ACK 생성 + 발신 로깅
    private OutboundDoneAckDto replyDoneAck(OutboundDoneDto req, String result, String message) {
        OutboundDoneAckDto ack = OutboundDoneAckDto.builder()
                .messageType("OUTBOUND_DONE_ACK")
                .messageId(UUID.randomUUID().toString())
                .refMessageId(req.getMessageId())
                .sequenceNo(1)
                .timestamp(LocalDateTime.now())
                .wcsTaskId(req.getWcsTaskId())
                .result(result)
                .message(message)
                .build();

        rcsMsgLogService.logSend("OUTBOUND_DONE_ACK", ack.getMessageId(), ack.getRefMessageId(),
                req.getDestStation(), req.getEqpPalletId(), req.getWcsTaskId(), ack, result);
        log.info("[RCS] OUTBOUND_DONE_ACK 회신 | wcsTaskId={} result={}", ack.getWcsTaskId(), result);
        return ack;
    }
}
