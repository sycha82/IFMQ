package com.example.shuttlewcs.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// ⑧ wcs_inventory — 재고 ((sku, location_id) 단위, location_id=eqp_pallet_id)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WcsInventory {

    private String sku;                // PK
    private String locationId;         // PK (eqp_pallet_id)
    private Long quantity;
    private String uom;
    private LocalDateTime updatedAt;
}
