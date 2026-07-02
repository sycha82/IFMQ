package com.example.wcsapp.producer;

import com.example.common.dto.OutboundCmdAckDto;
import com.example.wcsapp.config.RabbitMQProperties;
import com.example.wcsapp.service.MsgLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboundCmdAckProducer {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties props;
    private final MsgLogService msgLogService;

    public void send(OutboundCmdAckDto dto) {
        String routingKey = props.getRoutingKey().getOutboundCmdAck();
        try {
            rabbitTemplate.convertAndSend(props.getExchange(), routingKey, dto);
            msgLogService.insertOutbound(dto, routingKey);
            log.info("[PRODUCER] OUTBOUND_CMD_ACK sent | messageId={} taskId={} result={}",
                    dto.getMessageId(), dto.getTaskId(), dto.getResult());
        } catch (Exception e) {
            msgLogService.insertOutboundFailed(dto, routingKey, e.getMessage());
            log.error("[PRODUCER] OUTBOUND_CMD_ACK send failed | messageId={} error={}",
                    dto.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
}
