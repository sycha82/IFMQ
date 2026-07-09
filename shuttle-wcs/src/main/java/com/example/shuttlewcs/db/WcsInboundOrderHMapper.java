package com.example.shuttlewcs.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WcsInboundOrderHMapper {

    WcsInboundOrderH findByTaskId(@Param("taskId") String taskId);

    // 입고 시작 대상 — 특정 cmd_status 지시 목록 (received_at 순)
    List<WcsInboundOrderH> findByStatus(@Param("cmdStatus") String cmdStatus);

    int countAll();

    void insert(WcsInboundOrderH order);

    void updateStatus(@Param("taskId") String taskId,
                      @Param("cmdStatus") String cmdStatus);
}
