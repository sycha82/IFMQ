package com.example.wmsmock.consumer;

import com.example.common.dto.InboundCompleteDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InboundCompleteConsumer {

    @RabbitListener(queues = "${wms.rabbitmq.queue.inbound-complete}")
    public void receive(InboundCompleteDto dto) {
        log.info("[WMS] INBOUND_COMPLETE received | messageId={}", dto.getMessageId());
        System.out.println("\n[WMS 수신] INBOUND_COMPLETE");
        System.out.println("  messageId : " + dto.getMessageId());
        System.out.println("  taskId    : " + dto.getTaskId());
        System.out.println("  palletId  : " + dto.getPalletId());
        System.out.println("  itemCode  : " + dto.getItemCode());
        System.out.println("  lotId     : " + dto.getLotId());
        System.out.println("  qty       : " + dto.getQty());
        System.out.println("  status    : " + dto.getStatus());
        System.out.println("  message   : " + dto.getMessage());
        System.out.println("  timestamp : " + dto.getTimestamp());
        System.out.print("선택 > ");
    }
}
