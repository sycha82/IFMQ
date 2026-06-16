package com.example.shuttlewcs.exception;

// 이미 존재하는 입고 주문(동일 taskId+palletId) 재수신 시 발생.
// 동일 taskId+palletId 재전송은 WMS 재시도로만 해석하며 중복 처리한다.
public class InboundOrderConflictException extends RuntimeException {

    public InboundOrderConflictException(String message) {
        super(message);
    }
}
