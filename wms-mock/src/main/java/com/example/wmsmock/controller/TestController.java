package com.example.wmsmock.controller;

import com.example.common.dto.InboundCancelDto;
import com.example.common.dto.InboundCmdDetail;
import com.example.common.dto.InboundCmdDto;
import com.example.common.dto.OutboundCmdDto;
import com.example.common.dto.OutboundCmdItem;
import com.example.wmsmock.producer.InboundCancelPublisher;
import com.example.wmsmock.producer.InboundCmdPublisher;
import com.example.wmsmock.producer.OutboundCmdPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// 도커 컨테이너 등 대화형 CLI 사용이 불편한 환경을 위한 REST 발행 엔드포인트.
// WmsCliRunner 메뉴와 1:1 대응 (기본값도 동일).
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final InboundCmdPublisher inboundCmdPublisher;
    private final InboundCancelPublisher inboundCancelPublisher;
    private final OutboundCmdPublisher outboundCmdPublisher;

    @PostMapping("/inbound-cmd")
    public ResponseEntity<String> publishInboundCmd(@RequestBody(required = false) InboundCmdDto body) {
        InboundCmdDto dto = (body != null) ? body : defaultInboundCmd();
        inboundCmdPublisher.send(dto);
        return ResponseEntity.ok("INBOUND_CMD published: " + dto.getMessageId());
    }

    @PostMapping("/inbound-cancel")
    public ResponseEntity<String> publishInboundCancel(@RequestBody(required = false) InboundCancelDto body) {
        InboundCancelDto dto = (body != null) ? body : defaultInboundCancel();
        inboundCancelPublisher.send(dto);
        return ResponseEntity.ok("INBOUND_CANCEL published: " + dto.getMessageId());
    }

    @PostMapping("/outbound-cmd")
    public ResponseEntity<String> publishOutboundCmd(@RequestBody(required = false) OutboundCmdDto body) {
        OutboundCmdDto dto = (body != null) ? body : defaultOutboundCmd();
        outboundCmdPublisher.send(dto);
        return ResponseEntity.ok("OUTBOUND_CMD published: " + dto.getMessageId());
    }

    private InboundCmdDto defaultInboundCmd() {
        return InboundCmdDto.builder()
                .messageType("INBOUND_CMD")
                .messageId("MSG-20260422-0000")
                .refMessageId(null)
                .sequenceNo(1)
                .timestamp(LocalDateTime.of(2026, 4, 22, 8, 50, 0))
                .taskId("WMS-IN-20260422-001")
                .inboundDetail(List.of(
                        InboundCmdDetail.builder()
                                .lineNo(1)
                                .palletId("PLT-20260422-001")
                                .itemCode("ITEM-20260422-001")
                                .lotId("LOT-20260422-001")
                                .qty(24)
                                .expireDate(LocalDate.of(2027, 4, 22))
                                .build()
                ))
                .build();
    }

    private InboundCancelDto defaultInboundCancel() {
        return InboundCancelDto.builder()
                .messageType("INBOUND_CANCEL")
                .messageId("MSG-CANCEL-0001")
                .refMessageId(null)
                .sequenceNo(1)
                .timestamp(LocalDateTime.of(2026, 4, 22, 9, 10, 0))
                .taskId("WMS-IN-20260422-001")
                .palletId("PLT-20260422-001")
                .reason("재고 오류로 인한 입고 취소")
                .build();
    }

    private OutboundCmdDto defaultOutboundCmd() {
        return OutboundCmdDto.builder()
                .messageType("OUTBOUND_CMD")
                .messageId("MSG-OUT-20260422-0001")
                .refMessageId(null)
                .sequenceNo(1)
                .timestamp(LocalDateTime.of(2026, 4, 22, 10, 0, 0))
                .taskId("WMS-OUT-20260422-001")
                .items(List.of(
                        OutboundCmdItem.builder()
                                .palletId("PLT-20260422-001")
                                .itemCode("ITEM-20260422-001")
                                .lotId("LOT-20260422-001")
                                .pickQty(24)
                                .build()
                ))
                .build();
    }
}
