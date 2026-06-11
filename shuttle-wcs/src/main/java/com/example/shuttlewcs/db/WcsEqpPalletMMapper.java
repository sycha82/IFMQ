package com.example.shuttlewcs.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface WcsEqpPalletMMapper {

    WcsEqpPalletM findById(@Param("eqpPalletId") String eqpPalletId);

    int countAll();

    void insert(WcsEqpPalletM pallet);

    void updateStatus(@Param("eqpPalletId") String eqpPalletId,
                      @Param("palletStatus") String palletStatus,
                      @Param("maintReason") String maintReason);

    void updateLastUsedAt(@Param("eqpPalletId") String eqpPalletId,
                         @Param("lastUsedAt") LocalDateTime lastUsedAt);
}
