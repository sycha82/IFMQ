package com.example.shuttlewcs.service;

import com.example.common.dto.InboundTaskAckDto;
import com.example.common.dto.InboundTaskDto;
import com.example.common.dto.OutboundTaskAckDto;
import com.example.common.dto.OutboundTaskDto;
import com.example.shuttlewcs.db.WcsEqpPalletMap;
import com.example.shuttlewcs.db.WcsInboundOrderD;
import com.example.shuttlewcs.db.WcsOutboundOrderD;
import com.example.shuttlewcs.db.WcsTaskH;
import com.example.shuttlewcs.db.WcsTaskHMapper;
import com.example.shuttlewcs.exception.RcsProtocolException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 셔틀 이동 작업(TASK) 이력 적재 서비스 — biz.wcs_task_h.
 *
 * wcs_shuttle_msg_log 가 전문(payload) 원본 이력이라면, 이 서비스는 "작업" 단위 요약 이력을 남긴다.
 * 사용자가 payload JSON을 뒤지지 않고도 작업 진행 상태(발행→착수→완료)를 조회할 수 있게 하는 것이 목적.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskHistoryService {

    public static final String TYPE_INBOUND = "INBOUND";
    public static final String TYPE_OUTBOUND = "OUTBOUND";

    private final WcsTaskHMapper mapper;

    // 작업 채번 — TASK 발행 직전에 호출. wcs_task_id = TSK-{8자리}
    public String nextWcsTaskId() {
        return mapper.nextWcsTaskId();
    }

    // INBOUND_TASK 발행(ACK 수락) 시점 적재
    public void recordInboundDispatched(InboundTaskDto task, InboundTaskAckDto ack,
                                        WcsEqpPalletMap map, WcsInboundOrderD line) {
        mapper.insertDispatched(WcsTaskH.builder()
                .wcsTaskId(task.getWcsTaskId())
                .taskType(TYPE_INBOUND)
                .eqpPalletId(map.getEqpPalletId())
                .cycleNo(map.getCycleNo())
                .palletId(map.getPalletId())
                .orderTaskId(map.getTaskId())
                .stationId(task.getStationId())
                .shuttleId(ack != null ? ack.getShuttleId() : null)
                .skuCode(line != null ? line.getSkuCode() : null)
                .lotId(line != null ? line.getLotId() : null)
                .qty(line != null ? line.getQty() : null)
                .taskMessageId(task.getMessageId())
                .ackMessageId(ack != null ? ack.getMessageId() : null)
                .ackResult(ack != null ? ack.getResult() : null)
                .dispatchedAt(task.getTimestamp() != null ? task.getTimestamp() : LocalDateTime.now())
                .build());

        log.info("[TASK_HST] INBOUND TASK 이력 적재 | wcsTaskId={} eqpPalletId={} palletId={}",
                task.getWcsTaskId(), map.getEqpPalletId(), map.getPalletId());
    }

    // OUTBOUND_TASK 발행(ACK 수락) 시점 적재
    public void recordOutboundDispatched(OutboundTaskDto task, OutboundTaskAckDto ack,
                                         WcsEqpPalletMap map, WcsOutboundOrderD line,
                                         String orderTaskId) {
        mapper.insertDispatched(WcsTaskH.builder()
                .wcsTaskId(task.getWcsTaskId())
                .taskType(TYPE_OUTBOUND)
                .eqpPalletId(map.getEqpPalletId())
                .cycleNo(map.getCycleNo())
                .palletId(map.getPalletId())
                .orderTaskId(orderTaskId)
                .stationId(task.getDestStation())
                .shuttleId(ack != null ? ack.getShuttleId() : null)
                .skuCode(line != null ? line.getSkuCode() : null)
                .lotId(line != null ? line.getLotId() : null)
                .qty(line != null ? line.getQty() : null)
                .taskMessageId(task.getMessageId())
                .ackMessageId(ack != null ? ack.getMessageId() : null)
                .ackResult(ack != null ? ack.getResult() : null)
                .dispatchedAt(task.getTimestamp() != null ? task.getTimestamp() : LocalDateTime.now())
                .build());

        log.info("[TASK_HST] OUTBOUND TASK 이력 적재 | wcsTaskId={} eqpPalletId={} palletId={}",
                task.getWcsTaskId(), map.getEqpPalletId(), map.getPalletId());
    }

    // 설비 착수(INBOUND_START · OUTBOUND_START)
    public void markStarted(String taskType, String wcsTaskId, String shuttleId, LocalDateTime at) {
        int updated = mapper.markStarted(wcsTaskId, taskType, shuttleId, at);
        warnIfMissing(updated, taskType, wcsTaskId, "STARTED");
    }

    // 완료(INBOUND_DONE · OUTBOUND_DONE)
    public void markCompleted(String taskType, String wcsTaskId, String shuttleId, LocalDateTime at) {
        int updated = mapper.markCompleted(wcsTaskId, taskType, shuttleId, at);
        warnIfMissing(updated, taskType, wcsTaskId, "COMPLETED");
    }

    // 실패 보고(DONE status != COMPLETED)
    public void markFailed(String taskType, String wcsTaskId, String shuttleId,
                           String failReason, LocalDateTime at) {
        int updated = mapper.markFailed(wcsTaskId, taskType, shuttleId, failReason, at);
        warnIfMissing(updated, taskType, wcsTaskId, "FAILED");
    }

    public List<WcsTaskH> findRecent(String taskType, String taskStatus, String eqpPalletId, int limit) {
        return mapper.findRecent(taskType, taskStatus, eqpPalletId, limit);
    }

    public WcsTaskH findOneByWcsTaskId(String wcsTaskId) {
        return mapper.findOneByWcsTaskId(wcsTaskId);
    }

    /**
     * RCS가 보고한 wcsTaskId 검증 — 해당 작업이 존재하고, 기대한 방향(INBOUND/OUTBOUND)이며,
     * 보고된 eqpPalletId 와 일치하는지 확인한 뒤 작업 행을 돌려준다.
     * wcsTaskId 가 팔렛 상태에서 유도되지 않는 독립 채번이므로 검증은 이 조회로 수행한다.
     */
    public WcsTaskH requireTask(String taskType, String wcsTaskId, String eqpPalletId) {
        WcsTaskH task = mapper.findOneByWcsTaskId(wcsTaskId);
        if (task == null) {
            throw new RcsProtocolException("미등록 작업 | wcsTaskId=" + wcsTaskId);
        }
        if (!taskType.equals(task.getTaskType())) {
            throw new RcsProtocolException("작업 유형 불일치 | wcsTaskId=" + wcsTaskId
                    + " 기대=" + taskType + " 실제=" + task.getTaskType());
        }
        if (eqpPalletId != null && !eqpPalletId.equals(task.getEqpPalletId())) {
            throw new RcsProtocolException("작업-설비파레트 불일치 | wcsTaskId=" + wcsTaskId
                    + " 작업=" + task.getEqpPalletId() + " 수신=" + eqpPalletId);
        }
        return task;
    }

    // TASK 이력이 없는 건(기능 도입 전 데이터 등)은 본 처리를 막지 않고 경고만 남긴다.
    private void warnIfMissing(int updated, String taskType, String wcsTaskId, String toStatus) {
        if (updated == 0) {
            log.warn("[TASK_HST] 갱신 대상 TASK 이력 없음 | taskType={} wcsTaskId={} → {}",
                    taskType, wcsTaskId, toStatus);
        }
    }
}
