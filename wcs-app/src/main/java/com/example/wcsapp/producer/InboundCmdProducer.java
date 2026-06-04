package com.example.wcsapp.producer;

import com.example.wcsapp.config.RabbitMQProperties;
import com.example.wcsapp.dto.InboundCmdDto;
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

    public void send(InboundCmdDto dto) {
        String routingKey = props.getRoutingKey().getInboundCmd();
        rabbitTemplate.convertAndSend(props.getExchange(), routingKey, dto);
        log.info("[PRODUCER] INBOUND_CMD sent | messageId={} taskId={}",
                dto.getMessageId(), dto.getTaskId());
    }
}
