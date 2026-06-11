package com.example.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

// API 02 · RCS → WCS · BCR 감지 통보 (EqpPalletId 스캔 결과)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class BcrReadDto extends WcsMessageBase {

    private String stationId;
    private String eqpPalletId;
}
