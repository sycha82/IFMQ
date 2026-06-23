package com.example.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InboundCmdDetail {

    private int lineNo;
    private String palletId;
    private String itemCode;
    private String lotId;
    private int qty;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDate expireDate;
}
