package com.example.shuttlewcs.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 입고 주문 재수신(중복) → 409 Conflict
    @ExceptionHandler(InboundOrderConflictException.class)
    public ResponseEntity<String> handleConflict(InboundOrderConflictException e) {
        log.warn("[CONFLICT] {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }
}
