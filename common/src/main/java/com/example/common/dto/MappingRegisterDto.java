package com.example.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

// PRE 03 · 작업자 단말 → WCS · EqpPalletId ↔ PalletId 매핑 등록 요청
// 내부 단말 REST 호출용 (MQ 메시지 아님). 추적 편의를 위해 WcsMessageBase 상속.
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class MappingRegisterDto extends WcsMessageBase {

    private String palletId;
    private String eqpPalletId;
}
