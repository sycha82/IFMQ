package com.example.shuttlewcs.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WcsInboundOrderHMapper {

    WcsInboundOrderH findByTaskId(@Param("taskId") String taskId);

    int countAll();

    void insert(WcsInboundOrderH order);

    void updateStatus(@Param("taskId") String taskId,
                      @Param("cmdStatus") String cmdStatus);
}
