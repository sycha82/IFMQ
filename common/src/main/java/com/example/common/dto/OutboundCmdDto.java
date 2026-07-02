package com.example.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

// OUTBOUND_CMD (WMS → WCS) · API 01 — 출고 지시 (Case A · PalletId 지정)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class OutboundCmdDto extends WcsMessageBase {

    private String taskId;
    private List<OutboundCmdItem> items;
}
