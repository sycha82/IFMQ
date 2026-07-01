package com.example.shuttlewcs.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WcsInboundOrderDMapper {

    List<WcsInboundOrderD> findByTaskId(@Param("taskId") String taskId);

    WcsInboundOrderD findPendingByPalletId(@Param("palletId") String palletId);

    List<WcsInboundOrderD> findAllPendingByPalletId(@Param("palletId") String palletId);

    // INBOUND_TASK 발행용 — 매핑 완료(mapped_at 무관) · 미취소 활성 라인 조회
    List<WcsInboundOrderD> findActiveByPalletId(@Param("palletId") String palletId);

    int countAll();

    void insert(WcsInboundOrderD detail);

    void updateMappedAt(@Param("palletId") String palletId,
                        @Param("mappedAt") LocalDateTime mappedAt);

    // INBOUND_DONE 시 완료 처리 — palletId 기준 미취소·미완료 라인 completed_at 기록
    void updateCompletedByPalletId(@Param("palletId") String palletId,
                                   @Param("completedAt") LocalDateTime completedAt);

    // task 완료 집계용 — 미취소·미완료(진행중) 라인 수
    int countActiveIncompleteByTaskId(@Param("taskId") String taskId);
}
