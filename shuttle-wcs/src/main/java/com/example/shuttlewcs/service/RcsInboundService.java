package com.example.shuttlewcs.service;

import com.example.common.dto.BcrReadDto;
import com.example.common.dto.StationStatusDto;
import com.example.shuttlewcs.db.WcsEqpPalletMap;
import com.example.shuttlewcs.db.WcsEqpPalletMapMapper;
import com.example.shuttlewcs.db.WcsStation;
import com.example.shuttlewcs.db.WcsStationMapper;
import com.example.shuttlewcs.exception.RcsProtocolException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RcsInboundService {

    private final WcsStationMapper stationMapper;
    private final WcsEqpPalletMapMapper eqpPalletMapMapper;

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

    // API 02 · BCR_READ — 스테이션에서 읽은 eqpPallet 매핑 검증 + 스테이션 점유
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

        log.info("[RCS] BCR_READ 처리 | stationId={} eqpPalletId={} → palletId={} taskId={}",
                dto.getStationId(), dto.getEqpPalletId(), map.getPalletId(), map.getTaskId());
    }
}
