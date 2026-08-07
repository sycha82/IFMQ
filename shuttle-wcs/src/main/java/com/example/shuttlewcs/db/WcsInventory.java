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
    private String palletId;           // WMS 운영 PalletId (NOT NULL)
    private String lotId;              // 로트 번호 (lot 미관리 품목은 NULL)
    private Long quantity;             // on-hand (랙 물리 재고)
    private String uom;
    private Long reservedQty;          // 예약 수량 (available = quantity - reservedQty)
    private LocalDateTime updatedAt;
}
