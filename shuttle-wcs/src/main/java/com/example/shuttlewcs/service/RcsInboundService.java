package com.example.shuttlewcs.service;

import com.example.common.dto.BcrReadDto;
import com.example.common.dto.InboundCompleteDto;
import com.example.common.dto.InboundDoneAckDto;
import com.example.common.dto.InboundDoneDto;
import com.example.common.dto.InboundTaskAckDto;
import com.example.common.dto.InboundTaskDto;
import com.example.common.dto.StationStatusDto;
import com.example.shuttlewcs.client.RcsClient;
import com.example.shuttlewcs.client.WcsAppClient;
import com.example.shuttlewcs.db.WcsInboundOrderH;
import com.example.shuttlewcs.db.WcsEqpPalletMap;
import com.example.shuttlewcs.db.WcsEqpPalletMapH;
import com.example.shuttlewcs.db.WcsEqpPalletMapHMapper;
import com.example.shuttlewcs.db.WcsEqpPalletMapMapper;
import com.example.shuttlewcs.db.WcsInboundOrderD;
import com.example.shuttlewcs.db.WcsInboundOrderDMapper;
import com.example.shuttlewcs.db.WcsInboundOrderHMapper;
import com.example.shuttlewcs.db.WcsInventoryMapper;
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
public class RcsInboundService {

    private final WcsStationMapper stationMapper;
    private final WcsEqpPalletMapMapper eqpPalletMapMapper;
    private final WcsEqpPalletMapHMapper eqpPalletMapHMapper;
    private final WcsInboundOrderDMapper orderDMapper;
    private final WcsInboundOrderHMapper orderHMapper;
    private final WcsInventoryMapper inventoryMapper;
    private final RcsClient rcsClient;
    private final WcsAppClient wcsAppClient;
    private final RcsMsgLogService rcsMsgLogService;

    // API 01 · STATION_STATUS — 스테이션 상태 보고 반영 (upsert)
    @Transactional
    public void receiveStationStatus(StationStatusDto dto) {
        LocalDateTime changedAt = dto.getTimestamp() != null ? dto.getTimestamp() : LocalDateTime.now();
        stationMapper.upsertStatus(
                dto.getStationId(),
                dto.getStationType(),
                dto.getStatus(),
                changedAt);
        log.info("[RCS] STATION_STATUS 반영 | stationId={} type={} status={}",
                dto.getStationId(), dto.getStationType(), dto.getStatus());
    }

    // API 02 · BCR_READ — 스테이션에서 읽은 eqpPallet 매핑 검증 + 스테이션 점유 + INBOUND_TASK 자동 발행
    @Transactional
    public void receiveBcrRead(BcrReadDto dto) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 스테이션 존재·가용 검증
        WcsStation station = stationMapper.findById(dto.getStationId());
        if (station == null) {
            throw new RcsProtocolException("미등록 스테이션 | stationId=" + dto.getStationId());
        }
        if (!"AVAILABLE".equals(station.getStatus())) {
            throw new RcsProtocolException("사용 불가 스테이션 | stationId=" + dto.getStationId()
                    + " status=" + station.getStatus());
        }

        // 2. eqpPallet 매핑 검증 (MAPPED 상태 + palletId 확정)
        WcsEqpPalletMap map = eqpPalletMapMapper.findById(dto.getEqpPalletId());
        if (map == null || map.getPalletId() == null || !"MAPPED".equals(map.getMapStatus())) {
            throw new RcsProtocolException("매핑되지 않은 eqpPallet | eqpPalletId=" + dto.getEqpPalletId()
                    + " mapStatus=" + (map != null ? map.getMapStatus() : "NONE"));
        }

        // 3. 스테이션 점유(BUSY) + 현재 eqpPallet 기록
        stationMapper.updateStatusAndPallet(dto.getStationId(), "BUSY", dto.getEqpPalletId(), now);

        rcsMsgLogService.logReceive("BCR_READ", dto.getMessageId(), dto.getRefMessageId(),
                dto.getStationId(), dto.getEqpPalletId(), null, dto, null);

        log.info("[RCS] BCR_READ 처리 | stationId={} eqpPalletId={} → palletId={} taskId={}",
                dto.getStationId(), dto.getEqpPalletId(), map.getPalletId(), map.getTaskId());

