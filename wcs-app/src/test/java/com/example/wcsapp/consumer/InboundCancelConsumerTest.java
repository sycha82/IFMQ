package com.example.wcsapp.consumer;

import com.example.common.dto.InboundCancelDto;
import com.example.wcsapp.config.RabbitMQProperties;
import com.example.wcsapp.service.MsgLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

/**
 * InboundCancelConsumer 순수 단위 테스트 (Mockito)
 * - MsgLogService를 모킹하여 멱등 처리/상태전이 호출 흐름을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class InboundCancelConsumerTest {

    private static final String QUEUE_NAME = "wcs.queue.inbound.cancel";
    private static final String ROUTING_KEY = "wms.inbound.cancel";

    @Mock
    private MsgLogService msgLogService;

    @InjectMocks
    private InboundCancelConsumer inboundCancelConsumer;

    private RabbitMQProperties props;

    @BeforeEach
    void setUp() {
        // RabbitMQProperties는 단순 POJO이므로 직접 생성 후 setter로 값 채워서 주입한다.
        props = new RabbitMQProperties();
        props.setExchange("wcs.topic");

        RabbitMQProperties.Queue queue = new RabbitMQProperties.Queue();
        queue.setInboundCmd("wcs.queue.inbound.cmd");
        queue.setInboundCancel(QUEUE_NAME);
        props.setQueue(queue);

        RabbitMQProperties.RoutingKey routingKey = new RabbitMQProperties.RoutingKey();
        routingKey.setInboundCmd("wms.inbound.cmd");
        routingKey.setInboundComplete("wcs.inbound.complete");
        routingKey.setInboundCancel(ROUTING_KEY);
        props.setRoutingKey(routingKey);

        // @InjectMocks가 final 필드(생성자 주입)에 props를 채우지 못하므로 직접 새 인스턴스를 만들어 사용한다.
        inboundCancelConsumer = new InboundCancelConsumer(msgLogService, props);
    }

    private InboundCancelDto sampleDto() {
        return InboundCancelDto.builder()
                .messageType("INBOUND_CANCEL")
                .messageId("MSG-CANCEL-0001")
                .refMessageId(null)
                .sequenceNo(1)
                .timestamp(LocalDateTime.now())
                .taskId("TASK-001")
                .palletId("PLT-001")
                .reason("오입고")
                .build();
    }

    @Test
    @DisplayName("정상 수신 시 RECEIVED → PROCESSING → COMPLETED 순서로 상태가 전이된다")
    void receive_정상처리_상태전이_순서검증() {
        // given
        InboundCancelDto dto = sampleDto();
        when(msgLogService.insertInbound(eq(dto), eq(QUEUE_NAME), eq(ROUTING_KEY))).thenReturn(100L);

        // when
        inboundCancelConsumer.receive(dto);

        // then
        InOrder inOrder = inOrder(msgLogService);
        inOrder.verify(msgLogService).insertInbound(dto, QUEUE_NAME, ROUTING_KEY);
        inOrder.verify(msgLogService).updateProcessing(100L);
        inOrder.verify(msgLogService).updateCompleted(100L);

        verify(msgLogService, never()).updateFailed(anyLong(), anyString());
    }

    @Test
    @DisplayName("중복 메시지(insertInbound가 null 반환) 수신 시 처리를 스킵하고 상태전이를 호출하지 않는다")
    void receive_중복메시지_멱등성_스킵검증() {
        // given
        InboundCancelDto dto = sampleDto();
        when(msgLogService.insertInbound(eq(dto), eq(QUEUE_NAME), eq(ROUTING_KEY))).thenReturn(null);

        // when
        inboundCancelConsumer.receive(dto);

        // then
        verify(msgLogService).insertInbound(dto, QUEUE_NAME, ROUTING_KEY);
        verify(msgLogService, never()).updateProcessing(anyLong());
        verify(msgLogService, never()).updateCompleted(anyLong());
        verify(msgLogService, never()).updateFailed(anyLong(), anyString());
    }

    @Test
    @DisplayName("PROCESSING 갱신 중 예외가 발생하면 updateFailed가 호출되고 COMPLETED는 호출되지 않는다")
    void receive_처리중_예외발생_FAILED처리검증() {
        // given
        InboundCancelDto dto = sampleDto();
        when(msgLogService.insertInbound(eq(dto), eq(QUEUE_NAME), eq(ROUTING_KEY))).thenReturn(200L);
        doThrow(new RuntimeException("강제 예외 발생")).when(msgLogService).updateProcessing(200L);

        // when
        inboundCancelConsumer.receive(dto);

        // then
        verify(msgLogService).updateProcessing(200L);
        verify(msgLogService).updateFailed(eq(200L), anyString());
        verify(msgLogService, never()).updateCompleted(anyLong());
    }
}
