package com.example.shuttlewcs.exception;

// RCS/설비ECS 연계 프로토콜 검증 실패 (스테이션 상태·매핑 부재 등)
public class RcsProtocolException extends RuntimeException {

    public RcsProtocolException(String message) {
        super(message);
    }
}
