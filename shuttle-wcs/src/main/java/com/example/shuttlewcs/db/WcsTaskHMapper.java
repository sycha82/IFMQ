package com.example.shuttlewcs.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WcsTaskHMapper {

    // 작업 채번 — wcs_task_id = TSK-{8자리}. TASK 발행 시마다 새로 발급.
    String nextWcsTaskId();

    // TASK 선기록 — 전문 발송 이전에 커밋 (status=DISPATCHING, 셔틀/ACK 미정)
    void insertPending(WcsTaskH task);

    // ACK 수락 — 배정 셔틀·ACK 정보 반영
    int markDispatched(@Param("wcsTaskId") String wcsTaskId,
                       @Param("taskType") String taskType,
                       @Param("shuttleId") String shuttleId,
                       @Param("ackMessageId") String ackMessageId,
                       @Param("ackResult") String ackResult);

    // ACK 거부 — 이력 보존용
    int markRejected(@Param("wcsTaskId") String wcsTaskId,
                     @Param("taskType") String taskType,
                     @Param("shuttleId") String shuttleId,
                     @Param("ackMessageId") String ackMessageId,
                     @Param("failReason") String failReason);

    // 전송 실패(예외·타임아웃) — RCS 수신 여부 불명
    int markSendFailed(@Param("wcsTaskId") String wcsTaskId,
                       @Param("taskType") String taskType,
                       @Param("failReason") String failReason);

    // 설비 착수(INBOUND_START · OUTBOUND_START) 반영
    int markStarted(@Param("wcsTaskId") String wcsTaskId,
                    @Param("taskType") String taskType,
                    @Param("shuttleId") String shuttleId,
                    @Param("startedAt") LocalDateTime startedAt);

    // 완료(INBOUND_DONE · OUTBOUND_DONE) 반영
    int markCompleted(@Param("wcsTaskId") String wcsTaskId,
                      @Param("taskType") String taskType,
                      @Param("shuttleId") String shuttleId,
                      @Param("completedAt") LocalDateTime completedAt);

    // 실패 보고 반영
    int markFailed(@Param("wcsTaskId") String wcsTaskId,
                   @Param("taskType") String taskType,
                   @Param("shuttleId") String shuttleId,
                   @Param("failReason") String failReason,
                   @Param("failedAt") LocalDateTime failedAt);

    List<WcsTaskH> findRecent(@Param("taskType") String taskType,
                              @Param("taskStatus") String taskStatus,
                              @Param("eqpPalletId") String eqpPalletId,
                              @Param("limit") int limit);

    WcsTaskH findOneByWcsTaskId(@Param("wcsTaskId") String wcsTaskId);
}
