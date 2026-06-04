package com.example.wcsapp.consumer;

import com.example.wcsapp.dto.InboundCmdDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InboundCmdConsumer {

    @RabbitListener(queues = "${wcs.rabbitmq.queue.inbound-cmd}")
    public void receive(InboundCmdDto dto) {
        log.info("[CONSUMER] INBOUND_CMD received | messageId={} taskId={} palletId={} itemCode={} lotId={} qty={} expireDate={}",
                dto.getMessageId(),
                dto.getTaskId(),
                dto.getPalletId(),
                dto.getItemCode(),
                dto.getLotId(),
                dto.getQty(),
                dto.getExpireDate());
    }
}
