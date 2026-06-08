package com.example.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class InboundCancelDto extends WcsMessageBase {

    private String taskId;
    private String palletId;
    private String reason;
}
