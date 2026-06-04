package com.example.wcsapp.controller;

import com.example.common.dto.InboundCmdDto;
import com.example.common.dto.InboundCompleteDto;
import com.example.wcsapp.producer.InboundCmdProducer;
import com.example.wcsapp.producer.InboundCompleteProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final InboundCmdProducer inboundCmdProducer;
    private final InboundCompleteProducer inboundCompleteProducer;

    @PostMapping("/inbound-cmd")
    public ResponseEntity<String> publishInboundCmd(
            @RequestBody(required = false) InboundCmdDto body) {

        InboundCmdDto dto = (body != null) ? body : defaultInboundCmd();
        inboundCmdProducer.send(dto);
        return ResponseEntity.ok("INBOUND_CMD published: " + dto.getMessageId());
    }

    @PostMapping("/inbound-complete")
    public ResponseEntity<String> publishInboundComplete(
            @RequestBody(required = false) InboundCompleteDto body) {

        InboundCompleteDto dto = (body != null) ? body : defaultInboundComplete();
        inboundCompleteProducer.send(dto);
        return ResponseEntity.ok("INBOUND_COMPLETE published: " + dto.getMessageId());
    }

    private InboundCmdDto defaultInboundCmd() {
        return InboundCmdDto.builder()
                .messageType("INBOUND_CMD")
                .messageId("MSG-20260422-0000")
                .refMessageId(null)
                .sequenceNo(999)
                .timestamp(LocalDateTime.of(2026, 4, 22, 8, 50, 0))
                .taskId("WMS-IN-20260422-001")
                .palletId("PLT-20260422-001")
                .itemCode("ITEM-20260422-001")
                .lotId("LOT-20260422-001")
                .qty(24)
                .expireDate(LocalDate.of(2027, 4, 22))
                .build();
    }

    private InboundCompleteDto defaultInboundComplete() {
        return InboundCompleteDto.builder()
                .messageType("INBOUND_COMPLETE")
                .messageId("MSG-20260422-0007")
                .refMessageId(null)
                .sequenceNo(1006)
                .timestamp(LocalDateTime.of(2026, 4, 22, 9, 5, 32))
                .taskId("WMS-IN-20260422-001")
                .palletId("PLT-20260422-001")
                .itemCode("ITEM-20260422-001")
                .lotId("LOT-20260422-001")
                .qty(24)
                .status("COMPLETED")
                .message("")
                .build();
    }
}
