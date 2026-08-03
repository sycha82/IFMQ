package com.example.shuttlewcs.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WcsTaskHMapper {

    // 작업 채번 — wcs_task_id = TSK-{8자리}. TASK 발행 시마다 새로 발급.
    String nextWcsTaskId();

    // TASK 발행 적재 (wcs_task_id 가 발행 시 채번되므로 항상 신규 행)
    void insertDispatched(WcsTaskH task);

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
