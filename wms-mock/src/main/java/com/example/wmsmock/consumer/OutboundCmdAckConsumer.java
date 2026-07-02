package com.example.wmsmock.consumer;

import com.example.common.dto.OutboundCmdAckDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OutboundCmdAckConsumer {

    @RabbitListener(queues = "${wms.rabbitmq.queue.outbound-cmd-ack}")
    public void receive(OutboundCmdAckDto dto) {
        log.info("[WMS] OUTBOUND_CMD_ACK received | messageId={} result={}",
                dto.getMessageId(), dto.getResult());
        System.out.println("\n[WMS 수신] OUTBOUND_CMD_ACK");
        System.out.println("  messageId    : " + dto.getMessageId());
        System.out.println("  refMessageId : " + dto.getRefMessageId());
        System.out.println("  taskId       : " + dto.getTaskId());
        System.out.println("  result       : " + dto.getResult());
        System.out.println("  message      : " + dto.getMessage());
        System.out.println("  timestamp    : " + dto.getTimestamp());
        System.out.print("선택 > ");
    }
}
