package com.example.shuttlewcs.controller;

import com.example.shuttlewcs.service.MonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 모니터링 대시보드용 조회 API (읽기 전용)
// 주의: 이 프로젝트는 -parameters 없이 컴파일되므로 @RequestParam 이름을 반드시 명시한다(docs/05 규칙 6).
@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;

    @GetMapping("/snapshot")
    public ResponseEntity<MonitorService.Snapshot> snapshot(
            @RequestParam(name = "taskLimit", defaultValue = "30") int taskLimit,
            @RequestParam(name = "orderLimit", defaultValue = "10") int orderLimit) {

        int tl = Math.min(Math.max(taskLimit, 1), 200);
        int ol = Math.min(Math.max(orderLimit, 1), 100);
        return ResponseEntity.ok(monitorService.snapshot(tl, ol));
    }
}
