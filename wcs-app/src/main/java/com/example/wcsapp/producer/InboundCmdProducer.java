package com.example.wcsapp.producer;

import com.example.wcsapp.config.RabbitMQProperties;
import com.example.wcsapp.dto.InboundCmdDto;
import com.example.wcsapp.service.MsgLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InboundCmdProducer {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties props;
    private final MsgLogService msgLogService;

    public void send(InboundCmdDto dto) {
        String routingKey = props.getRoutingKey().getInboundCmd();
        try {
            rabbitTemplate.convertAndSend(props.getExchange(), routingKey, dto);
            msgLogService.insertOutbound(dto, routingKey);
            log.info("[PRODUCER] INBOUND_CMD sent | messageId={} taskId={}",
                    dto.getMessageId(), dto.getTaskId());
        } catch (Exception e) {
            msgLogService.insertOutboundFailed(dto, routingKey, e.getMessage());
            log.error("[PRODUCER] INBOUND_CMD send failed | messageId={} error={}",
                    dto.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
}
