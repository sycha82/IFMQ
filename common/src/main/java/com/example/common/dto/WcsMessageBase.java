package com.example.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class WcsMessageBase {

    private String messageType;
    private String messageId;
    private String refMessageId;
    private int sequenceNo;
    private LocalDateTime timestamp;
}
