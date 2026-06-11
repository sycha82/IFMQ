package com.example.shuttlewcs.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InboundMappingMapper {

    InboundMapping findByEqpPalletId(@Param("eqpPalletId") String eqpPalletId);

    InboundMapping findByPalletId(@Param("palletId") String palletId);

    int countAll();

    // PRE03 매핑 등록 시 호출. 동일 EqpPalletId 재사용 시 덮어쓰기 (사이클링)
    void upsertMapping(InboundMapping mapping);

    void updateStatus(@Param("eqpPalletId") String eqpPalletId,
                      @Param("status") String status);

    void updateWcsTaskId(@Param("eqpPalletId") String eqpPalletId,
                        @Param("wcsTaskId") String wcsTaskId,
                        @Param("status") String status);
}
