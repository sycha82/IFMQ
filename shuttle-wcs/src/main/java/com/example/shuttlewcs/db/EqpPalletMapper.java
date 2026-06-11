package com.example.shuttlewcs.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EqpPalletMapper {

    EqpPallet findById(@Param("eqpPalletId") String eqpPalletId);

    int countAll();

    void upsert(EqpPallet pallet);

    void updateLocation(@Param("eqpPalletId") String eqpPalletId,
                        @Param("location") String location);
}
