package com.example.shuttlewcs.db;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// EqpPallet 마스터 — 설비 전용 팔렛트 식별자. 물리 수량만큼 정의 후 사이클링
@Getter
@Setter
@Builder
public class EqpPallet {

    private String eqpPalletId;
    private String status;       // EMPTY | IN_USE | MAINTENANCE
    private String location;     // IN_RACK | PICKING_ZONE | STATION | IDLE
    private LocalDateTime updatedAt;
}
