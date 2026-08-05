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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    private static final String NO_STATION = "(none)";

    private final ShuttleWcsRcsClient shuttleWcsRcsClient;
    private final TaskScheduler taskScheduler;
    private final AutoSimulationProperties props;

    /**
     * 스테이션별 "다음 팔렛이 착수 가능한 가장 이른 시각" 커서.
     * 한 스테이션은 동시에 여러 팔렛을 처리할 수 없으므로 작업을 직렬화한다.
     * 입고 스테이션과 출고 스테이션은 키가 달라 서로 간섭하지 않는다.
     */
    private final Map<String, Instant> stationCursor = new ConcurrentHashMap<>();

    // 입고: TASK 수신 → (startDelay) INBOUND_START → (doneDelay) INBOUND_DONE
    public void scheduleInbound(InboundTaskDto task, String shuttleId) {
        if (!props.isEnabled()) {
            return;
        }
        String sid = (shuttleId != null) ? shuttleId : props.getDefaultShuttleId();
        Instant startAt = reserveSlot(task.getStationId());
        Instant doneAt = startAt.plusMillis(props.getDoneDelayMs());

        log.info("[SIM] 입고 자동 시뮬레이션 예약 | wcsTaskId={} station={} START={} DONE={}",
                task.getWcsTaskId(), task.getStationId(), startAt, doneAt);

        taskScheduler.schedule(() -> sendInboundStart(task, sid), startAt);
        taskScheduler.schedule(() -> sendInboundDone(task, sid), doneAt);
    }

    // 출고: TASK 수신 → (startDelay) OUTBOUND_START → (doneDelay) OUTBOUND_DONE
    public void scheduleOutbound(OutboundTaskDto task, String shuttleId) {
        if (!props.isEnabled()) {
            return;
        }
        String sid = (shuttleId != null) ? shuttleId : props.getDefaultShuttleId();
        Instant startAt = reserveSlot(task.getDestStation());
        Instant doneAt = startAt.plusMillis(props.getDoneDelayMs());

        log.info("[SIM] 출고 자동 시뮬레이션 예약 | wcsTaskId={} destStation={} START={} DONE={}",
                task.getWcsTaskId(), task.getDestStation(), startAt, doneAt);

        taskScheduler.schedule(() -> sendOutboundStart(task, sid), startAt);
        taskScheduler.schedule(() -> sendOutboundDone(task, sid), doneAt);
    }

    /**
     * 해당 스테이션의 다음 작업 슬롯을 예약하고 착수(START) 시각을 돌려준다.
     *
     * <p>착수 시각 = max(지금 + startDelay, 앞 팔렛 완료 + gap).
     * 즉 앞 팔렛이 배출/적재를 마치기 전에는 다음 팔렛이 출발하지 않는다.
     * user-outbound-request 는 여러 TASK 를 for-loop 로 연달아 발행하므로, 이 직렬화가 없으면
     * 모든 팔렛의 START/DONE 이 거의 동시에 발생해 같은 스테이션에 동시 도착하는
     * 물리적으로 불가능한 상황이 된다.
     */
    private Instant reserveSlot(String stationId) {
        String key = (stationId != null) ? stationId : NO_STATION;
        Instant earliest = Instant.now().plusMillis(props.getStartDelayMs());
        long occupyMs = props.getDoneDelayMs() + props.getGapMs();

        // compute() 로 원자적으로 슬롯을 잡는다 (TASK 가 동시에 들어와도 겹치지 않도록)
        Instant[] holder = new Instant[1];
        stationCursor.compute(key, (k, cursor) -> {
            Instant startAt = (cursor == null || cursor.isBefore(earliest)) ? earliest : cursor;
            holder[0] = startAt;
            return startAt.plusMillis(occupyMs);
        });
        return holder[0];
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
