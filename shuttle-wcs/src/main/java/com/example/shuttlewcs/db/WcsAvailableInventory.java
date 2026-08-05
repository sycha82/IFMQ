package com.example.shuttlewcs.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// 가용재고 뷰(biz.wcs_vw_available_inventory) 읽기 전용 모델 — available = quantity - reserved_qty
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WcsAvailableInventory {

    private String sku;
    private String locationId;      // eqp_pallet_id
    private Long quantity;          // on-hand
    private Long reservedQty;       // 예약
    private Long availableQty;      // quantity - reservedQty
    private String uom;
    private LocalDateTime updatedAt;
}
