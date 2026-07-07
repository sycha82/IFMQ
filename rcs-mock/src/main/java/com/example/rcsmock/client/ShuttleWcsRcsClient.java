package com.example.rcsmock.client;

import com.example.common.dto.BcrReadDto;
import com.example.common.dto.InboundDoneAckDto;
import com.example.common.dto.InboundDoneDto;
import com.example.common.dto.OutboundDoneAckDto;
import com.example.common.dto.OutboundDoneDto;
import com.example.common.dto.StationStatusDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// RCS/설비ECS → shuttle-wcs 호출용 선언형 클라이언트
@FeignClient(name = "shuttle-wcs-rcs", url = "${rcs.shuttle.base-url}")
public interface ShuttleWcsRcsClient {

    // API 01 · STATION_STATUS
    @PostMapping("/rcs/station-status")
    String sendStationStatus(@RequestBody StationStatusDto dto);

    // API 02 · BCR_READ
    @PostMapping("/rcs/bcr-read")
    String sendBcrRead(@RequestBody BcrReadDto dto);

    // API 05 · INBOUND_DONE → 응답이 곧 API 06 INBOUND_DONE_ACK
    @PostMapping("/rcs/inbound-done")
    InboundDoneAckDto sendInboundDone(@RequestBody InboundDoneDto dto);

    // API 05 · OUTBOUND_DONE → 응답이 곧 API 06 OUTBOUND_DONE_ACK
    @PostMapping("/rcs/outbound-done")
    OutboundDoneAckDto sendOutboundDone(@RequestBody OutboundDoneDto dto);
}
