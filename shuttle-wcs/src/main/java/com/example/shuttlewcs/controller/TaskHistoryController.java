package com.example.shuttlewcs.controller;

import com.example.shuttlewcs.db.WcsTaskH;
import com.example.shuttlewcs.service.TaskHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 셔틀 작업(TASK) 이력 조회 API — 사용자가 전문 payload 없이 작업 진행 상태를 확인하는 용도
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskHistoryController {

    private static final int MAX_LIMIT = 500;

    private final TaskHistoryService taskHistoryService;

    /**
     * 최근 작업 이력 조회.
     * 예) GET /api/tasks?taskType=INBOUND&taskStatus=COMPLETED&eqpPalletId=EP0001&limit=20
     */
    @GetMapping
    public ResponseEntity<List<WcsTaskH>> findRecent(
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) String taskStatus,
            @RequestParam(required = false) String eqpPalletId,
            @RequestParam(defaultValue = "50") int limit) {

        int capped = Math.min(Math.max(limit, 1), MAX_LIMIT);
        return ResponseEntity.ok(taskHistoryService.findRecent(taskType, taskStatus, eqpPalletId, capped));
    }

    // 특정 wcsTaskId 조회 — 입고/출고가 같은 값을 공유하므로 최대 2건(INBOUND·OUTBOUND) 반환
    @GetMapping("/{wcsTaskId}")
    public ResponseEntity<List<WcsTaskH>> findByWcsTaskId(@PathVariable String wcsTaskId) {
        return ResponseEntity.ok(taskHistoryService.findByWcsTaskId(wcsTaskId));
    }
}
