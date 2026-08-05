package com.example.shuttlewcs.db;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WcsInventoryMapper {

    WcsInventory findByPk(@Param("sku") String sku,
                          @Param("locationId") String locationId);

    int countAll();

    // 모니터링 조회 — 가용재고 뷰(biz.wcs_vw_available_inventory)
    List<WcsAvailableInventory> findAllAvailable();

    // INBOUND_DONE 시 재고 누적 — (sku, location_id) upsert, 수량 가산
    void upsertAdd(@Param("sku") String sku,
                   @Param("locationId") String locationId,
                   @Param("qty") Integer qty,
                   @Param("uom") String uom);

    // OUTBOUND_CMD 할당 시 팔렛 전체 예약 — 미예약 상태에서만 reserved_qty=quantity 로 설정
    int reserveByLocation(@Param("locationId") String locationId);

    // 예약 여부 조회 — 해당 위치에 예약된(reserved_qty>0) 라인 존재 여부
    boolean existsReservedByLocation(@Param("locationId") String locationId);

    // OUTBOUND_DONE 시 랙 재고 제거 — 팔렛이 랙에서 배출되어 해당 위치 재고 전체 삭제
    int deleteByLocation(@Param("locationId") String locationId);
}
