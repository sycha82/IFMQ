package com.example.wmsmock.producer;

import com.example.common.dto.OutboundCmdDto;
import com.example.wmsmock.config.WmsRabbitMQProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboundCmdPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final WmsRabbitMQProperties props;

    public void send(OutboundCmdDto dto) {
        String routingKey = props.getRoutingKey().getOutboundCmd();
        rabbitTemplate.convertAndSend(props.getExchange(), routingKey, dto);
        log.info("[WMS] OUTBOUND_CMD sent | messageId={} taskId={}", dto.getMessageId(), dto.getTaskId());
    }
}
