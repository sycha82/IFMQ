package com.example.wcsapp.producer;

import com.example.wcsapp.config.RabbitMQProperties;
import com.example.wcsapp.dto.InboundCompleteDto;
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

    public void send(InboundCompleteDto dto) {
        String routingKey = props.getRoutingKey().getInboundComplete();
        rabbitTemplate.convertAndSend(props.getExchange(), routingKey, dto);
        log.info("[PRODUCER] INBOUND_COMPLETE sent | messageId={} taskId={} status={}",
                dto.getMessageId(), dto.getTaskId(), dto.getStatus());
    }
}
