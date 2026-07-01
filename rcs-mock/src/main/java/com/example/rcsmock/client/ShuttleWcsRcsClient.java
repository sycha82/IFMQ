package com.example.rcsmock.client;

import com.example.common.dto.BcrReadDto;
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
}
