package com.example.shuttlewcs.service;

import com.example.shuttlewcs.db.WcsRcsMsgLog;
import com.example.shuttlewcs.db.WcsRcsMsgLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RcsMsgLogService {

    private final WcsRcsMsgLogMapper mapper;
    private final ObjectMapper objectMapper;

    public void logReceive(String apiName, String messageId, String refMessageId,
                           String stationId, String eqpPalletId, String wcsTaskId,
                           Object payloadDto, String result) {
        insert("RECEIVE", apiName, messageId, refMessageId, stationId, eqpPalletId, wcsTaskId, payloadDto, result);
    }

    public void logSend(String apiName, String messageId, String refMessageId,
                        String stationId, String eqpPalletId, String wcsTaskId,
                        Object payloadDto, String result) {
        insert("SEND", apiName, messageId, refMessageId, stationId, eqpPalletId, wcsTaskId, payloadDto, result);
    }

    private void insert(String direction, String apiName, String messageId, String refMessageId,
                        String stationId, String eqpPalletId, String wcsTaskId,
                        Object payloadDto, String result) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(payloadDto);
        } catch (Exception e) {
            log.warn("[RCS_LOG] payload 직렬화 실패: {}", e.getMessage());
            payload = "{}";
        }

        mapper.insert(WcsRcsMsgLog.builder()
                .direction(direction)
                .apiName(apiName)
                .messageId(messageId)
                .refMessageId(refMessageId)
                .stationId(stationId)
                .eqpPalletId(eqpPalletId)
                .wcsTaskId(wcsTaskId)
                .payload(payload)
                .result(result)
                .build());
    }
}
