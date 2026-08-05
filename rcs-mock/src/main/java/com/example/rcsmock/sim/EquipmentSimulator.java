package com.example.rcsmock.sim;

import com.example.common.dto.InboundDoneDto;
import com.example.common.dto.InboundStartDto;
import com.example.common.dto.InboundTaskDto;
import com.example.common.dto.OutboundDoneDto;
import com.example.common.dto.OutboundStartDto;
import com.example.common.dto.OutboundTaskDto;
import com.example.rcsmock.client.ShuttleWcsRcsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 설비 동작 자동 시뮬레이터.
 *
 * <p>WCS로부터 TASK(INBOUND_TASK/OUTBOUND_TASK)를 수신하면, 실제 설비가 작업을 수행하는 것처럼
 * 일정 시간 뒤 START → DONE 을 자동으로 WCS에 보고한다.
 *
 * <p><b>트리거가 TASK 수신 시점인 이유</b> — START/DONE 전문에는 {@code wcsTaskId} 가 필수인데,
 * 이 값은 WCS가 채번해서 TASK 전문에 실어 보내주는 값이다. BCR_READ 시점에는 알 수 없다.
 *
 * <p><b>ACK 를 절대 블로킹하지 않는다</b> — WCS의 BCR_READ 처리 트랜잭션 안에서 TASK 전송이
 * 일어나므로, 여기서 응답을 지연시키면 그 트랜잭션이 열린 채 대기하게 된다.
 * 예약만 걸고 즉시 반환해야 하며, 지연시간도 WCS 트랜잭션이 커밋될 만큼은 확보되어야 한다
 * (커밋 전에 START 가 도착하면 WCS가 작업 이력을 찾지 못해 거부한다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EquipmentSimulator {

    private final ShuttleWcsRcsClient shuttleWcsRcsClient;
    private final TaskScheduler taskScheduler;
    private final AutoSimulationProperties props;

    // 입고: TASK 수신 → (startDelay) INBOUND_START → (doneDelay) INBOUND_DONE
    public void scheduleInbound(InboundTaskDto task, String shuttleId) {
        if (!props.isEnabled()) {
            return;
        }
        String sid = (shuttleId != null) ? shuttleId : props.getDefaultShuttleId();
        Instant startAt = Instant.now().plusMillis(props.getStartDelayMs());
        Instant doneAt = startAt.plusMillis(props.getDoneDelayMs());

        log.info("[SIM] 입고 자동 시뮬레이션 예약 | wcsTaskId={} START(+{}ms) DONE(+{}ms)",
                task.getWcsTaskId(), props.getStartDelayMs(),
                props.getStartDelayMs() + props.getDoneDelayMs());

        taskScheduler.schedule(() -> sendInboundStart(task, sid), startAt);
        taskScheduler.schedule(() -> sendInboundDone(task, sid), doneAt);
    }

    // 출고: TASK 수신 → (startDelay) OUTBOUND_START → (doneDelay) OUTBOUND_DONE
    public void scheduleOutbound(OutboundTaskDto task, String shuttleId) {
        if (!props.isEnabled()) {
            return;
        }
        String sid = (shuttleId != null) ? shuttleId : props.getDefaultShuttleId();
        Instant startAt = Instant.now().plusMillis(props.getStartDelayMs());
        Instant doneAt = startAt.plusMillis(props.getDoneDelayMs());

        log.info("[SIM] 출고 자동 시뮬레이션 예약 | wcsTaskId={} START(+{}ms) DONE(+{}ms)",
                task.getWcsTaskId(), props.getStartDelayMs(),
                props.getStartDelayMs() + props.getDoneDelayMs());

        taskScheduler.schedule(() -> sendOutboundStart(task, sid), startAt);
        taskScheduler.schedule(() -> sendOutboundDone(task, sid), doneAt);
    }

    private void sendInboundStart(InboundTaskDto task, String shuttleId) {
        run("INBOUND_START", task.getWcsTaskId(), () ->
                shuttleWcsRcsClient.sendInboundStart(InboundStartDto.builder()
                        .messageType("INBOUND_START")
                        .messageId(UUID.randomUUID().toString())
                        .refMessageId(task.getMessageId())
                        .sequenceNo(1)
                        .timestamp(LocalDateTime.now())
                        .stationId(task.getStationId())
                        .eqpPalletId(task.getEqpPalletId())
                        .wcsTaskId(task.getWcsTaskId())
                        .shuttleId(shuttleId)
                        .build()));
    }

    private void sendInboundDone(InboundTaskDto task, String shuttleId) {
        run("INBOUND_DONE", task.getWcsTaskId(), () ->
                shuttleWcsRcsClient.sendInboundDone(InboundDoneDto.builder()
                        .messageType("INBOUND_DONE")
                        .messageId(UUID.randomUUID().toString())
                        .refMessageId(task.getMessageId())
                        .sequenceNo(1)
                        .timestamp(LocalDateTime.now())
                        .wcsTaskId(task.getWcsTaskId())
                        .eqpPalletId(task.getEqpPalletId())
                        .shuttleId(shuttleId)
                        .status("COMPLETED")
                        .failReason(null)
                        .build()));
    }

    private void sendOutboundStart(OutboundTaskDto task, String shuttleId) {
        run("OUTBOUND_START", task.getWcsTaskId(), () ->
                shuttleWcsRcsClient.sendOutboundStart(OutboundStartDto.builder()
                        .messageType("OUTBOUND_START")
                        .messageId(UUID.randomUUID().toString())
                        .refMessageId(task.getMessageId())
                        .sequenceNo(1)
                        .timestamp(LocalDateTime.now())
                        .wcsTaskId(task.getWcsTaskId())
                        .eqpPalletId(task.getEqpPalletId())
                        .shuttleId(shuttleId)
                        .destStation(task.getDestStation())
                        .build()));
    }

    private void sendOutboundDone(OutboundTaskDto task, String shuttleId) {
        run("OUTBOUND_DONE", task.getWcsTaskId(), () ->
                shuttleWcsRcsClient.sendOutboundDone(OutboundDoneDto.builder()
                        .messageType("OUTBOUND_DONE")
                        .messageId(UUID.randomUUID().toString())
                        .refMessageId(task.getMessageId())
                        .sequenceNo(1)
                        .timestamp(LocalDateTime.now())
                        .wcsTaskId(task.getWcsTaskId())
                        .eqpPalletId(task.getEqpPalletId())
                        .shuttleId(shuttleId)
                        .destStation(task.getDestStation())
                        .status("COMPLETED")
                        .failReason(null)
                        .build()));
    }

    /**
     * 시뮬레이션 전송은 실패해도 삼킨다 — 목업 스케줄러가 죽으면 안 되고,
     * 수동 조작으로 이미 진행된 작업이면 WCS가 상태 검증으로 거부하는 게 정상이기 때문.
     */
    private void run(String api, String wcsTaskId, Runnable action) {
        try {
            action.run();
            log.info("[SIM] {} 자동 전송 완료 | wcsTaskId={}", api, wcsTaskId);
            System.out.println("[SIM] " + api + " 자동 전송 | wcsTaskId=" + wcsTaskId);
        } catch (Exception e) {
            log.warn("[SIM] {} 자동 전송 실패(무시) | wcsTaskId={} error={}", api, wcsTaskId, e.getMessage());
        }
    }
}
