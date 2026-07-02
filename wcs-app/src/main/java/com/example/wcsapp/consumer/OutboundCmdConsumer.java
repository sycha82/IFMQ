package com.example.wcsapp.consumer;

import com.example.common.dto.OutboundCmdAckDto;
import com.example.common.dto.OutboundCmdDto;
import com.example.common.dto.OutboundCmdItem;
import com.example.wcsapp.client.ShuttleWcsClient;
import com.example.wcsapp.config.RabbitMQProperties;
import com.example.wcsapp.producer.OutboundCmdAckProducer;
import com.example.wcsapp.service.MsgLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboundCmdConsumer {

    private final MsgLogService msgLogService;
    private final RabbitMQProperties props;
    private final ShuttleWcsClient shuttleWcsClient;
    private final OutboundCmdAckProducer outboundCmdAckProducer;

    @RabbitListener(queues = "${wcs.rabbitmq.queue.outbound-cmd}")
    public void receive(OutboundCmdDto dto) {
        String queueName = props.getQueue().getOutboundCmd();
        String routingKey = props.getRoutingKey().getOutboundCmd();

        // 1. 수신 즉시 RECEIVED (중복 메시지면 null 반환 → skip)
        Long logId = msgLogService.insertInbound(dto, queueName, routingKey);
        if (logId == null) {
            log.warn("[CONSUMER] OUTBOUND_CMD duplicate, skipped | messageId={}", dto.getMessageId());
            return;
        }
        log.info("[CONSUMER] OUTBOUND_CMD received | messageId={} logId={}", dto.getMessageId(), logId);

        try {
            // 2. 비즈니스 로직 처리 시작 → PROCESSING
            msgLogService.updateProcessing(logId);

            // 3. shuttle-wcs 매핑 조회 위임 → OUTBOUND_CMD_ACK 산출 (Case A)
            OutboundCmdAckDto ack = shuttleWcsClient.notifyOutboundOrder(dto);

            // 4. WMS에 OUTBOUND_CMD_ACK 발행 (if_msg_log OUTBOUND 적재는 Producer가 처리)
            outboundCmdAckProducer.send(ack);

            // 5. 완료 → COMPLETED
            msgLogService.updateCompleted(logId);
            log.info("[CONSUMER] OUTBOUND_CMD completed | messageId={} logId={} ackResult={}",
                    dto.getMessageId(), logId, ack.getResult());
            printReceived(dto, ack);

        } catch (Exception e) {
            // 6. 실패 → FAILED
            msgLogService.updateFailed(logId, e.getMessage());
            log.error("[CONSUMER] OUTBOUND_CMD failed | messageId={} logId={} error={}",
                    dto.getMessageId(), logId, e.getMessage(), e);
        }
    }

    private void printReceived(OutboundCmdDto dto, OutboundCmdAckDto ack) {
        System.out.println("\n[WCS 수신] OUTBOUND_CMD");
        System.out.println("  messageId : " + dto.getMessageId());
        System.out.println("  taskId    : " + dto.getTaskId());
        if (dto.getItems() != null) {
            for (OutboundCmdItem item : dto.getItems()) {
                System.out.printf("  - palletId=%s itemCode=%s lotId=%s pickQty=%d%n",
                        item.getPalletId(), item.getItemCode(), item.getLotId(), item.getPickQty());
            }
        }
        System.out.println("  → ACK result=" + ack.getResult() + " message=" + ack.getMessage());
        System.out.print("선택 > ");
    }
}
