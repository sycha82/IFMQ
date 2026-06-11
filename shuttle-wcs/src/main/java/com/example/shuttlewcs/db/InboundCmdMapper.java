package com.example.shuttlewcs.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InboundCmdMapper {

    InboundCmd findByPalletId(@Param("palletId") String palletId);

    int countAll();

    // 동일 PalletId 재수신 시 수량/품목 갱신 (라벨 재사용)
    void upsert(InboundCmd cmd);

    void updateStatus(@Param("palletId") String palletId,
                      @Param("status") String status);
}
