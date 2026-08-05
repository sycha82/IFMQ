package com.example.shuttlewcs.db;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface WcsStationMapper {

    WcsStation findById(@Param("stationId") String stationId);

    // 출고 시작 destStation 지정용 — 해당 타입 스테이션 1건 (가용성 무관)
    WcsStation findFirstByType(@Param("stationType") String stationType);

    int countAll();

    // 모니터링 조회 — 전체 스테이션
    List<WcsStation> findAll();

    // STATION_STATUS 수신 — 미존재 시 생성, 존재 시 상태 갱신 (upsert)
    void upsertStatus(@Param("stationId") String stationId,
                      @Param("stationType") String stationType,
                      @Param("status") String status,
                      @Param("changedAt") LocalDateTime changedAt);

    // BCR_READ 등 내부 상태 전이 — 상태 + 현재 eqpPallet 갱신
    void updateStatusAndPallet(@Param("stationId") String stationId,
                               @Param("status") String status,
                               @Param("curEqpPalletId") String curEqpPalletId,
                               @Param("changedAt") LocalDateTime changedAt);

    // INBOUND_DONE 시 스테이션 해제 — 해당 eqpPallet 점유 스테이션을 AVAILABLE 로
    void freeByEqpPallet(@Param("eqpPalletId") String eqpPalletId,
                         @Param("changedAt") LocalDateTime changedAt);
}
