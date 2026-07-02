package com.example.shuttlewcs.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WcsOutboundOrderDMapper {

    List<WcsOutboundOrderD> findByTaskId(@Param("taskId") String taskId);

    int countAll();

    void insert(WcsOutboundOrderD detail);

    // OUTBOUND_COMPLETE 시 완료 처리 — palletId 기준 미취소·미완료 라인 completed_at 기록
    void updateCompletedByPalletId(@Param("palletId") String palletId,
                                   @Param("completedAt") LocalDateTime completedAt);

    // 출고 지시 완료 집계용 — 미취소·미완료(진행중) 라인 수
    int countActiveIncompleteByTaskId(@Param("taskId") String taskId);
}
