package com.example.wcsapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class InboundCmdDto extends WcsMessageBase {

    private String taskId;
    private String palletId;
    private String itemCode;
    private String lotId;
    private int qty;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDate expireDate;
}
