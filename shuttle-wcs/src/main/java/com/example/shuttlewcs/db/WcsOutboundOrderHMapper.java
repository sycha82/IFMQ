package com.example.shuttlewcs.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WcsOutboundOrderHMapper {

    WcsOutboundOrderH findByTaskId(@Param("taskId") String taskId);

    // 출고 시작 대상 — 특정 cmd_status 지시 목록 (received_at 순)
    List<WcsOutboundOrderH> findByStatus(@Param("cmdStatus") String cmdStatus);

    int countAll();

    // 모니터링 조회 — 최근 출고 지시
    List<WcsOutboundOrderH> findRecent(@Param("limit") int limit);

    void insert(WcsOutboundOrderH order);

    void updateStatus(@Param("taskId") String taskId,
                      @Param("cmdStatus") String cmdStatus);
}
