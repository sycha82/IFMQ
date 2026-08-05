package com.example.shuttlewcs.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * inf.if_msg_log(WMS↔WCS MQ 전문 이력) 읽기 전용 모델 — 모니터링 화면 표시용.
 *
 * <p>이 테이블의 <b>적재 주체는 wcs-app</b> 이다(CLAUDE.md 규칙 2). shuttle-wcs 는 대시보드에
 * WMS 구간 전문을 함께 보여주기 위해 <b>조회만</b> 한다. 절대 여기서 INSERT/UPDATE 하지 않는다.
 * (두 모듈이 같은 DB(wcs)를 쓰기 때문에 스키마 한정 조회로 접근 가능)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IfMsgLogView {

    private Long logId;
    private String direction;        // INBOUND(WMS→WCS) | OUTBOUND(WCS→WMS)
    private String messageType;
    private String messageId;
    private String refMessageId;
    private String routingKey;
    private String queueName;
    private String payload;
    private String status;           // RECEIVED | PROCESSING | COMPLETED | FAILED
    private String errorMsg;
    private LocalDateTime createdAt;
}
