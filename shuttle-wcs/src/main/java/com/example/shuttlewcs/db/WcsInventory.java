package com.example.shuttlewcs.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// ⑧ wcs_inventory — 재고 ((sku_code, location_id) 단위, location_id=eqp_pallet_id)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WcsInventory {

    private String skuCode;            // PK
    private String locationId;         // PK (eqp_pallet_id)
    private Long quantity;             // on-hand (랙 물리 재고)
    private Long reservedQty;          // 예약 수량 (available = quantity - reservedQty)
    private String uom;
    private String palletId;           // WMS 운영 PalletId (표시·추적용)
    private String lotId;              // 로트 번호 (표시·추적용)
    private LocalDateTime updatedAt;
}
