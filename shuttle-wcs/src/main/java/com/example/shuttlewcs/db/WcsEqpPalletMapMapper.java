package com.example.shuttlewcs.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface WcsEqpPalletMapMapper {

    WcsEqpPalletMap findById(@Param("eqpPalletId") String eqpPalletId);

    WcsEqpPalletMap findByPalletId(@Param("palletId") String palletId);

    int countAll();

    // 마스터(③) 등록 시 함께 EMPTY/IDLE/cycle_no=0 으로 초기 row 생성
    void insertEmpty(@Param("eqpPalletId") String eqpPalletId);

    // PRE03 매핑 등록 — cycle_no+1, task/pallet 채우고 MAPPED/STATION 으로 전환
    void applyMapping(@Param("eqpPalletId") String eqpPalletId,
                      @Param("taskId") String taskId,
                      @Param("palletId") String palletId,
                      @Param("mappedAt") LocalDateTime mappedAt);

    void updateStatus(@Param("eqpPalletId") String eqpPalletId,
                      @Param("mapStatus") String mapStatus);

    void updateLocation(@Param("eqpPalletId") String eqpPalletId,
                        @Param("location") String location);

    // STORED 처리 : 셀 입고 완료
    void markStored(@Param("eqpPalletId") String eqpPalletId,
                    @Param("storedAt") LocalDateTime storedAt);
}
