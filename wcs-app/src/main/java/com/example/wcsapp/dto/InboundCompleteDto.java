package com.example.wcsapp.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class InboundCompleteDto extends WcsMessageBase {

    private String taskId;
    private String palletId;
    private String itemCode;
    private String lotId;
    private int qty;
    private String status;  // COMPLETED | FAILED
    private String message;
}
