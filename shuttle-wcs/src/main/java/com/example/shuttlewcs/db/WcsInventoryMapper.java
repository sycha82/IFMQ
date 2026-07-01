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
}
