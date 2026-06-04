package com.example.wcsapp.service;

import com.example.wcsapp.db.IfMsgLog;
import com.example.wcsapp.db.IfMsgLogMapper;
import com.example.wcsapp.dto.WcsMessageBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MsgLogService {

    private final IfMsgLogMapper mapper;
    private final ObjectMapper objectMapper;

    /**
     * INBOUND 수신 즉시 RECEIVED 상태로 insert.
     * @return 생성된 logId
     */
    public Long insertInbound(WcsMessageBase dto, String queueName, String routingKey) {
        IfMsgLog record = buildBase(dto, "INBOUND", routingKey, queueName, "RECEIVED");
        mapper.insert(record);
        return record.getLogId();
    }

    /**
     * OUTBOUND 발행 직전 COMPLETED 상태로 insert.
     * @return 생성된 logId
     */
    public Long insertOutbound(WcsMessageBase dto, String routingKey) {
        IfMsgLog record = buildBase(dto, "OUTBOUND", routingKey, null, "COMPLETED");
        mapper.insert(record);
        return record.getLogId();
    }

    /**
     * OUTBOUND 발행 실패 시 FAILED 상태로 insert.
     * @return 생성된 logId
     */
    public Long insertOutboundFailed(WcsMessageBase dto, String routingKey, String errorMsg) {
        IfMsgLog record = buildBase(dto, "OUTBOUND", routingKey, null, "FAILED");
        record.setErrorMsg(errorMsg);
        mapper.insert(record);
        return record.getLogId();
    }

    public void updateProcessing(Long logId) {
        mapper.updateStatus(logId, "PROCESSING", null, null);
    }

    public void updateCompleted(Long logId) {
        mapper.updateStatus(logId, "COMPLETED", null, LocalDateTime.now());
    }

    public void updateFailed(Long logId, String errorMsg) {
        mapper.updateStatus(logId, "FAILED", errorMsg, LocalDateTime.now());
    }

    private IfMsgLog buildBase(WcsMessageBase dto, String direction,
                               String routingKey, String queueName, String status) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(dto);
        } catch (Exception e) {
            log.warn("payload 직렬화 실패: {}", e.getMessage());
            payload = "{}";
        }

        return IfMsgLog.builder()
                .direction(direction)
                .messageType(dto.getMessageType())
                .messageId(dto.getMessageId())
                .refMessageId(dto.getRefMessageId())
                .sequenceNo(dto.getSequenceNo())
                .msgTimestamp(dto.getTimestamp())
                .routingKey(routingKey)
                .queueName(queueName)
                .payload(payload)
                .status(status)
                .build();
    }
}
