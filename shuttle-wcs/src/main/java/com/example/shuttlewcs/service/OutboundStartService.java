package com.example.shuttlewcs.service;

import com.example.common.dto.OutboundTaskAckDto;
import com.example.common.dto.OutboundTaskDto;
import com.example.shuttlewcs.client.RcsClient;
import com.example.shuttlewcs.db.WcsEqpPalletMap;
import com.example.shuttlewcs.db.WcsEqpPalletMapH;
import com.example.shuttlewcs.db.WcsEqpPalletMapHMapper;
import com.example.shuttlewcs.db.WcsEqpPalletMapMapper;
import com.example.shuttlewcs.db.WcsOutboundOrderD;
import com.example.shuttlewcs.db.WcsOutboundOrderDMapper;
import com.example.shuttlewcs.db.WcsOutboundOrderH;
import com.example.shuttlewcs.db.WcsOutboundOrderHMapper;
import com.example.shuttlewcs.db.WcsStation;
import com.example.shuttlewcs.db.WcsStationMapper;
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
public class OutboundStartService {

    private static final String OUTBOUND_STATION_TYPE = "OUTBOUND";
    private static final String SHIPPABLE_MAP_STATUS = "STORED";

    private final WcsStationMapper stationMapper;
    private final WcsOutboundOrderHMapper orderHMapper;
    private final WcsOutboundOrderDMapper orderDMapper;
    private final WcsEqpPalletMapMapper eqpPalletMapMapper;
    private final WcsEqpPalletMapHMapper eqpPalletMapHMapper;
    private final RcsClient rcsClient;
    private final RcsMsgLogService rcsMsgLogService;

    /**
     * 출고 시작 — cmd_status='RECEIVED' 출고 지시를 순차 조회해 팔렛 라인별로 OUTBOUND_TASK 발행.
     * destStation은 OUTBOUND 스테이션 1건으로 지정(가용성 판정은 하지 않음).
     * 라인 단위 best-effort — 매핑 미존재·거부 라인은 스킵하고 다음 라인 진행.
     * task 내 전체 라인 발송 성공 시 지시 헤더 cmd_status → DISPATCHED.
     */
    @Transactional
    public String startOutbound() {
        WcsStation station = stationMapper.findFirstByType(OUTBOUND_STATION_TYPE);
        if (station == null) {
            throw new RcsProtocolException("등록된 OUTBOUND 스테이션 없음 — STATION_STATUS(출고) 먼저 발행 필요");
        }
        String destStation = station.getStationId();

        List<WcsOutboundOrderH> orders = orderHMapper.findByStatus("RECEIVED");
        int dispatched = 0;
        int skipped = 0;

        for (WcsOutboundOrderH order : orders) {
            List<WcsOutboundOrderD> lines = orderDMapper.findByTaskId(order.getTaskId());
            boolean allDispatched = !lines.isEmpty();

            for (WcsOutboundOrderD line : lines) {
                if (line.getCancelledAt() != null || line.getCompletedAt() != null) {
                    continue;
                }
                boolean ok;
                try {
                    ok = dispatchLine(order, line, destStation);
                } catch (Exception e) {
                    log.error("[OUTBOUND_START] 발송 실패 | taskId={} palletId={} error={}",
                            order.getTaskId(), line.getPalletId(), e.getMessage());
                    ok = false;
                }
                if (ok) {
                    dispatched++;
                } else {
                    skipped++;
                    allDispatched = false;
                }
            }

            if (allDispatched) {
                orderHMapper.updateStatus(order.getTaskId(), "DISPATCHED");
                log.info("[OUTBOUND_START] 지시 발송 완료 | taskId={}", order.getTaskId());
            }
        }

        String summary = String.format("출고 시작 | destStation=%s 대상지시=%d건 발송=%d건 스킵=%d건",
                destStation, orders.size(), dispatched, skipped);
        log.info("[OUTBOUND_START] {}", summary);
        return summary;
    }

    // 팔렛 라인 1건 OUTBOUND_TASK 발행 + ACK 처리 + 상태 전이. 발송 성공 시 true.
    private boolean dispatchLine(WcsOutboundOrderH order, WcsOutboundOrderD line, String destStation) {
        WcsEqpPalletMap map = eqpPalletMapMapper.findByPalletId(line.getPalletId());
        if (map == null || !SHIPPABLE_MAP_STATUS.equals(map.getMapStatus())) {
            log.warn("[OUTBOUND_START] 스킵 · 출고 불가 | taskId={} palletId={} mapStatus={}",
                    order.getTaskId(), line.getPalletId(), map != null ? map.getMapStatus() : "NONE");
            return false;
        }

        String wcsTaskId = map.getEqpPalletId() + "-" + map.getCycleNo();
        OutboundTaskDto taskDto = OutboundTaskDto.builder()
                .messageType("OUTBOUND_TASK")
                .messageId(UUID.randomUUID().toString())
                .refMessageId(null)
                .sequenceNo(1)
                .timestamp(LocalDateTime.now())
                .wcsTaskId(wcsTaskId)
                .taskType("OUTBOUND")
                .eqpPalletId(map.getEqpPalletId())
                .destStation(destStation)
                .build();

        rcsMsgLogService.logSend("OUTBOUND_TASK", taskDto.getMessageId(), null,
                destStation, map.getEqpPalletId(), wcsTaskId, taskDto, null);
        log.info("[OUTBOUND_START] OUTBOUND_TASK 발행 | wcsTaskId={} eqpPalletId={} palletId={} destStation={}",
                wcsTaskId, map.getEqpPalletId(), line.getPalletId(), destStation);

        OutboundTaskAckDto ack = rcsClient.sendOutboundTask(taskDto);

        rcsMsgLogService.logReceive("OUTBOUND_TASK_ACK", ack.getMessageId(), ack.getRefMessageId(),
                destStation, map.getEqpPalletId(), wcsTaskId, ack, ack.getResult());
        log.info("[OUTBOUND_START] OUTBOUND_TASK_ACK 수신 | wcsTaskId={} result={} shuttleId={}",
                ack.getWcsTaskId(), ack.getResult(), ack.getShuttleId());

        if (!"ACCEPTED".equals(ack.getResult())) {
            log.warn("[OUTBOUND_START] OUTBOUND_TASK 거부 | wcsTaskId={} message={}",
                    wcsTaskId, ack.getMessage());
            return false;
        }

        // 상태 전이: eqpPallet STORED → IN_PROGRESS, location IN_RACK → OUTBOUNDING
        eqpPalletMapMapper.updateStatus(map.getEqpPalletId(), "IN_PROGRESS");
        eqpPalletMapMapper.updateLocation(map.getEqpPalletId(), "OUTBOUNDING");

        eqpPalletMapHMapper.insert(WcsEqpPalletMapH.builder()
                .eqpPalletId(map.getEqpPalletId())
                .cycleNo(map.getCycleNo())
                .taskId(map.getTaskId())
                .palletId(map.getPalletId())
                .mapStatus("IN_PROGRESS")
                .location("OUTBOUNDING")
                .mappedAt(map.getMappedAt())
                .eventType("OUTBOUND_TASK")
                .eventAt(LocalDateTime.now())
                .eventBy("WCS")
                .note("wcsTaskId=" + wcsTaskId + " shuttleId=" + ack.getShuttleId()
                        + " destStation=" + destStation)
                .build());
        return true;
    }
}
