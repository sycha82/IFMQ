package com.example.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class InboundCmdDto extends WcsMessageBase {

    private String taskId;
    private List<InboundCmdDetail> inboundDetail;
}
