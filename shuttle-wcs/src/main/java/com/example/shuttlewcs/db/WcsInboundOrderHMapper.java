package com.example.shuttlewcs.db;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WcsInboundOrderHMapper {

    WcsInboundOrderH findByTaskId(@Param("taskId") String taskId);

    int countAll();

    // 모니터링 조회 — 최근 입고 지시
    List<WcsInboundOrderH> findRecent(@Param("limit") int limit);

    void insert(WcsInboundOrderH order);

    void updateStatus(@Param("taskId") String taskId,
                      @Param("cmdStatus") String cmdStatus);
}
