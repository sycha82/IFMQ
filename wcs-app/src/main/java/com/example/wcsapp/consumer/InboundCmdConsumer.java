package com.example.wcsapp.consumer;

import com.example.wcsapp.client.ShuttleWcsClient;
import com.example.wcsapp.config.RabbitMQProperties;
import com.example.common.dto.InboundCmdDetail;
import com.example.common.dto.InboundCmdDto;
import com.example.wcsapp.service.MsgLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InboundCmdConsumer {

    private final MsgLogService msgLogService;
    private final RabbitMQProperties props;
    private final ShuttleWcsClient shuttleWcsClient;

    @RabbitListener(queues = "${wcs.rabbitmq.queue.inbound-cmd}")
    public void receive(InboundCmdDto dto) {
        String queueName = props.getQueue().getInboundCmd();
        String routingKey = props.getRoutingKey().getInboundCmd();

        // 1. 수신 즉시 RECEIVED (중복 메시지면 null 반환 → skip)
        Long logId = msgLogService.insertInbound(dto, queueName, routingKey);
        if (logId == null) {
            log.warn("[CONSUMER] INBOUND_CMD duplicate, skipped | messageId={}", dto.getMessageId());
            return;
        }
        log.info("[CONSUMER] INBOUND_CMD received | messageId={} logId={}", dto.getMessageId(), logId);

        try {
            // 2. 비즈니스 로직 처리 시작 → PROCESSING
            msgLogService.updateProcessing(logId);

            processInboundCmd(dto);

            // 3. 완료 → COMPLETED
            msgLogService.updateCompleted(logId);
            log.info("[CONSUMER] INBOUND_CMD completed | messageId={} logId={}", dto.getMessageId(), logId);
            printReceived(dto);

        } catch (Exception e) {
            // 4. 실패 → FAILED
            msgLogService.updateFailed(logId, e.getMessage());
            log.error("[CONSUMER] INBOUND_CMD failed | messageId={} logId={} error={}",
                    dto.getMessageId(), logId, e.getMessage(), e);
        }
    }

    private void processInboundCmd(InboundCmdDto dto) {
        log.debug("[CONSUMER] processing | taskId={} detailCount={}",
                dto.getTaskId(),
                dto.getInboundDetail() != null ? dto.getInboundDetail().size() : 0);

        shuttleWcsClient.notifyInboundOrder(dto);
    }

    private void printReceived(InboundCmdDto dto) {
        System.out.println("\n[WCS 수신] INBOUND_CMD");
        System.out.println("  messageId : " + dto.getMessageId());
        System.out.println("  taskId    : " + dto.getTaskId());
        if (dto.getInboundDetail() != null) {
            for (InboundCmdDetail d : dto.getInboundDetail()) {
                System.out.printf("  [seq=%d] palletId=%s itemCode=%s lotId=%s qty=%d expireDate=%s%n",
                        d.getSequenceNo(), d.getPalletId(), d.getItemCode(),
                        d.getLotId(), d.getQty(), d.getExpireDate());
            }
        }
        System.out.print("선택 > ");
    }
}
