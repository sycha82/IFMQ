package com.example.shuttlewcs.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WcsInventoryMapper {

    WcsInventory findByPk(@Param("sku") String sku,
                          @Param("locationId") String locationId);

    int countAll();

    // INBOUND_DONE 시 재고 누적 — (sku, location_id) upsert, 수량 가산
    void upsertAdd(@Param("sku") String sku,
                   @Param("locationId") String locationId,
                   @Param("qty") Integer qty,
                   @Param("uom") String uom);

    // OUTBOUND_DONE 시 랙 재고 제거 — 팔렛이 랙에서 배출되어 해당 위치 재고 전체 삭제
    int deleteByLocation(@Param("locationId") String locationId);
}
