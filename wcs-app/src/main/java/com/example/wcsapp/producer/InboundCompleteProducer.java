package com.example.wcsapp.producer;

import com.example.wcsapp.config.RabbitMQProperties;
import com.example.common.dto.InboundCompleteDto;
import com.example.wcsapp.service.MsgLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InboundCompleteProducer {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties props;
    private final MsgLogService msgLogService;

    public void send(InboundCompleteDto dto) {
        String routingKey = props.getRoutingKey().getInboundComplete();
        try {
            rabbitTemplate.convertAndSend(props.getExchange(), routingKey, dto);
            msgLogService.insertOutbound(dto, routingKey);
            log.info("[PRODUCER] INBOUND_COMPLETE sent | messageId={} taskId={} status={}",
                    dto.getMessageId(), dto.getTaskId(), dto.getStatus());
        } catch (Exception e) {
            msgLogService.insertOutboundFailed(dto, routingKey, e.getMessage());
            log.error("[PRODUCER] INBOUND_COMPLETE send failed | messageId={} error={}",
                    dto.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
}