        // 4. INBOUND_TASK 자동 발행 (API 03)
        sendInboundTask(dto, map);
    }

    // API 03 · INBOUND_TASK 발행 + API 04 INBOUND_TASK_ACK 동기 수신
    private void sendInboundTask(BcrReadDto bcrRead, WcsEqpPalletMap map) {
        String wcsTaskId = map.getEqpPalletId() + "-" + map.getCycleNo();

        List<WcsInboundOrderD> lines = orderDMapper.findActiveByPalletId(map.getPalletId());
        if (lines.isEmpty()) {
            throw new RcsProtocolException("입고 지시 상세 없음 | palletId=" + map.getPalletId());
        }
        WcsInboundOrderD line = lines.get(0); // 팔레트당 1라인 전제 (다품목 팔레트는 향후 확장)

        InboundTaskDto taskDto = InboundTaskDto.builder()
                .messageType("INBOUND_TASK")
                .messageId(UUID.randomUUID().toString())
                .refMessageId(bcrRead.getMessageId())
                .sequenceNo(1)
                .timestamp(LocalDateTime.now())
                .wcsTaskId(wcsTaskId)
                .stationId(bcrRead.getStationId())
                .eqpPalletId(map.getEqpPalletId())
                .result("ACCEPTED")
                .itemCode(line.getSkuCode())
                .lotId(line.getLotId())
                .qty(line.getQty())
                .expireDate(line.getExpireDate())
                .build();

        rcsMsgLogService.logSend("INBOUND_TASK", taskDto.getMessageId(), taskDto.getRefMessageId(),
                taskDto.getStationId(), taskDto.getEqpPalletId(), wcsTaskId, taskDto, taskDto.getResult());
        log.info("[RCS] INBOUND_TASK 발행 | wcsTaskId={} eqpPalletId={} itemCode={} qty={}",
                wcsTaskId, map.getEqpPalletId(), line.getSkuCode(), line.getQty());

        InboundTaskAckDto ack = rcsClient.sendInboundTask(taskDto);

        rcsMsgLogService.logReceive("INBOUND_TASK_ACK", ack.getMessageId(), ack.getRefMessageId(),
                taskDto.getStationId(), taskDto.getEqpPalletId(), wcsTaskId, ack, ack.getResult());
        log.info("[RCS] INBOUND_TASK_ACK 수신 | wcsTaskId={} result={} shuttleId={}",
                ack.getWcsTaskId(), ack.getResult(), ack.getShuttleId());

        if (!"ACCEPTED".equals(ack.getResult())) {
            throw new RcsProtocolException("INBOUND_TASK 거부 | wcsTaskId=" + wcsTaskId
                    + " message=" + ack.getMessage());
        }

        // 5. eqpPallet 상태 전이 MAPPED → IN_PROGRESS
        eqpPalletMapMapper.updateStatus(map.getEqpPalletId(), "IN_PROGRESS");

        eqpPalletMapHMapper.insert(WcsEqpPalletMapH.builder()
                .eqpPalletId(map.getEqpPalletId())
                .cycleNo(map.getCycleNo())
                .taskId(map.getTaskId())
                .palletId(map.getPalletId())
                .mapStatus("IN_PROGRESS")
                .location("STATION")
                .mappedAt(map.getMappedAt())
                .eventType("TASK_ASSIGNED")
                .eventAt(LocalDateTime.now())
                .eventBy("RCS")
                .note("wcsTaskId=" + wcsTaskId + " shuttleId=" + ack.getShuttleId())
                .build());
    }

    // API 05 · INBOUND_DONE — 셀 입고 완료 보고 수신 → 재고 갱신 → API 06 INBOUND_DONE_ACK 동기 회신
    @Transactional
    public InboundDoneAckDto receiveInboundDone(InboundDoneDto dto) {
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
        if (!"IN_PROGRESS".equals(map.getMapStatus())) {
            throw new RcsProtocolException("진행중(IN_PROGRESS) 상태 아님 | eqpPalletId="
                    + dto.getEqpPalletId() + " mapStatus=" + map.getMapStatus());
        }

        rcsMsgLogService.logReceive("INBOUND_DONE", dto.getMessageId(), dto.getRefMessageId(),
                null, dto.getEqpPalletId(), dto.getWcsTaskId(), dto, dto.getStatus());
        log.info("[RCS] INBOUND_DONE 수신 | wcsTaskId={} eqpPalletId={} status={} shuttleId={}",
                dto.getWcsTaskId(), dto.getEqpPalletId(), dto.getStatus(), dto.getShuttleId());

        // 2. 실패 보고 — 재고 반영 없이 실패 ACK
        if (!"COMPLETED".equals(dto.getStatus())) {
            return replyDoneAck(dto, "FAILED", "입고 실패 보고 수신: " + dto.getFailReason());
        }

        // 3. 완료 처리 — 상태 전이 + 재고 누적
        eqpPalletMapMapper.markStored(map.getEqpPalletId(), now);                 // IN_PROGRESS → STORED / IN_RACK
        orderDMapper.updateCompletedByPalletId(map.getPalletId(), now);          // 상세 완료
        stationMapper.freeByEqpPallet(map.getEqpPalletId(), now);               // 스테이션 해제 BUSY → AVAILABLE

        List<WcsInboundOrderD> lines = orderDMapper.findActiveByPalletId(map.getPalletId());
        for (WcsInboundOrderD line : lines) {
            inventoryMapper.upsertAdd(line.getSkuCode(), map.getEqpPalletId(), line.getQty(), "EA");
        }

        eqpPalletMapHMapper.insert(WcsEqpPalletMapH.builder()
                .eqpPalletId(map.getEqpPalletId())
                .cycleNo(map.getCycleNo())
                .taskId(map.getTaskId())
                .palletId(map.getPalletId())
                .mapStatus("STORED")
                .location("IN_RACK")
                .mappedAt(map.getMappedAt())
                .storedAt(now)
                .eventType("INBOUND_DONE")
                .eventAt(now)
                .eventBy("RCS")
                .note("wcsTaskId=" + dto.getWcsTaskId() + " shuttleId=" + dto.getShuttleId())
                .build());

        // 4. task 전체 완료 시 헤더 COMPLETED
        int remaining = orderDMapper.countActiveIncompleteByTaskId(map.getTaskId());
        if (remaining == 0) {
            orderHMapper.updateStatus(map.getTaskId(), "COMPLETED");
            log.info("[RCS] 입고 task 완료 | taskId={}", map.getTaskId());
        }

        // 5. WMS에 INBOUND_COMPLETE 자동 통보 (shuttle-wcs → wcs-app → MQ)
        notifyWmsInboundComplete(map, lines);

        log.info("[RCS] INBOUND_DONE 완료 | eqpPalletId={} palletId={} 재고반영={}건 taskRemaining={}",
                map.getEqpPalletId(), map.getPalletId(), lines.size(), remaining);

        return replyDoneAck(dto, "OK", "");
    }

    // API 07 · INBOUND_COMPLETE — 완료된 팔레트 라인별로 WMS에 통보 위임 (wcs-app 발행)
    private void notifyWmsInboundComplete(WcsEqpPalletMap map, List<WcsInboundOrderD> lines) {
        WcsInboundOrderH orderH = orderHMapper.findByTaskId(map.getTaskId());
        String refMessageId = orderH != null ? orderH.getRecvMessageId() : null;

        for (WcsInboundOrderD line : lines) {
            InboundCompleteDto completeDto = InboundCompleteDto.builder()
                    .messageType("INBOUND_COMPLETE")
                    .messageId(UUID.randomUUID().toString())
                    .refMessageId(refMessageId)
                    .sequenceNo(1)
                    .timestamp(LocalDateTime.now())
                    .taskId(map.getTaskId())
                    .palletId(map.getPalletId())
                    .itemCode(line.getSkuCode())
                    .lotId(line.getLotId())
                    .qty(line.getQty())
                    .status("COMPLETED")
                    .message("")
                    .build();
            try {
                wcsAppClient.notifyInboundComplete(completeDto);
                log.info("[RCS] INBOUND_COMPLETE 통보 | taskId={} palletId={} itemCode={} qty={}",
                        map.getTaskId(), map.getPalletId(), line.getSkuCode(), line.getQty());
            } catch (Exception e) {
                // WMS 통보 실패는 재고/완료 처리를 되돌리지 않는다 (경고만, 추후 재발행 대상)
                log.error("[RCS] INBOUND_COMPLETE 통보 실패 | taskId={} palletId={} error={}",
                        map.getTaskId(), map.getPalletId(), e.getMessage());
            }
        }
    }

    // API 06 · INBOUND_DONE_ACK 생성 + 발신 로깅
    private InboundDoneAckDto replyDoneAck(InboundDoneDto req, String result, String message) {
        InboundDoneAckDto ack = InboundDoneAckDto.builder()
                .messageType("INBOUND_DONE_ACK")
                .messageId(UUID.randomUUID().toString())
                .refMessageId(req.getMessageId())
                .sequenceNo(1)
                .timestamp(LocalDateTime.now())
                .wcsTaskId(req.getWcsTaskId())
                .result(result)
                .message(message)
                .build();

        rcsMsgLogService.logSend("INBOUND_DONE_ACK", ack.getMessageId(), ack.getRefMessageId(),
                null, req.getEqpPalletId(), req.getWcsTaskId(), ack, result);
        log.info("[RCS] INBOUND_DONE_ACK 회신 | wcsTaskId={} result={}", ack.getWcsTaskId(), result);
        return ack;
    }
}
