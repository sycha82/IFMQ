package com.example.wcsapp.consumer;

import com.example.wcsapp.config.RabbitMQProperties;
import com.example.common.dto.InboundCancelDto;
import com.example.wcsapp.service.MsgLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InboundCancelConsumer {

    private final MsgLogService msgLogService;
    private final RabbitMQProperties props;

    @RabbitListener(queues = "${wcs.rabbitmq.queue.inbound-cancel}")
    public void receive(InboundCancelDto dto) {
        String queueName = props.getQueue().getInboundCancel();
        String routingKey = props.getRoutingKey().getInboundCancel();

        // 1. 수신 즉시 RECEIVED (중복 메시지면 null 반환 → skip)
        Long logId = msgLogService.insertInbound(dto, queueName, routingKey);
        if (logId == null) {
            log.warn("[CONSUMER] INBOUND_CANCEL duplicate, skipped | messageId={}", dto.getMessageId());
            return;
        }
        log.info("[CONSUMER] INBOUND_CANCEL received | messageId={} logId={}", dto.getMessageId(), logId);

        try {
            // 2. 비즈니스 로직 처리 시작 → PROCESSING
            msgLogService.updateProcessing(logId);

            processInboundCancel(dto);

            // 3. 완료 → COMPLETED
            msgLogService.updateCompleted(logId);
            log.info("[CONSUMER] INBOUND_CANCEL completed | messageId={} logId={}", dto.getMessageId(), logId);
            printReceived(dto);

        } catch (Exception e) {
            // 4. 실패 → FAILED
            msgLogService.updateFailed(logId, e.getMessage());
            log.error("[CONSUMER] INBOUND_CANCEL failed | messageId={} logId={} error={}",
                    dto.getMessageId(), logId, e.getMessage(), e);
        }
    }

    private void processInboundCancel(InboundCancelDto dto) {
        log.debug("[CONSUMER] processing | taskId={} palletId={} reason={}",
                dto.getTaskId(), dto.getPalletId(), dto.getReason());
        // TODO: 실제 비즈니스 로직 구현 (입고 취소 처리)
    }

    private void printReceived(InboundCancelDto dto) {
        System.out.println("\n[WCS 수신] INBOUND_CANCEL");
        System.out.println("  messageId : " + dto.getMessageId());
        System.out.println("  taskId    : " + dto.getTaskId());
        System.out.println("  palletId  : " + dto.getPalletId());
        System.out.println("  reason    : " + dto.getReason());
        System.out.println("  timestamp : " + dto.getTimestamp());
        System.out.print("선택 > ");
    }
}
