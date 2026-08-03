package com.example.shuttlewcs.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WcsTaskHMapper {

    /**
     * TASK 발행 적재. 동일 (wcs_task_id, task_type) 재발행(거부 후 재시도 등) 시
     * 기존 행을 재무장(dispatched_at 갱신 · start/complete 초기화)한다.
     */
    void upsertDispatched(WcsTaskH task);

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

    List<WcsTaskH> findByWcsTaskId(@Param("wcsTaskId") String wcsTaskId);
}
